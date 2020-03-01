package com.xicp.server.quorum;

import com.xicp.server.Record;

/**
 * @description: QuorumPacket
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class QuorumPacket implements Record {
    private int type;
    private long zxid;
    private byte[] data;

    public QuorumPacket(
            int type,
            long zxid,
            byte[] data) {
        this.type = type;
        this.zxid = zxid;
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public void setType(int m_) {
        type = m_;
    }

    public long getZxid() {
        return zxid;
    }

    public void setZxid(long m_) {
        zxid = m_;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] m_) {
        data = m_;
    }
}
