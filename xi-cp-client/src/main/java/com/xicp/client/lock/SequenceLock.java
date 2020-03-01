package com.xicp.client.lock;

import com.xicp.EventType;
import com.xicp.WatchedEvent;
import com.xicp.Watcher;
import com.xicp.XiCP;
import com.xicp.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @description: CaptureLock
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class SequenceLock extends Lock {

    private static final String DEFUALT_LOCK_PREFFIX = "/SequenceLock";
    private static final String EPHEMER_LOCK_PRFFIX = "/locks";
    private static final String REPLACE = "locks-";
    //private Map<String, String> locks = new ConcurrentHashMap<>();
    //private Map<String, PostProcess> postProcessMap = new ConcurrentHashMap<>();
    private String connectString;
    private XiCP xiCP;
    //private long threadId = Thread.currentThread().getId();
    private String ephemeralPath;
    private PostProcess postProcess;

    public SequenceLock(String connectString) throws IOException {
        this.connectString = connectString;
        this.xiCP = new XiCP(connectString, new Listener(Thread.currentThread().getId()));
    }

    public static interface PostProcess {
        void handler();
    }

    public synchronized void acquireLock(String lock, PostProcess postProcess) {
        lock = getValidLock(lock, DEFUALT_LOCK_PREFFIX);
        long t = Thread.currentThread().getId();
        if (!this.xiCP.exsits(lock)) {
            //System.out.println("[SequenceLock] [acquireLock] [create lock]");
            this.xiCP.createPermNode(lock);
        }
        String path = this.xiCP.createTermOrderlyNode(lock + EPHEMER_LOCK_PRFFIX);
        //System.out.println(Thread.currentThread().getId() + "] - [acquireLock] [lock] - " + lock);
        //System.out.println(Thread.currentThread().getId() + "] - [acquireLock] [path] - " + path);
        //locks.put(lock + t, path);
        //postProcessMap.put(lock + t, postProcess);
        this.ephemeralPath = path;
        this.postProcess = postProcess;
        doAcquire(lock, t);
    }

    private synchronized void doAcquire(String path, long t) {
//        if (locks.size() == 0) {
//            return;
//        }
        List<String> childs = this.xiCP.getChildren(path);
        if (childs == null || childs.size() < 1) {
            return;
        }
        String ephemeral = sort(childs).get(0);
        //String ephemeralLocal = locks.get(path + t).replaceAll(path + "/", "");
        String ephemeralLocal = this.ephemeralPath.replaceAll(path + "/", "");
        //System.out.println(t + "] - [ephemeralLocal] [" + ephemeralLocal);
        //System.out.println(t + "] - [ephemeral] [" + ephemeral);
        if (ephemeral.equalsIgnoreCase(ephemeralLocal)) {
            //System.out.println(t + "] - [SequenceLock] [doAcquire][获得锁成功].");
            //this.postProcessMap.get(path + t).handler();
            this.postProcess.handler();
        } else {
            this.xiCP.getChildren(path);
            //System.out.println(t + "] - [SequenceLock] [doAcquire][重新监控].");
        }
    }

    private List<String> sort(List<String> childs) {
        int idx = childs.size() - 1;
        String child = childs.get(idx);
        for (int i = 0; i < childs.size() - 1; i++) {
            for (int k = i + 1; k < childs.size(); k++) {
                String is = childs.get(i);
                String ks = childs.get(k);
                if (Long.valueOf(is.replace(REPLACE, ""))
                        > Long.valueOf(ks.replace(REPLACE, ""))) {
                    child = ks;
                    idx = k;
                    childs.set(i, is);
                }
            }
        }
        childs.set(idx, child);
//        System.out.println(Thread.currentThread().getId() + "] - [childs.size()] - [" + childs.size());
//        childs.forEach(c -> {
//            System.out.println("c - " + c);
//        });
        return childs;
    }

    public void releaseLock(String lock) {
//        lock = getValidLock(lock, DEFUALT_LOCK_PREFFIX);
//        if (locks.size() == 0) {
//            return;
//        }
//        long t = Thread.currentThread().getId();
//        System.out.println("[releaseLock] - " + t);
//        String ephemerPath = locks.get(lock + t);
//        if (ephemerPath == null) {
//            return;
//        }
        xiCP.delNode(ephemeralPath);
        //this.postProcessMap.remove(lock + t);
        //this.locks.remove(lock + t);
        //System.out.println(ephemeralPath + "] - [SequenceLock] [releaseLock][清理数据OK]");
    }

    class Listener implements Watcher {

        long t;
        public Listener (long t){
            this.t = t;
        }

        @Override
        public void process(WatchedEvent event) {
            if (EventType.NodeChildrenChanged.equals(event.getEventType())) {
                String path = event.getPath();
                //System.out.println(Thread.currentThread().getId() + "] - [SequenceLock] [收到子节点变化通知] [" + path + "]");
                doAcquire(path, t);
            }
        }
    }

    public void close() {
        xiCP.close();
    }
}