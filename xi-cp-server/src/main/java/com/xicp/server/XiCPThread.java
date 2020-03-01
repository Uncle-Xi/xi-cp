package com.xicp.server;

/**
 * @description: XiCPThread
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class XiCPThread extends Thread {

    private UncaughtExceptionHandler uncaughtExceptionalHandler = new UncaughtExceptionHandler() {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            handleException(t.getName(), e);
        }
    };

    public XiCPThread(Runnable thread, String threadName) {
        super(thread, threadName);
        setUncaughtExceptionHandler(uncaughtExceptionalHandler);
    }

    public XiCPThread(String threadName) {
        super(threadName);
        setUncaughtExceptionHandler(uncaughtExceptionalHandler);
    }

    protected void handleException(String thName, Throwable e) {
        System.out.println("Exception occurred from thread " + thName + e);
    }
}
