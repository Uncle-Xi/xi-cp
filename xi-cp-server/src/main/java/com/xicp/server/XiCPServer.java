package com.xicp.server;

import com.xicp.OpCode;
import com.xicp.ServerCnxn;
import com.xicp.ServerCnxnFactory;
import com.xicp.server.quorum.XiCPDatabase;
import com.xicp.util.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @description: XiCPServer
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class XiCPServer {

    public static final int DEFAULT_TICK_TIME = 3000;
    protected int tickTime = DEFAULT_TICK_TIME;
    protected int minSessionTimeout = -1;
    protected int maxSessionTimeout = -1;
    protected FileTxnSnapLog txnLogFactory = null;
    protected XiCPDatabase xcDb;
    protected final AtomicLong hzxid = new AtomicLong(0);
    protected final static Exception ok = new Exception("No prob");
    protected RequestProcessor firstProcessor;
    protected volatile State state = State.INITIAL;
    protected ServerCnxnFactory serverCnxnFactory;

    protected enum State {
        INITIAL, RUNNING, SHUTDOWN, ERROR;
    }

    public XiCPServer() { }

    public void startdata() throws IOException, InterruptedException {
        System.out.println("[XiCPServer] startdata -> " + xcDb);
        if (xcDb == null) {
            xcDb = new XiCPDatabase(this.txnLogFactory);
        }
        if (!xcDb.isInitialized()) {
            loadData();
        }
    }

    public void loadData() throws IOException, InterruptedException {
        System.out.println("[XiCPServer] loadData...");
        if (xcDb.isInitialized()) {
            setZxid(xcDb.getDataTreeLastProcessedZxid());
        } else {
            setZxid(xcDb.loadDataBase());
        }
    }


    public void processConnectRequest(ServerCnxn cnxn) throws IOException {
        createSession(cnxn);
    }

    long createSession(ServerCnxn cnxn) {
        submitRequest(cnxn,
                cnxn.getMessage().getClientId(),
                cnxn.getMessage().getType(), 0);
        return 0;
    }

    private void submitRequest(ServerCnxn cnxn, long sessionId, int type, int xid) {
        Request request = new Request(cnxn, sessionId, type, xid);
        //System.out.println("submitRequest -> " + StringUtils.getString(request));
        submitRequest(request);
    }

    public void submitRequest(Request request) {
        try {
            firstProcessor.processRequest(request);
        } catch (RequestProcessor.RequestProcessorException e) {
            System.out.println("Unable to process request:" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setZxid(long zxid) {
        hzxid.set(zxid);
    }

    public synchronized void startup() {
        setupRequestProcessors();
    }

    protected void setupRequestProcessors() {
        RequestProcessor finalProcessor = new FinalRequestProcessor(this);
        RequestProcessor syncProcessor = new SyncRequestProcessor(this, finalProcessor);
        firstProcessor = new PrepRequestProcessor(this, syncProcessor);
        ((SyncRequestProcessor) syncProcessor).start();
        ((PrepRequestProcessor) firstProcessor).start();
    }

    XiCPServerShutdownHandler xcShutdownHandler;

    public void registerServerShutdownHandler(XiCPServerShutdownHandler xcShutdownHandler) {
        this.xcShutdownHandler = xcShutdownHandler;
    }

    public void setServerCnxnFactory(ServerCnxnFactory factory) {
        serverCnxnFactory = factory;
    }

    public int getClientPort() {
        return serverCnxnFactory != null ? serverCnxnFactory.getLocalPort() : -1;
    }

    public ServerCnxnFactory getServerCnxnFactory() {
        return serverCnxnFactory;
    }

    public void setTxnLogFactory(FileTxnSnapLog txnLog) {
        this.txnLogFactory = txnLog;
    }

    public void setTickTime(int tickTime) {
        System.out.println("tickTime set to " + tickTime);
        this.tickTime = tickTime;
    }

    public void setMinSessionTimeout(int min) {
        System.out.println("minSessionTimeout set to " + min);
        this.minSessionTimeout = min;
    }

    public int getMaxSessionTimeout() {
        return maxSessionTimeout == -1 ? tickTime * 20 : maxSessionTimeout;
    }

    public void setMaxSessionTimeout(int max) {
        System.out.println("maxSessionTimeout set to " + max);
        this.maxSessionTimeout = max;
    }

    public XiCPDatabase getXCDatabase() {
        return this.xcDb;
    }

    public void setXCDatabase(XiCPDatabase xcDb) {
        this.xcDb = xcDb;
    }
}
