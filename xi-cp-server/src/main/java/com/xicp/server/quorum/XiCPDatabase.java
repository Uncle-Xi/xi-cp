package com.xicp.server.quorum;

import com.xicp.ServerCnxn;
import com.xicp.Watcher;
import com.xicp.server.*;
import com.xicp.server.data.Message;
import com.xicp.server.data.Stat;
import com.xicp.server.quorum.Leader.Proposal;
import com.xicp.util.SerializeUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @description: XiCPDatabase
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class XiCPDatabase {

    protected DataTree dataTree;
    protected ConcurrentHashMap<Long, Integer> sessionsWithTimeouts;
    protected FileTxnSnapLog snapLog;
    volatile private boolean initialized = false;
    protected long minCommittedLog, maxCommittedLog;
    public volatile long lastProcessedZxid = 0;
    public static final int commitLogCount = 500;
    protected static int commitLogBuffer = 700;
    protected LinkedList<Proposal> committedLog = new LinkedList<Proposal>();
    protected ReentrantReadWriteLock logLock = new ReentrantReadWriteLock();
    TakeSnapshot takeSnapshot = null;

    public XiCPDatabase(FileTxnSnapLog snapLog) {
        dataTree = new DataTree();
        sessionsWithTimeouts = new ConcurrentHashMap<Long, Integer>();
        this.snapLog = snapLog;
        takeSnapshot = new TakeSnapshot(snapLog, dataTree);
        takeSnapshot.start();
    }

    public void clear() {
        dataTree = new DataTree();
        sessionsWithTimeouts.clear();
        ReentrantReadWriteLock.WriteLock lock = logLock.writeLock();
        try {
            lock.lock();
            committedLog.clear();
        } finally {
            lock.unlock();
        }
        initialized = false;
    }

    public DataTree getDataTree() {
        return this.dataTree;
    }

    public ReentrantReadWriteLock getLogLock() {
        return logLock;
    }

    public ConcurrentHashMap<Long, Integer> getSessionWithTimeOuts() {
        return sessionsWithTimeouts;
    }

    public long loadDataBase() throws IOException {
        long zxid = snapLog.restore(dataTree, sessionsWithTimeouts);
        initialized = true;
        return zxid;
    }

    public void addCommittedProposal(Request request) {
        ReentrantReadWriteLock.WriteLock wl = logLock.writeLock();
        try {
            wl.lock();
            if (committedLog.size() > commitLogCount) {
                committedLog.removeFirst();
                minCommittedLog = committedLog.getFirst().packet.getZxid();
            }
            if (committedLog.size() == 0) {
                minCommittedLog = request.zxid;
                maxCommittedLog = request.zxid;
            }
            byte[] data = SerializeUtils.serializeRequest(request);
            QuorumPacket pp = new QuorumPacket(Leader.PROPOSAL, request.zxid, data);
            Proposal p = new Proposal();
            p.packet = pp;
            p.request = request;
            committedLog.add(p);
            maxCommittedLog = p.packet.getZxid();
        } finally {
            wl.unlock();
        }
    }



    static class TakeSnapshot extends XiCPThread{

        FileTxnSnapLog snapLog;
        DataTree dataTree;

        int dataSize = 0;

        public TakeSnapshot(FileTxnSnapLog snapLog, DataTree dataTree) {
            super("TakeSnapshot ... ");
            this.snapLog = snapLog;
            this.dataTree = dataTree;
        }

        @Override
        public void run() {
            while (true){
                try {
                    Thread.sleep(60000);
                    if (dataSize == dataTree.getNodeCount()) {
                        continue;
                    }
                    dataSize = dataTree.getNodeCount();
                    snapLog.save(dataTree, dataTree.getEphemeralsMap());
                } catch (Exception e) {
                    System.err.println("Severe unrecoverable error, exiting" + e);
                }
            }
        }
    }

    public Message processTxn(Message txn) {
        return dataTree.processTxn(txn);
    }

    public DataNode getNode(String path) {
        return dataTree.getNode(path);
    }

    public byte[] getData(String path, Watcher watcher) throws Exception {
        return dataTree.getData(path, watcher);
    }

    public boolean exists(String path) throws Exception {
        return dataTree.exists(path);
    }

    public void setWatches(List<String> dataWatches, List<String> childWatches, Watcher watcher) {
        dataTree.setWatches(dataWatches, childWatches, watcher);
    }

    public List<String> getChildren(String path, Watcher watcher) throws Exception {
        return dataTree.getChildren(path, watcher);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public long getDataTreeLastProcessedZxid() {
        return dataTree.lastProcessedZxid;
    }
}
