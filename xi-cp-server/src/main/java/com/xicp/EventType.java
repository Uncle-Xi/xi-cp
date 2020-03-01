package com.xicp;

public enum EventType {
    None(-1),
    NodeCreated(1),
    NodeDeleted(2),
    NodeDataChanged(3),
    NodeChildrenChanged(4);

    private final int intValue;     // Integer representation of value
    // for sending over wire

    EventType(int intValue) {
        this.intValue = intValue;
    }

    public int getIntValue() {
        return intValue;
    }

    public static EventType fromInt(int intValue) {
        switch (intValue) {
            case -1: return EventType.None;
            case  1: return EventType.NodeCreated;
            case  2: return EventType.NodeDeleted;
            case  3: return EventType.NodeDataChanged;
            case  4: return EventType.NodeChildrenChanged;
            default:
                throw new RuntimeException("Invalid integer value for conversion to EventType");
        }
    }
}
