package com.xicp.client;

import com.xicp.*;
import com.xicp.server.XiCPThread;
import com.xicp.server.data.Message;
import com.xicp.server.data.XiDecoder;
import com.xicp.server.data.XiEncoder;
import com.xicp.util.StringUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @description: XiCPClient
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class XiCPClient {

    private XiCPClientHandler xiCPClientHandler;
    private volatile boolean shutdown = false;
    private XiCP xiCP;
    private Watcher defaultWatcher;
    private ConnectStringParser connectStringParser;
    private final LinkedBlockingQueue<Message> sendQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<Message> resultQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<WatchedEvent> eventQueue = new LinkedBlockingQueue<>();
    private ArrayList<InetSocketAddress> serverAddresses = new ArrayList<InetSocketAddress>();

    public XiCPClient(String connectString, XiCP xiCP, Watcher watcher) {
        this.xiCP = xiCP;
        this.defaultWatcher = watcher;
        this.xiCPClientHandler = new XiCPClientHandler();
        this.sendThread = new SendThread(xiCPClientHandler);
        this.eventThread = new EventThread();
        this.connectStringParser = new ConnectStringParser(connectString);
        this.serverAddresses = connectStringParser.getServerAddresses();
        this.connect(next(0));
    }

    public synchronized Message submitRequest(Message message) throws InterruptedException {
        //System.out.println("submitRequest ... " + message);
        if (sendQueue.add(message)) {
            if (message.isSync()) {
                Message result = resultQueue.take();
                return result;
            }
            //System.out.println("submitRequest ... OK ");
        }
        return null;
    }

    class XiCPClientHandler extends ChannelInboundHandlerAdapter {
        //class XiCPClientHandler extends SimpleChannelInboundHandler<Message> {

        private ChannelHandlerContext ctx;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            this.ctx = ctx;
            //System.out.println("channelActive ... ");
        }

        private boolean sendMsg(Message msg) throws InterruptedException {
            while (ctx == null) {
                Thread.sleep(200);
                //System.out.println("XiCPClient ChannelHandlerContext is null...");
            }
            ctx.channel().writeAndFlush(StringUtils.getString(msg));
            //System.out.println("数据发送完成.");
            return true;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object object) throws Exception {
            try {
                if (shutdown) {
                    ctx.close();
                    return;
                }
                List<Message> messages = StringUtils.getMessageObjecct((String) object);
                for (Message message : messages) {
                    if (message.getEvent() != null) {
                        eventQueue.add(message.getEvent());
                    } else if (message.isSync()) {
                        resultQueue.add(message);
                    }
                }
                //Message message = object;
            } catch (Exception e) {
                System.out.println("转换错误");
                e.printStackTrace();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.out.println("[XiCPClient] [与服务器断开连接] - " + cause.getMessage());
            ctx.close();
        }
    }

    public void connect(InetSocketAddress address) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    Bootstrap b = new Bootstrap();
                    b.group(workerGroup);
                    b.channel(NioSocketChannel.class);
                    b.option(ChannelOption.SO_KEEPALIVE, true);
                    b.handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
                            //pipeline.addLast(new StringDecoder(Charset.forName("UTF-8")));
                            //pipeline.addLast(new StringEncoder(Charset.forName("UTF-8")));
                            pipeline.addLast(new XiDecoder());
                            pipeline.addLast(new LengthFieldPrepender(2));
                            pipeline.addLast(new XiEncoder());
                            pipeline.addLast(xiCPClientHandler);
                        }
                    });
                    ChannelFuture f = b.connect(address).sync();
                    f.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    workerGroup.shutdownGracefully();
                }
            }
        }).start();
    }

    final SendThread sendThread;
    final EventThread eventThread;

    class SendThread extends XiCPThread {

        XiCPClientHandler handler;

        public SendThread(XiCPClientHandler handler) {
            super("SendThread...");
            this.handler = handler;
        }

        @Override
        public void run() {
            while (!shutdown) {
                try {
                    Message message = sendQueue.take();
                    handler.sendMsg(message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class EventThread extends XiCPThread {

        public EventThread() {
            super("EventThread");
        }

        @Override
        public void run() {
            while (!shutdown) {
                try {
                    WatchedEvent event = eventQueue.take();
                    if (defaultWatcher != null) {
                        defaultWatcher.process(event);
                    }
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        }
    }

    public void start() {
        eventThread.start();
        sendThread.start();
    }

    public void shutdownNow() {
        this.shutdown = true;
        sendThread.interrupt();
        eventThread.interrupt();
        //System.out.println("[XiCPClient] [shutdown][now].");
    }

    private int lastIndex = -1;
    private int currentIndex = -1;

    public InetSocketAddress next(long spinDelay) {
        currentIndex = ++currentIndex % serverAddresses.size();
        if (currentIndex == lastIndex && spinDelay > 0) {
            try {
                Thread.sleep(spinDelay);
            } catch (InterruptedException e) {
                System.out.println("Unexpected exception" + e);
            }
        } else if (lastIndex == -1) {
            lastIndex = 0;
        }
        return serverAddresses.get(currentIndex);
    }
}
