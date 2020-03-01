package com.xicp.server.quorum;

import com.xicp.server.Request;

import java.util.HashSet;

/**
 * @description: Leader
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class Leader {

    final static int REQUEST = 1;
    public final static int PROPOSAL = 2;
    final static int ACK = 3;
    final static int COMMIT = 4;
    final static int PING = 5;

    QuorumPeer self;
    LeaderXiCPServer server;

    public Leader(QuorumPeer self, LeaderXiCPServer server){
        this.self = self;
        this.server = server;
    }

    // 构建用户请求责任链
    // 同步链完成数据同步，二阶段提交
    void lead() throws InterruptedException {
        server.startup();
        boolean tickSkip = true;
        while (true){
            Thread.sleep(self.tickTime / 2);
            if (!tickSkip) {
                self.tick.incrementAndGet();
            }
            HashSet<Long> syncedSet = new HashSet<Long>();
        }
    }

    static public class Proposal {
        public QuorumPacket packet;
        public HashSet<Long> ackSet = new HashSet<Long>();
        public Request request;

        @Override
        public String toString() {
            return packet.getType() + ", " + packet.getZxid() + ", " + request;
        }
    }
}
