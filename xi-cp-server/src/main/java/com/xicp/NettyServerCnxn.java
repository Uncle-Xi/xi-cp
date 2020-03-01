package com.xicp;


import com.xicp.server.Record;
import com.xicp.server.XiCPServer;
import com.xicp.server.data.Message;
import com.xicp.util.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @description: NettyServerCnxn
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class NettyServerCnxn extends ServerCnxn {

    ChannelHandlerContext ctx;
    XiCPServer xcs;
    NettyServerCnxnFactory factory;

    NettyServerCnxn(ChannelHandlerContext ctx, XiCPServer xcs, NettyServerCnxnFactory factory) {
        this.ctx = ctx;
        this.xcs = xcs;
        this.factory = factory;
    }

    @Override
    public void process(WatchedEvent event) {
        try {
            System.out.println("[触发了事件通知] [WatchedEvent]=[ " + StringUtils.getString(event));
            if (!ctx.isRemoved()) {
                Message message = new Message();
                message.setEvent(event);
                ctx.writeAndFlush(StringUtils.getString(message));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void receiveMessage() {
        try {
            xcs.processConnectRequest(this);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendResponse(Record r) throws IOException {
        if (!ctx.isRemoved()) {
            String resp = StringUtils.getString(r);
            System.out.println("[NettyServerCnxn] [响应用户]=[ " + resp);
            this.ctx.writeAndFlush(resp);
        }
    }
}
