package com.xicp.server;

import java.util.concurrent.CountDownLatch;

/**
 * @description: XiCPServerShutdownHandler
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class XiCPServerShutdownHandler {

    private final CountDownLatch shutdownLatch;

    public XiCPServerShutdownHandler(CountDownLatch shutdownLatch) {
        this.shutdownLatch = shutdownLatch;
    }

    void handle(XiCPServer.State state) {
        if (state == XiCPServer.State.ERROR || state == XiCPServer.State.SHUTDOWN) {
            shutdownLatch.countDown();
        }
    }
}
