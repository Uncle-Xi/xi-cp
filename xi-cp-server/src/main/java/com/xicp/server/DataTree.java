package com.xicp.server;

import com.xicp.*;
import com.xicp.server.data.Message;
import com.xicp.server.data.Stat;
import com.xicp.server.data.StatPersisted;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: DataTree
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class DataTree {

    private final ConcurrentHashMap<String, DataNode> nodes = new ConcurrentHashMap<>();
    private final Map<Long, HashSet<String>> ephemerals = new ConcurrentHashMap<>(); // 为每个节点创建 sessionId
    private final WatchManager dataWatches = new WatchManager();
    private final WatchManager childWatches = new WatchManager();
    private static final String rootXiCP = "/";
    private DataNode root = new DataNode(null, new byte[0], -1L, new StatPersisted());
    public volatile long lastProcessedZxid = 0;
    private static final String procXiCP = Quotas.procXiCP;
    private static final String quotaXiCP = Quotas.quotaXiCP;
    private static final String procChildXiCP = procXiCP.substring(1);
    private static final String quotaChildXiCP = quotaXiCP.substring(procXiCP.length() + 1);
    private DataNode procDataNode = new DataNode(root, new byte[0], -1L, new StatPersisted());
    private DataNode quotaDataNode = new DataNode(procDataNode, new byte[0], -1L, new StatPersisted());

    public DataTree() {
        nodes.put("", root);
        nodes.put(rootXiCP, root);
        root.addChild(procChildXiCP);
        nodes.put(procXiCP, procDataNode);
        procDataNode.addChild(quotaChildXiCP);
        nodes.put(quotaXiCP, quotaDataNode);
    }

    public void addDataNode(String path, DataNode node) {
        nodes.put(path, node);
    }

    public DataNode getNode(String path) {
        return nodes.get(path);
    }

    public ConcurrentHashMap<String, DataNode> getNodes(){
        return nodes;
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getWatchCount() {
        return dataWatches.size() + childWatches.size();
    }

    public int getEphemeralsCount() {
        Map<Long, HashSet<String>> map = this.getEphemeralsMap();
        int result = 0;
        for (HashSet<String> set : map.values()) {
            result += set.size();
        }
        return result;
    }

    public HashSet<String> getEphemerals(long sessionId) {
        HashSet<String> retv = ephemerals.get(sessionId);
        if (retv == null) {
            return new HashSet<String>();
        }
        HashSet<String> cloned = null;
        synchronized (retv) {
            cloned = (HashSet<String>) retv.clone();
        }
        return cloned;
    }

    public Map<Long, HashSet<String>> getEphemeralsMap() {
        return ephemerals;
    }

    public Collection<Long> getSessions() {
        return ephemerals.keySet();
    }


    public void updateBytes(String lastPrefix, long diff) {
        String statNode = Quotas.statPath(lastPrefix);
        DataNode node = nodes.get(statNode);
    }

    public void updateCount(String lastPrefix, int diff) {
        String statNode = Quotas.statPath(lastPrefix);
    }

    public String createNode(String path, byte data[],
                             long ephemeralOwner,
                             long zxid, long time)
            throws Exception {
        int lastSlash = path.lastIndexOf('/');
        String parentName = path.substring(0, lastSlash);
        String childName = path.substring(lastSlash + 1);
        StatPersisted stat = new StatPersisted();
        stat.setCtime(time);
        stat.setEphemeralOwner(ephemeralOwner);
        DataNode parent = nodes.get(parentName);
        //System.out.println("parentName -> " + parentName);
        if (parent == null) {
            throw new RuntimeException("[createNode] [path]=[" + path + "],[父节点]=[ " + parentName + " ]不存在");
        }
        synchronized (parent) {
            Set<String> children = parent.getChildren();
            if (children.contains(childName)) {
                throw new RuntimeException("[createNode][节点已存在] [path]=[" + childName);
//                System.out.println("[createNode][节点已存在] - [" + childName);
//                return path;
            }
            parent.stat.setPzxid(zxid);
            DataNode child = new DataNode(parent, data, 0L, stat);
            parent.addChild(childName);
            nodes.put(path, child);
            if (ephemeralOwner != 0) {
                HashSet<String> list = ephemerals.get(ephemeralOwner);
                if (list == null) {
                    list = new HashSet<>();
                    ephemerals.put(ephemeralOwner, list);
                }
                synchronized (list) {
                    list.add(path);
                }
            }
        }
        updateCount("", 1);
        updateBytes("", data == null ? 0 : data.length);
        dataWatches.triggerWatch(path, EventType.NodeCreated);
        childWatches.triggerWatch(parentName.equals("") ? "/" : parentName, EventType.NodeChildrenChanged);

        //System.out.println("createNode -> " + exists(path));
        //System.out.println("createNode -> " + path);
        //System.out.println("createNode -> " + nodes);
        return path;
    }

    public void setWatches(List<String> dataWatches,
                           List<String> childWatches,
                           Watcher watcher) {
        for (String path : dataWatches) {
            DataNode node = getNode(path);
            this.dataWatches.addWatch(path, watcher);
        }
        for (String path : childWatches) {
            DataNode node = getNode(path);
            this.childWatches.addWatch(path, watcher);
        }
    }

    public Stat setData(String path, byte data[], long zxid, long time) throws RuntimeException {
        Stat s = new Stat();
        DataNode n = nodes.get(path);
        if (n == null) {
            throw new RuntimeException("[setData][节点不存在] - [" + path);
        }
        byte lastdata[] = null;
        synchronized (n) {
            lastdata = n.data;
            n.data = data;
            n.stat.setMtime(time);
            n.stat.setMzxid(zxid);
            n.copyStat(s);
        }
        this.updateBytes("监控节点", 0);
        dataWatches.triggerWatch(path, EventType.NodeDataChanged);
        return s;
    }

    public List<String> getChildren(String path, Watcher watcher) throws RuntimeException {
        DataNode n = nodes.get(path);
        //System.out.println("getChildren path -> " + path);
        if (n == null) {
            //for (String key : nodes.keySet()) {
            //    System.out.println("key -> " + key);
            //}
            throw new RuntimeException("[getChildren][节点不存在] - [" + path);
        }
        synchronized (n) {
            List<String> children = new ArrayList<>(n.getChildren());
            if (watcher != null) {
                childWatches.addWatch(path, watcher);
            }
            return children;
        }
    }

    public boolean exists(String path){
        DataNode n = nodes.get(path);
        //System.out.println("exists -> " + (n != null));
        //System.out.println("exists -> " + path);
        //System.out.println("exists -> " + nodes);
        return n != null;
    }

    public byte[] getData(String path, Watcher watcher) throws RuntimeException {
        DataNode n = nodes.get(path);
        if (n == null) {
            throw new RuntimeException("[getData][节点不存在] - [" + path);
        }
        synchronized (n) {
            if (watcher != null) {
                dataWatches.addWatch(path, watcher);
            }
            return n.data;
        }
    }

    public void deleteNode(String path, long zxid) throws RuntimeException {
        int lastSlash = path.lastIndexOf('/');
        String parentName = path.substring(0, lastSlash);
        String childName = path.substring(lastSlash + 1);
        DataNode node = nodes.get(path);
        if (node == null) {
            throw new RuntimeException("[deleteNode][节点不存在] - [" + path);
        }
        nodes.remove(path);
        DataNode parent = nodes.get(parentName);
        if (parent == null) {
            throw new RuntimeException("[deleteNode][父节点不存在] - [" + path);
        }
        synchronized (parent) {
            parent.removeChild(childName);
            parent.stat.setPzxid(zxid);
            long eowner = node.stat.getEphemeralOwner();
            if (eowner != 0) {
                HashSet<String> nodes = ephemerals.get(eowner);
                if (nodes != null) {
                    synchronized (nodes) {
                        nodes.remove(path);
                    }
                }
            }
            node.parent = null;
        }
        //System.out.println("触发监听");
        Set<Watcher> processed = dataWatches.triggerWatch(path, EventType.NodeDeleted);
        //childWatches.triggerWatch(path, EventType.NodeDeleted, processed);
        childWatches.triggerWatch(parentName.equals("") ? "/" : parentName, EventType.NodeChildrenChanged);
    }

    public Message processTxn(Message txn) {
        Message rc = txn;
        try {
            lastProcessedZxid ++;
            switch (txn.getType()) {
                case OpCode.create:
                    //System.out.println("OpCode.create -> " + txn.getPath());
                    createNode(txn.getPath(), txn.getContent().getBytes(),
                            txn.getClientId(), txn.getZxid(), txn.getTime());
                    break;
                case OpCode.delete:
                    //System.out.println("OpCode.delete -> " + txn.getPath());
                    deleteNode(txn.getPath(), txn.getZxid());
                    break;
                case OpCode.setData:
                    //System.out.println("OpCode.setData -> " + txn.getPath());
                    setData(txn.getPath(), txn.getContent().getBytes(), txn.getZxid(), txn.getTime());
                    break;
                case OpCode.closeSession:
                    //System.out.println("OpCode.closeSession -> " + txn.getPath());
                    killSession(txn.getClientId(), txn.getZxid());
                    break;
            }
            rc.setContent("OK");
        } catch (Exception e) {
            e.printStackTrace();
            rc.setContent(e.getMessage());
        }
        return rc;
    }

    void killSession(long session, long zxid) {
        HashSet<String> list = ephemerals.remove(session);
        if (list != null) {
            for (String path : list) {
                try {
                    deleteNode(path, zxid);
                    System.out.println("Deleting ephemeral node " + path + " for session 0x" + Long.toHexString(session));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}