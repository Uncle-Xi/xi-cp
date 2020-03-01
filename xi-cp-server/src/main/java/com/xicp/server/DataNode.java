package com.xicp.server;

import com.xicp.server.data.Stat;
import com.xicp.server.data.StatPersisted;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @description: DataNode
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class DataNode implements Record {

    DataNode parent;
    byte data[];
    Long acl;
    private Set<String> children = null;
    private static final Set<String> EMPTY_SET = Collections.emptySet();
    public StatPersisted stat;

    DataNode() {
        // default constructor
    }

    public DataNode(DataNode parent, byte data[], Long acl, StatPersisted stat) {
        this.parent = parent;
        this.data = data;
        this.acl = acl;
        this.stat = stat;
    }

    public synchronized boolean addChild(String child) {
        if (children == null) {
            children = new HashSet<String>(8);
        }
        return children.add(child);
    }

    public synchronized boolean removeChild(String child) {
        if (children == null) {
            return false;
        }
        return children.remove(child);
    }

    public synchronized void setChildren(HashSet<String> children) {
        this.children = children;
    }

    public synchronized Set<String> getChildren() {
        if (children == null) {
            return EMPTY_SET;
        }

        return Collections.unmodifiableSet(children);
    }

    synchronized public void copyStat(Stat to) {
        to.setAversion(stat.getAversion());
        to.setCtime(stat.getCtime());
        to.setCzxid(stat.getCzxid());
        to.setMtime(stat.getMtime());
        to.setMzxid(stat.getMzxid());
        to.setPzxid(stat.getPzxid());
        to.setVersion(stat.getVersion());
        to.setEphemeralOwner(stat.getEphemeralOwner());
        to.setDataLength(data == null ? 0 : data.length);
        int numChildren = 0;
        if (this.children != null) {
            numChildren = children.size();
        }
        to.setCversion(stat.getCversion()*2 - numChildren);
        to.setNumChildren(numChildren);
    }

    public DataNode getParent() {
        return parent;
    }

    public void setParent(DataNode parent) {
        this.parent = parent;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setChildren(Set<String> children) {
        this.children = children;
    }
}
