package com.xicp.server.quorum;

import com.xicp.server.XiCPThread;

import javax.transaction.xa.Xid;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

import static com.xicp.server.quorum.ServerState.*;

/**
 * @description: ResponderThread
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class ResponderThread extends XiCPThread {

    DatagramSocket udpSocket;
    volatile private Vote currentVote;
    private ServerState state;
    private long myid;

    public ResponderThread(DatagramSocket udpSocket,
                           Vote currentVote,
                           ServerState state,
                           long myid) {
        super("ResponderThread");
        this.udpSocket = udpSocket;
        this.currentVote = currentVote;
        this.state = state;
        this.myid = myid;
    }

    volatile boolean running = true;

    @Override
    public void run() {
        try {
            byte b[] = new byte[36];
            ByteBuffer responseBuffer = ByteBuffer.wrap(b);
            DatagramPacket packet = new DatagramPacket(b, b.length);
            while (running) {
                udpSocket.receive(packet);
                if (packet.getLength() != 4) {
                    System.err.println("Got more than just an xid! Len = " + packet.getLength());
                } else {
                    responseBuffer.clear();
                    responseBuffer.getInt(); // Skip the xid
                    responseBuffer.putLong(myid);
                    Vote current = currentVote;
                    switch (state) {
                        case LOOKING:
                            responseBuffer.putLong(current.getId());
                            responseBuffer.putLong(current.getZxid());
                            break;
                        case LEADING:
                            try {
                                long proposed;

                            } catch (NullPointerException npe) {
                            }
                            break;
                        case FOLLOWING:
                            responseBuffer.putLong(current.getId());
                            try {

                            } catch (NullPointerException npe) {
                            }
                            break;
                        case OBSERVING:
                            break;
                    }
                    packet.setData(b);
                    udpSocket.send(packet);
                }
                packet.setLength(b.length);
            }
        } catch (RuntimeException e) {
            System.err.println("Unexpected runtime exception in ResponderThread" + e);
        } catch (IOException e) {
            System.err.println("Unexpected IO exception in ResponderThread" + e);
        } finally {
            System.err.println("QuorumPeer responder thread exited");
        }
    }
}
