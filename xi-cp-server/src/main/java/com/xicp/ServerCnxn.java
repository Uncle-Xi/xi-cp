package com.xicp;

import com.xicp.server.Record;
import com.xicp.server.data.Message;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ServerCnxn implements Watcher {

    final public static Object me = new Object();
    protected static final AtomicLong packetsReceived = new AtomicLong();
    protected long incrPacketsReceived() {
        return packetsReceived.incrementAndGet();
    }

    Message message;

    public abstract void sendResponse(Record record)
            throws IOException;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
