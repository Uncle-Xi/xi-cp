package com.xicp.client.lock;

import com.xicp.WatchedEvent;
import com.xicp.Watcher;
import com.xicp.XiCP;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @description: CompeteLock
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class CompeteLock extends Lock implements Watcher {

    private static final String DEFUALT_LOCK_PREFFIX = "/competeLock";
    private String connectString;
    private XiCP xiCP;

    public CompeteLock(String connectString) throws IOException {
        this.connectString = connectString;
        this.xiCP = new XiCP(connectString, this);
    }

    public boolean lock(String lock, long timeout) {
        if (timeout < 0) {
            throw new RuntimeException("timeout must greater than or equals zero.");
        }
        lock = getValidLock(lock, DEFUALT_LOCK_PREFFIX);
        try {
            if (xiCP.exists(lock, true)) {
                Thread.sleep(timeout);
                if (!xiCP.exists(lock, true)) {
                    xiCP.create(lock, lock.getBytes(), true, false);
                    return true;
                }
            } else {
                xiCP.create(lock, lock.getBytes(), true, false);
                return true;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public boolean unLock(String lock) {
        lock = getValidLock(lock, DEFUALT_LOCK_PREFFIX);
        try {
            xiCP.delete(lock);
            return true;
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public void close(){
        xiCP.close();
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        // TODO
    }
}