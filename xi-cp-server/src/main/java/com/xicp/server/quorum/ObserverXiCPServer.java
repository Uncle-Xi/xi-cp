package com.xicp.server.quorum;

import com.xicp.server.*;

/**
 * @description: ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class ObserverXiCPServer extends XiCPServer {

    FileTxnSnapLog logFactory;
    QuorumPeer peer;
    protected XiCPDatabase xiDb;

    public ObserverXiCPServer(FileTxnSnapLog logFactory, QuorumPeer peer, XiCPDatabase xiDb) {
        this.logFactory = logFactory;
        this.peer = peer;
        this.xiDb = xiDb;
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
