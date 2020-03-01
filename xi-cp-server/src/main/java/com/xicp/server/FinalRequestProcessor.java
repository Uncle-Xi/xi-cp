package com.xicp.server;

import com.sun.org.apache.bcel.internal.classfile.Code;
import com.xicp.OpCode;
import com.xicp.ServerCnxn;
import com.xicp.server.data.Message;
import com.xicp.util.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * @description: FinalRequestProcessor
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class FinalRequestProcessor implements RequestProcessor {

    XiCPServer xcs;

    public FinalRequestProcessor(XiCPServer xcs) {
        this.xcs = xcs;
    }

    @Override
    public void processRequest(Request request) throws RequestProcessorException {
        //System.out.println("FinalRequestProcessor processRequest -> " + StringUtils.getString(request));
        if (request.cnxn == null) {
            return;
        }
        ServerCnxn cnxn = request.cnxn;
        Message rsp = cnxn.getMessage();
        boolean closeSession = false;
        try {
            switch (request.type) {
                case OpCode.ping: {
                    //System.out.println("[FinalRequestProcessor][OpCode][ping]");
                    rsp.setContent("OK");
                    cnxn.sendResponse(rsp);
                    return;
                } case OpCode.getData: {
                    //System.out.println("[FinalRequestProcessor][OpCode][getData]");
                    byte[] data = xcs.getXCDatabase().getData(cnxn.getMessage().getPath(), cnxn);
                    rsp.setContent(new String(data));
                    cnxn.sendResponse(rsp);
                    return;
                } case OpCode.exists: {
                    //System.out.println("[FinalRequestProcessor][OpCode][exists]");
                    rsp.setExists(xcs.getXCDatabase().exists(cnxn.getMessage().getPath()));
                    cnxn.sendResponse(rsp);
                    return;
                } case OpCode.setWatches: {
                    //System.out.println("[FinalRequestProcessor][OpCode][setWatches]");
                    xcs.getXCDatabase().setWatches(
                            cnxn.getMessage().getDataWatches(),
                            cnxn.getMessage().getChildWatches(),
                            cnxn);
                    cnxn.sendResponse(rsp);
                    return;
                } case OpCode.getChildren: {
                    //System.out.println("OpCode.getChildren -> " + cnxn.getMessage().getPath());
                    List<String> children = xcs.getXCDatabase().getChildren(
                            cnxn.getMessage().getPath(), cnxn);
                    rsp.setContent(StringUtils.getString(children));
                    //System.out.println("StringUtils.getString(children) -> " + StringUtils.getString(children));
                    cnxn.sendResponse(rsp);
                    return;
                } default:
                    //System.out.println("[OpCode.default]");
                    Message message = xcs.getXCDatabase().processTxn(cnxn.getMessage());
                    cnxn.sendResponse(message);
                    return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            rsp.setContent(e.getMessage());
            try {
                cnxn.sendResponse(rsp);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void shutdown() {

    }
}
