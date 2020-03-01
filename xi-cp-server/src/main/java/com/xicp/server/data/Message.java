package com.xicp.server.data;

import com.xicp.WatchedEvent;
import com.xicp.server.Record;

import java.util.List;

/**
 * @description: Message
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class Message implements Record {

    private long busId;
    private String path;
    private String content;
    private long clientId;
    private int cxid;
    private long zxid;
    private long time;
    private int type;
    private Boolean exists;
    private boolean sync;
    private WatchedEvent event;
    private List<String> dataWatches;
    private List<String> childWatches;
    private boolean ephemer = false;
    private boolean orderly = false;

    public Message() { }
    public Message(
            long clientId,
            int cxid,
            long zxid,
            long time,
            int type) {
        this.clientId=clientId;
        this.cxid=cxid;
        this.zxid=zxid;
        this.time=time;
        this.type=type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public int getCxid() {
        return cxid;
    }

    public void setCxid(int cxid) {
        this.cxid = cxid;
    }

    public long getZxid() {
        return zxid;
    }

    public void setZxid(long zxid) {
        this.zxid = zxid;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Boolean getExists() {
        return exists;
    }

    public void setExists(Boolean exists) {
        this.exists = exists;
    }

    public List<String> getDataWatches() {
        return dataWatches;
    }

    public void setDataWatches(List<String> dataWatches) {
        this.dataWatches = dataWatches;
    }

    public List<String> getChildWatches() {
        return childWatches;
    }

    public void setChildWatches(List<String> childWatches) {
        this.childWatches = childWatches;
    }

    public boolean isEphemer() {
        return ephemer;
    }

    public void setEphemer(boolean ephemer) {
        this.ephemer = ephemer;
    }

    public long getBusId() {
        return busId;
    }

    public void setBusId(long busId) {
        this.busId = busId;
    }

    public WatchedEvent getEvent() {
        return event;
    }

    public void setEvent(WatchedEvent event) {
        this.event = event;
    }

    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    public boolean isOrderly() {
        return orderly;
    }

    public void setOrderly(boolean orderly) {
        this.orderly = orderly;
    }
}
