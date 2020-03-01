package com.xicp.server.data;

import com.xicp.server.Record;

/**
 * @description: Stat
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class Stat implements Record {
    private long czxid;
    private long mzxid;
    private long ctime;
    private long mtime;
    private int version;
    private int cversion;
    private int aversion;
    private long ephemeralOwner;
    private int dataLength;
    private int numChildren;
    private long pzxid;

    public Stat() {
    }

    public Stat(
            long czxid,
            long mzxid,
            long ctime,
            long mtime,
            int version,
            int cversion,
            int aversion,
            long ephemeralOwner,
            int dataLength,
            int numChildren,
            long pzxid) {
        this.czxid = czxid;
        this.mzxid = mzxid;
        this.ctime = ctime;
        this.mtime = mtime;
        this.version = version;
        this.cversion = cversion;
        this.aversion = aversion;
        this.ephemeralOwner = ephemeralOwner;
        this.dataLength = dataLength;
        this.numChildren = numChildren;
        this.pzxid = pzxid;
    }

    public long getCzxid() {
        return czxid;
    }

    public void setCzxid(long m_) {
        czxid = m_;
    }

    public long getMzxid() {
        return mzxid;
    }

    public void setMzxid(long m_) {
        mzxid = m_;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long m_) {
        ctime = m_;
    }

    public long getMtime() {
        return mtime;
    }

    public void setMtime(long m_) {
        mtime = m_;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int m_) {
        version = m_;
    }

    public int getCversion() {
        return cversion;
    }

    public void setCversion(int m_) {
        cversion = m_;
    }

    public int getAversion() {
        return aversion;
    }

    public void setAversion(int m_) {
        aversion = m_;
    }

    public long getEphemeralOwner() {
        return ephemeralOwner;
    }

    public void setEphemeralOwner(long m_) {
        ephemeralOwner = m_;
    }

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int m_) {
        dataLength = m_;
    }

    public int getNumChildren() {
        return numChildren;
    }

    public void setNumChildren(int m_) {
        numChildren = m_;
    }

    public long getPzxid() {
        return pzxid;
    }

    public void setPzxid(long m_) {
        pzxid = m_;
    }


    public int compareTo(Object peer_) throws ClassCastException {
        if (!(peer_ instanceof Stat)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        Stat peer = (Stat) peer_;
        int ret = 0;
        ret = (czxid == peer.czxid) ? 0 : ((czxid < peer.czxid) ? -1 : 1);
        if (ret != 0) return ret;
        ret = (mzxid == peer.mzxid) ? 0 : ((mzxid < peer.mzxid) ? -1 : 1);
        if (ret != 0) return ret;
        ret = (ctime == peer.ctime) ? 0 : ((ctime < peer.ctime) ? -1 : 1);
        if (ret != 0) return ret;
        ret = (mtime == peer.mtime) ? 0 : ((mtime < peer.mtime) ? -1 : 1);
        if (ret != 0) return ret;
        ret = (version == peer.version) ? 0 : ((version < peer.version) ? -1 : 1);
        if (ret != 0) return ret;
        ret = (cversion == peer.cversion) ? 0 : ((cversion < peer.cversion) ? -1 : 1);
        if (ret != 0) return ret;
        ret = (aversion == peer.aversion) ? 0 : ((aversion < peer.aversion) ? -1 : 1);
        if (ret != 0) return ret;
        ret = (ephemeralOwner == peer.ephemeralOwner) ? 0 : ((ephemeralOwner < peer.ephemeralOwner) ? -1 : 1);
        if (ret != 0) return ret;
        ret = (dataLength == peer.dataLength) ? 0 : ((dataLength < peer.dataLength) ? -1 : 1);
        if (ret != 0) return ret;
        ret = (numChildren == peer.numChildren) ? 0 : ((numChildren < peer.numChildren) ? -1 : 1);
        if (ret != 0) return ret;
        ret = (pzxid == peer.pzxid) ? 0 : ((pzxid < peer.pzxid) ? -1 : 1);
        if (ret != 0) return ret;
        return ret;
    }

    public boolean equals(Object peer_) {
        if (!(peer_ instanceof Stat)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        Stat peer = (Stat) peer_;
        boolean ret = false;
        ret = (czxid == peer.czxid);
        if (!ret) return ret;
        ret = (mzxid == peer.mzxid);
        if (!ret) return ret;
        ret = (ctime == peer.ctime);
        if (!ret) return ret;
        ret = (mtime == peer.mtime);
        if (!ret) return ret;
        ret = (version == peer.version);
        if (!ret) return ret;
        ret = (cversion == peer.cversion);
        if (!ret) return ret;
        ret = (aversion == peer.aversion);
        if (!ret) return ret;
        ret = (ephemeralOwner == peer.ephemeralOwner);
        if (!ret) return ret;
        ret = (dataLength == peer.dataLength);
        if (!ret) return ret;
        ret = (numChildren == peer.numChildren);
        if (!ret) return ret;
        ret = (pzxid == peer.pzxid);
        if (!ret) return ret;
        return ret;
    }

    @Override
    public int hashCode() {
        int result = 17;
        int ret;
        ret = (int) (czxid ^ (czxid >>> 32));
        result = 37 * result + ret;
        ret = (int) (mzxid ^ (mzxid >>> 32));
        result = 37 * result + ret;
        ret = (int) (ctime ^ (ctime >>> 32));
        result = 37 * result + ret;
        ret = (int) (mtime ^ (mtime >>> 32));
        result = 37 * result + ret;
        ret = (int) version;
        result = 37 * result + ret;
        ret = (int) cversion;
        result = 37 * result + ret;
        ret = (int) aversion;
        result = 37 * result + ret;
        ret = (int) (ephemeralOwner ^ (ephemeralOwner >>> 32));
        result = 37 * result + ret;
        ret = (int) dataLength;
        result = 37 * result + ret;
        ret = (int) numChildren;
        result = 37 * result + ret;
        ret = (int) (pzxid ^ (pzxid >>> 32));
        result = 37 * result + ret;
        return result;
    }

    public static String signature() {
        return "LStat(lllliiiliil)";
    }
}