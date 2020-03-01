package com.xicp;

/**
 * @description: WatchedEvent
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class WatchedEvent {

    final private EventType eventType;
    private String path;
    private Object cnt;

    public WatchedEvent(EventType eventType, String path) {
        this.eventType = eventType;
        this.path = path;
    }

    public WatchedEvent(EventType eventType, String path, Object cnt) {
        this.eventType = eventType;
        this.path = path;
        this.cnt = cnt;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Object getCnt() {
        return cnt;
    }

    public void setCnt(Object cnt) {
        this.cnt = cnt;
    }
}
