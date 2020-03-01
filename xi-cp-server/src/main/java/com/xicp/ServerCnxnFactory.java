package com.xicp;

import com.xicp.server.XiCPServer;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @description: ServerCnxnFactory
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public abstract class ServerCnxnFactory {

    public static final String ZOOKEEPER_SERVER_CNXN_FACTORY = "zookeeper.serverCnxnFactory";

    static public ServerCnxnFactory createFactory() throws IOException {
        String serverCnxnFactoryName = System.getProperty(ZOOKEEPER_SERVER_CNXN_FACTORY);
        if (serverCnxnFactoryName == null) {
            serverCnxnFactoryName = NettyServerCnxnFactory.class.getName();
        }
        try {
            ServerCnxnFactory serverCnxnFactory = (ServerCnxnFactory) Class.forName(serverCnxnFactoryName)
                    .getDeclaredConstructor().newInstance();
            return serverCnxnFactory;
        } catch (Exception e) {
            IOException ioe = new IOException("Couldn't instantiate " + serverCnxnFactoryName);
            ioe.initCause(e);
            throw ioe;
        }
    }

    protected XiCPServer xcServer;

    final public void setXiCPServer(XiCPServer xc) {
        this.xcServer = xc;
        if (xc != null) {
            xc.setServerCnxnFactory(this);
        }
    }

    public abstract int getLocalPort();

    public abstract InetSocketAddress getLocalAddress();

    public abstract void start();

    public abstract void startup(XiCPServer zkServer) throws IOException, InterruptedException;

    public abstract void configure(InetSocketAddress addr, int maxClientCnxns) throws IOException;
}
