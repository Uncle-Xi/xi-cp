package com.xicp.server.quorum;

import com.xicp.server.*;

/**
 * @description: FollowerXiCPServer
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class FollowerXiCPServer extends XiCPServer {

    QuorumPeer self;

    public FollowerXiCPServer(FileTxnSnapLog logFactory, QuorumPeer self, XiCPDatabase xiDb) {
        this.txnLogFactory = logFactory;
        this.self = self;
        this.xcDb = xcDb;
    }

    @Override
    protected void setupRequestProcessors() {
        RequestProcessor finalProcessor = new FinalRequestProcessor(this);
        firstProcessor = new FollowerRequestProcessor(this, finalProcessor);
        ((FollowerRequestProcessor) firstProcessor).start();
    }
}
