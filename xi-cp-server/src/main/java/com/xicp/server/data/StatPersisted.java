package com.xicp.server.data;

import com.xicp.server.Record;

/**
 * @description: StatPersisted
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class StatPersisted implements Record {

    private long czxid;
    private long mzxid;
    private long ctime;
    private long mtime;
    private int version;
    private int cversion;
    private int aversion;
    private long ephemeralOwner;
    private long pzxid;

    public StatPersisted() {
    }
    public StatPersisted(
            long czxid,
            long mzxid,
            long ctime,
            long mtime,
            int version,
            int cversion,
            int aversion,
            long ephemeralOwner,
            long pzxid) {
        this.czxid=czxid;
        this.mzxid=mzxid;
        this.ctime=ctime;
        this.mtime=mtime;
        this.version=version;
        this.cversion=cversion;
        this.aversion=aversion;
        this.ephemeralOwner=ephemeralOwner;
        this.pzxid=pzxid;
    }
    public long getCzxid() {
        return czxid;
    }
    public void setCzxid(long m_) {
        czxid=m_;
    }
    public long getMzxid() {
        return mzxid;
    }
    public void setMzxid(long m_) {
        mzxid=m_;
    }
    public long getCtime() {
        return ctime;
    }
    public void setCtime(long m_) {
        ctime=m_;
    }
    public long getMtime() {
        return mtime;
    }
    public void setMtime(long m_) {
        mtime=m_;
    }
    public int getVersion() {
        return version;
    }
    public void setVersion(int m_) {
        version=m_;
    }
    public int getCversion() {
        return cversion;
    }
    public void setCversion(int m_) {
        cversion=m_;
    }
    public int getAversion() {
        return aversion;
    }
    public void setAversion(int m_) {
        aversion=m_;
    }
    public long getEphemeralOwner() {
        return ephemeralOwner;
    }
    public void setEphemeralOwner(long m_) {
        ephemeralOwner=m_;
    }
    public long getPzxid() {
        return pzxid;
    }
    public void setPzxid(long m_) {
        pzxid=m_;
    }
}
