package com.xicp.server;

import com.xicp.server.quorum.FollowerXiCPServer;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @description: FollowerRequestProcessor
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class FollowerRequestProcessor extends XiCPThread implements RequestProcessor {

    FollowerXiCPServer xcs;
    RequestProcessor nextProcessor;
    private final LinkedBlockingQueue<Request> queuedRequests = new LinkedBlockingQueue<Request>();

    public FollowerRequestProcessor(FollowerXiCPServer xcs, RequestProcessor nextProcessor) {
        super("SyncRequestProcessor");
        this.xcs = xcs;
        this.nextProcessor = nextProcessor;
    }

    @Override
    public void run() {
        try {
            while (true){
                Request request = queuedRequests.take();
                if (nextProcessor != null) {
                    //System.out.println("SyncRequestProcessor run -> " + request);
                    nextProcessor.processRequest(request);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void processRequest(Request request) throws RequestProcessorException {

    }

    @Override
    public void shutdown() {

    }
}
