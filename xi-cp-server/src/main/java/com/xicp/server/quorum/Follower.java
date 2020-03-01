package com.xicp.server.quorum;

import com.xicp.server.XiCPThread;
import com.xicp.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

/**
 * @description: Follower
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class Follower extends Learner{

    QuorumPeer self;
    FollowerXiCPServer server;

    public Follower(QuorumPeer self, FollowerXiCPServer server){
        this.self = self;
        this.server = server;
    }

    // 新建节点通信网络，从主节点同步数据
    // 启动一个心跳器，定时问候主节点，主节点挂掉，结束当前逻辑，进入选举状态
    // 构建任务处理责任链[转发事务请求到主节点]
    void followLeader() throws InterruptedException {
        syncData();
        server.setupRequestProcessors();
        while (!shutdown){
            try {
                Thread.sleep(tickTime * 1000);
                QuorumServer quorumServer = self.getMaster();
                ping(quorumServer);
            } catch (Exception e){
                e.printStackTrace();
                triggerLooking();
            }
        }
    }

    // TODO 发送最大 zxid 给主节点，
    // 将主节点返回的数据包，存入内存
    // 写快照
    protected void syncData(){
        System.out.println("开始从主节点同步数据：");
        long zxid = self.xiDb.lastProcessedZxid;
        //
    }

    long tickTime = 5; // second
    long refuseLimit = 5; // 心跳拒绝最大次数
    long refuseCnt = 0; // second
    volatile boolean shutdown = false;

    protected void triggerLooking(){
        // TODO 心跳拒绝达到次数后，开始重新选举
        refuseCnt++;
        if (refuseCnt == refuseLimit) {
            shutdown = true;
            self.setPeerState(ServerState.LOOKING);
        }
    }
}
