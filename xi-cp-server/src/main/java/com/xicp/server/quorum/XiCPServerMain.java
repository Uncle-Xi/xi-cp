package com.xicp.server.quorum;

import com.xicp.ServerCnxnFactory;
import com.xicp.server.FileTxnSnapLog;
import com.xicp.server.XiCPServer;
import com.xicp.server.XiCPServerShutdownHandler;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @description: XiCPServerMain
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class XiCPServerMain {

    public static final String CONF_LOCATION = "conf/xicp.properties";

    public static void main(String[] args) {
        XiCPServerMain main = new XiCPServerMain();
        try {
            main.initializeAndRun(args);
        } catch (Exception e) {
            System.err.println(e);
            System.exit(2);
        }
        System.exit(0);
    }

    protected void initializeAndRun(String[] args) throws Exception {
        ServerConfig config = new ServerConfig();
        if (args.length == 1) {
            config.parse(args[0]);
        } else {
            config.parse(CONF_LOCATION);
        }
        runFromConfig(config);
    }

    private ServerCnxnFactory cnxnFactory;

    public void runFromConfig(ServerConfig config) throws IOException {
        System.out.println("Starting server");
        FileTxnSnapLog txnLog = null;
        try {
            final XiCPServer xcServer = new XiCPServer();
            final CountDownLatch shutdownLatch = new CountDownLatch(1);
            txnLog = new FileTxnSnapLog(new File(config.dataLogDir), new File(config.dataDir));
            xcServer.registerServerShutdownHandler(new XiCPServerShutdownHandler(shutdownLatch));
            xcServer.setTxnLogFactory(txnLog);
            xcServer.setTickTime(config.tickTime);
            xcServer.setMinSessionTimeout(config.minSessionTimeout);
            xcServer.setMaxSessionTimeout(config.maxSessionTimeout);
            cnxnFactory = ServerCnxnFactory.createFactory();
            cnxnFactory.configure(config.getClientPortAddress(), config.getMaxClientCnxns());
            cnxnFactory.startup(xcServer);
            shutdownLatch.await();
        } catch (InterruptedException e) {
            System.err.println("Server interrupted" + e);
        } finally {
            if (txnLog != null) {
                //txnLog.close();
            }
        }
    }
}
