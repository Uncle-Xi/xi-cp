package com.xicp.server.quorum;

import com.xicp.config.QuorumPeerConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * @description: QuorumServer
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class QuorumServer {
    private QuorumServer(long id, InetSocketAddress addr,
                         InetSocketAddress electionAddr) {
        this.id = id;
        this.addr = addr;
        this.electionAddr = electionAddr;
    }

    public QuorumServer(long id, InetSocketAddress addr) {
        this.id = id;
        this.addr = addr;
        this.electionAddr = null;
    }

    private QuorumServer(long id, InetSocketAddress addr,
                         InetSocketAddress electionAddr, QuorumPeer.LearnerType type) {
        this.id = id;
        this.addr = addr;
        this.electionAddr = electionAddr;
        this.type = type;
    }

    public QuorumServer(long id, String hostname,
                        Integer port, Integer electionPort,
                        QuorumPeer.LearnerType type) {
        this.id = id;
        this.hostname = hostname;
        if (port != null) {
            this.port = port;
        }
        if (electionPort != null) {
            this.electionPort = electionPort;
        }
        if (type != null) {
            this.type = type;
        }
        this.recreateSocketAddresses();
    }

    public void recreateSocketAddresses() {
        InetAddress address = null;
        try {
            int ipReachableTimeout = 0;
            String ipReachableValue = System.getProperty("zookeeper.ipReachableTimeout");
            if (ipReachableValue != null) {
                try {
                    ipReachableTimeout = Integer.parseInt(ipReachableValue);
                } catch (NumberFormatException e) {
                    System.out.printf("{%s} is not a valid number\n", ipReachableValue);
                }
            }
            if (ipReachableTimeout <= 0) {
                address = InetAddress.getByName(this.hostname);
            } else {
                address = getReachableAddress(this.hostname, ipReachableTimeout);
            }
            System.out.printf("Resolved hostname: {%s} to address: {%s}\n", this.hostname, address);
            this.addr = new InetSocketAddress(address, this.port);
            if (this.electionPort > 0) {
                this.electionAddr = new InetSocketAddress(address, this.electionPort);
            }
        } catch (UnknownHostException ex) {
            System.out.printf("Failed to resolve address: {%s} - {%s}\n" + this.hostname, ex);
            if (this.addr != null) {
                return;
            }
            this.addr = InetSocketAddress.createUnresolved(this.hostname, this.port);
            if (this.electionPort > 0) {
                this.electionAddr = InetSocketAddress.createUnresolved(this.hostname,
                        this.electionPort);
            }
        }
    }

    public InetAddress getReachableAddress(String hostname, int timeout)
            throws UnknownHostException {
        InetAddress[] addresses = InetAddress.getAllByName(hostname);
        for (InetAddress a : addresses) {
            try {
                if (a.isReachable(timeout)) {
                    return a;
                }
            } catch (IOException e) {
                System.out.println("IP address {} is unreachable" + a);
            }
        }
        return addresses[0];
    }

    public InetSocketAddress addr;

    public InetSocketAddress electionAddr;

    public String hostname;

    public int port = 2888;

    public int electionPort = -1;

    public long id;

    public QuorumPeer.LearnerType type = QuorumPeer.LearnerType.PARTICIPANT;

}
