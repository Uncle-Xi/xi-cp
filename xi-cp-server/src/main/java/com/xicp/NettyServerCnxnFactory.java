package com.xicp;

import com.xicp.server.XiCPServer;
import com.xicp.server.data.Message;
import com.xicp.server.data.XiDecoder;
import com.xicp.server.data.XiEncoder;
import com.xicp.util.StringUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * @description: NettyServerCnxnFactory
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class NettyServerCnxnFactory extends ServerCnxnFactory {

    ServerBootstrap bootstrap = new ServerBootstrap();
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    InetSocketAddress localAddress;
    int maxClientCnxns = 60;
    NettyServerCnxnFactory factory;

    class NettyServerHandler extends ChannelInboundHandlerAdapter {
        //class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            //System.out.println("NettyServerHandler channelActive...");
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("[NettyServerCnxnFactory] [用户请求]=[ " + msg);
            NettyServerCnxn cnxn = new NettyServerCnxn(ctx, xcServer, factory);
            List<Message> messages = StringUtils.getMessageObjecct((String) msg);
            //Message message = msg;
            for (Message message : messages) {
                message.setClientId(0);
                if (message.isEphemer()) {
                    //System.out.println("这是一个临时节点 - " + message.getPath() + " - " + cnxn.incrPacketsReceived());
                    message.setPath(message.getPath());
                    //System.out.println("orderly -> " + message.isOrderly());
                    if (message.isOrderly()) {
                        message.setPath(message.getPath() + "-" + cnxn.incrPacketsReceived());
                    }
                    String ipPort = getClientIpPort(ctx);
                    message.setClientId(getClientId(ipPort));
                    if (message.getContent() == null) {
                        message.setContent(ipPort);
                    }
                }
                cnxn.setMessage(message);
                synchronized (cnxn) {
                    processMessage(cnxn);
                }
            }
        }

        protected String getClientIpPort(ChannelHandlerContext channelHandlerContext) {
            InetSocketAddress ipSocket = (InetSocketAddress) channelHandlerContext.channel().remoteAddress();
            String clientIp = ipSocket.getAddress().getHostAddress();
            int port = ipSocket.getPort();
            System.out.printf("[客户端] [IP:PORT]={%s}\n", (clientIp + port));
            return clientIp + ":" + port;
        }

        protected String getIp(ChannelHandlerContext ctx) {
            InetSocketAddress ipSocket = (InetSocketAddress) ctx.channel().remoteAddress();
            String clientIp = ipSocket.getAddress().getHostAddress();
            return clientIp;
        }

        protected long getClientId(String ipPort) {
            ipPort = ipPort.replaceAll(":", "");
            ipPort = ipPort.replaceAll("\\.", "");
            if (StringUtils.isEmpty(ipPort)) {
                return 0;
            }
            //System.out.println("getClientId ipPort -> " + ipPort);
            return Long.valueOf(ipPort);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            //System.out.println("关闭连接,触发删除临时节点：" + cause.getMessage());
            cause.printStackTrace();
            NettyServerCnxn cnxn = new NettyServerCnxn(ctx, xcServer, factory);
            Message message = new Message();
            message.setType(OpCode.closeSession);
            message.setClientId(getClientId(getClientIpPort(ctx)));
            cnxn.setMessage(message);
            synchronized (cnxn) {
                processMessage(cnxn);
            }
        }

        private void processMessage(NettyServerCnxn cnxn) {
            cnxn.receiveMessage();
        }
    }

    NettyServerCnxnFactory() {
        this.factory = this;
        bootstrap.group(bossGroup, workerGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                //socketChannel.pipeline().addLast("decoder",new StringDecoder(CharsetUtil.UTF_8));
                //socketChannel.pipeline().addLast("encoder",new StringEncoder(CharsetUtil.UTF_8));
                //pipeline.addLast(new StringDecoder(Charset.forName("UTF-8")));
                //pipeline.addLast(new StringEncoder(Charset.forName("UTF-8")));
                pipeline.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
                pipeline.addLast(new XiDecoder());
                pipeline.addLast(new LengthFieldPrepender(2));
                pipeline.addLast(new XiEncoder());
                pipeline.addLast(new NettyServerHandler());
            }
        });
        bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
    }

    @Override
    public void start() {
        new Thread(() -> {
            try {
                System.out.println("binding to port " + localAddress);
                ChannelFuture channelFuture = bootstrap.bind(localAddress).sync();
                channelFuture.channel().closeFuture().sync();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }).start();
    }

    @Override
    public void startup(XiCPServer xcServer) throws IOException, InterruptedException {
        start();
        setXiCPServer(xcServer);
        xcServer.startdata();
        xcServer.startup();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public int getLocalPort() {
        return localAddress.getPort();
    }

    @Override
    public void configure(InetSocketAddress addr, int maxClientCnxns) throws IOException {
        localAddress = addr;
        this.maxClientCnxns = maxClientCnxns;
    }
}
