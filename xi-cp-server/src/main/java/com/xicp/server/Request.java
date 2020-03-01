package com.xicp.server;

import com.xicp.OpCode;
import com.xicp.ServerCnxn;

import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.List;

/**
 * @description: Request
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class Request {

    public Request(ServerCnxn cnxn, long sessionId, int type, int xid) {
        this.cnxn = cnxn;
        this.sessionId = sessionId;
        this.cxid = xid;
        this.type = type;
    }

    public final long sessionId;
    public final int cxid;
    public final int type;
    public final ServerCnxn cnxn;
    public Record txn;
    public long zxid = -1;
    public final long createTime = System.nanoTime() / 1000000;;
    private Object owner;
    static boolean isValid(int type) {
        switch (type) {
            case OpCode.notification:
                return false;
            case OpCode.create:
            case OpCode.delete:
            case OpCode.createSession:
            case OpCode.exists:
            case OpCode.getData:
            case OpCode.check:
            case OpCode.multi:
            case OpCode.setData:
            case OpCode.sync:
            case OpCode.getACL:
            case OpCode.setACL:
            case OpCode.getChildren:
            case OpCode.getChildren2:
            case OpCode.ping:
            case OpCode.closeSession:
            case OpCode.setWatches:
                return true;
            default:
                return false;
        }
    }

    static boolean isQuorum(int type) {
        switch (type) {
            case OpCode.exists:
            case OpCode.getACL:
            case OpCode.getChildren:
            case OpCode.getChildren2:
            case OpCode.getData:
                return false;
            case OpCode.error:
            case OpCode.closeSession:
            case OpCode.create:
            case OpCode.createSession:
            case OpCode.delete:
            case OpCode.setACL:
            case OpCode.setData:
            case OpCode.check:
            case OpCode.multi:
                return true;
            default:
                return false;
        }
    }

    static String op2String(int op) {
        switch (op) {
            case OpCode.notification:
                return "notification";
            case OpCode.create:
                return "create";
            case OpCode.setWatches:
                return "setWatches";
            case OpCode.delete:
                return "delete";
            case OpCode.exists:
                return "exists";
            case OpCode.getData:
                return "getData";
            case OpCode.check:
                return "check";
            case OpCode.multi:
                return "multi";
            case OpCode.setData:
                return "setData";
            case OpCode.sync:
                return "sync:";
            case OpCode.getACL:
                return "getACL";
            case OpCode.setACL:
                return "setACL";
            case OpCode.getChildren:
                return "getChildren";
            case OpCode.getChildren2:
                return "getChildren2";
            case OpCode.ping:
                return "ping";
            case OpCode.createSession:
                return "createSession";
            case OpCode.closeSession:
                return "closeSession";
            case OpCode.error:
                return "error";
            default:
                return "unknown " + op;
        }
    }


    public long getSessionId() {
        return sessionId;
    }

    public int getCxid() {
        return cxid;
    }

    public int getType() {
        return type;
    }

    public ServerCnxn getCnxn() {
        return cnxn;
    }

    public Record getTxn() {
        return txn;
    }

    public void setTxn(Record txn) {
        this.txn = txn;
    }

    public long getZxid() {
        return zxid;
    }

    public void setZxid(long zxid) {
        this.zxid = zxid;
    }

    public long getCreateTime() {
        return createTime;
    }

    public Object getOwner() {
        return owner;
    }

    public void setOwner(Object owner) {
        this.owner = owner;
    }
}
