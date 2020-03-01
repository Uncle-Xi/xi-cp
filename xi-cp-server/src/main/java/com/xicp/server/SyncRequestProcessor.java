package com.xicp.server;

import java.io.Flushable;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @description: SyncRequestProcessor
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class SyncRequestProcessor extends XiCPThread implements RequestProcessor {

    private final XiCPServer xcs;
    private final LinkedBlockingQueue<Request> queuedRequests = new LinkedBlockingQueue<Request>();
    private final RequestProcessor nextProcessor;

    public SyncRequestProcessor(XiCPServer xcs, RequestProcessor nextProcessor) {
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
        queuedRequests.add(request);
    }

    @Override
    public void shutdown() {

    }
}
