package com.xicp.server.quorum;

import com.xicp.OpCode;
import com.xicp.server.data.Message;
import com.xicp.util.StringUtils;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @description: Learner
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class Learner {

    // TODO 发送心跳包
    protected void ping(QuorumServer quorumServer){
        try {
            InetSocketAddress addr = quorumServer.electionAddr;
            Socket socket = new Socket(addr.getHostName(), addr.getPort());
            OutputStream out = socket.getOutputStream();
            Message message = new Message();
            message.setType(OpCode.ping);
            String voteStr = StringUtils.getString(message);
            out.write(voteStr.getBytes());
            out.flush();
            socket.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    static class Message{
        int type;
        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }
    }
}
