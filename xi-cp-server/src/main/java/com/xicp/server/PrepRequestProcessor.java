package com.xicp.server;

import com.xicp.OpCode;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @description: PrepRequestProcessor
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class PrepRequestProcessor extends XiCPThread implements RequestProcessor {

    LinkedBlockingQueue<Request> submittedRequests = new LinkedBlockingQueue<Request>();
    private final XiCPServer xcs;
    private final RequestProcessor nextProcessor;

    public PrepRequestProcessor(XiCPServer xcs, RequestProcessor nextProcessor) {
        super("ProcessThread(sid:" + 0 + " cport:" + xcs.getClientPort() + ")");
        this.nextProcessor = nextProcessor;
        this.xcs = xcs;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Request request = submittedRequests.take();
                pRequest(request);
            }
        } catch (RequestProcessorException e) {
            handleException(this.getName(), e);
        } catch (Exception e) {
            handleException(this.getName(), e);
        }
    }

    protected void pRequest(Request request) throws RequestProcessorException {
        //System.out.println("PrepRequestProcessor pRequest -> " + request);
        nextProcessor.processRequest(request);
    }

    @Override
    public void processRequest(Request request) throws RequestProcessorException {
        submittedRequests.add(request);
    }

    @Override
    public void shutdown() {

    }
}
