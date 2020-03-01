package com.xicp.server.quorum;

import com.xicp.server.XiCPThread;
import com.xicp.util.StringUtils;

import java.io.*;
import java.net.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description: QuorumCnxManager
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class QuorumCnxManager {

    final int socketTimeout = 30000;
    public final Listener listener;
    volatile boolean shutdown = false;
    static final int SEND_CAPACITY = 1;
    static final int PACKETMAXSIZE = 1024 * 512;
    static final int RECV_CAPACITY = 100;

    SendVoteWorker sendVoteWorker = null;
    RecvVoteWorker recvVoteWorker = null;
    protected Map<Long, QuorumServer> view;
    protected Map<Long, QuorumServer> validServer = new HashMap<>();
    QuorumPeer self;
    long mySid;

    public QuorumCnxManager(QuorumPeer self) {
        this.self = self;
        listener = new Listener();
        view = self.quorumPeers;
        mySid = self.getMyServer().id;
        //recvVoteWorker = new RecvVoteWorker();
    }

    public class Listener extends XiCPThread {
        volatile ServerSocket serverSocket = null;

        public Listener() {
            super("ListenerThread");
        }

        @Override
        public void run() {
            System.out.println("监听选举请求！");
            int numRetries = 0;
            while ((!shutdown) && (numRetries < 3)) {
                try {
                    int port = self.getMyServer().electionAddr.getPort();
                    System.out.println("port -> " + port);
                    serverSocket = new ServerSocket(port);
                    while (!shutdown) {
                        Socket client = serverSocket.accept();
                        recivePacket(client);
                        numRetries = 0;
                    }
                } catch (IOException e) {
                    System.out.println("Exception while listening" + e);
                    numRetries++;
                    try {
                        serverSocket.close();
                        Thread.sleep(1000);
                    } catch (Exception ie) {
                        System.out.println("Error closing server socket" + ie);
                    }
                }
            }
        }
    }

    private void recivePacket(Socket client) {
        try {
            InputStream inputStream = client.getInputStream();
            String content = "";
            byte[] buff = new byte[1024];
            int len = 0;
            if ((len = inputStream.read(buff)) > 0) {
                content += new String(buff, 0, len);
            }
            System.out.printf("recivePacket = [%s], content = [%s]\n", client, content);
            if (StringUtils.isEmpty(content)) {
                return;
            }
            if (content.contains("electionEpoch")) {
                getVote(content);
                return;
            }
            Long sid = Long.valueOf(content);
            validServer.put(sid, view.get(sid));
            OutputStream out = client.getOutputStream();
            out.write(("" + self.getMyid()).getBytes("UTF-8"));
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    protected int connectAll() {
        for (Long sid : view.keySet()) {
            connectOne(sid);
        }
        return validServer.size();
    }

    protected void connectOne(Long sid) {
        try {
            InetSocketAddress addr = view.get(sid).electionAddr;
            System.out.println("InetSocketAddress - " + addr + " - sid - " + sid);
            Socket socket = new Socket(addr.getHostName(), addr.getPort());
            OutputStream out = socket.getOutputStream();
            out.write(("" + sid).getBytes());
            out.flush();
            socket.close();
            validServer.put(sid, view.get(sid));
        } catch (Exception e) {
            System.out.println("connectOne Exception e = " + e + ", sid = " + sid);
        }
    }

    public void starter() {
        System.out.println("开始启动，票据线程...");
        sendVoteWorker = new SendVoteWorker();
        sendVoteWorker.start();
        //recvVoteWorker.start();
        System.out.println("启动了票据发送和票据接受线程...");
    }

    private AtomicInteger threadCnt = new AtomicInteger(0);
    private final Object recvQLock = new Object();
    private LinkedBlockingQueue sendQueue = new LinkedBlockingQueue<Vote>();
    private LinkedBlockingQueue recvQueue = new LinkedBlockingQueue<Vote>();

    public void sendVote(Vote vote) {
        sendQueue.add(vote);
    }

    public void clearAndClose() {
        try {
            sendQueue.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Vote takeVote() {
        try {
            return (Vote) recvQueue.take();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public class SendVoteWorker extends XiCPThread {
        public SendVoteWorker() {
            super("SendVoteWorker");
        }

        @Override
        public void run() {
            System.out.println("发送选票线程启动！");
            Socket socket = null;
            while (!shutdown) {
                Vote vote = null;
                try {
                    vote = (Vote) sendQueue.take();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                for (Long sid : validServer.keySet()) {
                    try {
                        InetSocketAddress addr = validServer.get(sid).electionAddr;
                        socket = new Socket(addr.getHostName(), addr.getPort());
                        OutputStream out = socket.getOutputStream();
                        String voteStr = StringUtils.getString(vote);
                        out.write(voteStr.getBytes());
                        out.flush();
                        socket.close();
                    } catch (IOException e) {
                        System.out.println("Exception while SendVoteWorker" + e);
                        e.printStackTrace();
                    }
                }
            }
        }

        public void shutdown() {
            shutdown = true;
        }
    }

    public class RecvVoteWorker extends XiCPThread {
        int port = self.getMyServer().electionAddr.getPort();
        volatile ServerSocket serverSocket = null;

        public RecvVoteWorker() {
            super("RecvVoteWorker");
        }

        @Override
        public void run() {
            System.out.println("接受投票！");
            try {
                serverSocket = new ServerSocket(port);
            } catch (Exception e) {
                e.printStackTrace();
            }
            while (!shutdown) {
                try {
                    Socket client = serverSocket.accept();
                    recivePacket(client);
                } catch (IOException e) {
                    System.out.println("Exception while listening" + e);
                    e.printStackTrace();
                }
            }
        }
    }

    private void getVote(String content) {
        try {
            Vote vote = (Vote) StringUtils.getObjecctByClazz(content, Vote.class);
            recvQueue.add(vote);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
