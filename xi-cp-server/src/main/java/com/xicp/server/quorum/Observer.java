package com.xicp.server.quorum;

/**
 * @description: Observer
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class Observer extends Learner{

    QuorumPeer self;
    ObserverXiCPServer server;

    public Observer(QuorumPeer self, ObserverXiCPServer server){
        this.self = self;
        this.server = server;
    }


    void observeLeader() throws InterruptedException {

    }

}
