package com.xicp.server.quorum;

import com.xicp.server.*;

/**
 * @description: LeaderXiCPServer
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class LeaderXiCPServer extends XiCPServer {

    QuorumPeer self;

    public LeaderXiCPServer(FileTxnSnapLog logFactory, QuorumPeer self, XiCPDatabase xiDb) {
        this.txnLogFactory = logFactory;
        this.self = self;
        this.xcDb = xiDb;
    }

    @Override
    protected void setupRequestProcessors() {
        RequestProcessor finalProcessor = new FinalRequestProcessor(this);
        RequestProcessor syncProcessor = new SyncRequestProcessor(this, finalProcessor);
        firstProcessor = new PrepRequestProcessor(this, syncProcessor);
        ((SyncRequestProcessor) syncProcessor).start();
        ((PrepRequestProcessor) firstProcessor).start();
    }
}
