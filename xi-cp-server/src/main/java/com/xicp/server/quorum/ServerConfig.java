package com.xicp.server.quorum;

import com.xicp.config.QuorumPeerConfig;

import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * @description: ServerConfig
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class ServerConfig {

    public static final int DEFAULT_TICK_TIME = 3000;

    protected InetSocketAddress clientPortAddress;
    protected String dataDir;
    protected String dataLogDir;
    protected int tickTime = DEFAULT_TICK_TIME;
    protected int maxClientCnxns;
    protected int minSessionTimeout = -1;
    protected int maxSessionTimeout = -1;

    public void parse(String[] args) {
        if (args.length < 2 || args.length > 4) {
            throw new IllegalArgumentException("Invalid number of arguments:" + Arrays.toString(args));
        }
        clientPortAddress = new InetSocketAddress(Integer.parseInt(args[0]));
        dataDir = args[1];
        dataLogDir = dataDir;
        if (args.length >= 3) {
            tickTime = Integer.parseInt(args[2]);
        }
        if (args.length == 4) {
            maxClientCnxns = Integer.parseInt(args[3]);
        }
    }

    public void parse(String path) throws Exception {
        QuorumPeerConfig config = new QuorumPeerConfig();
        config.parse(path);
        readFrom(config);
    }

    public void readFrom(QuorumPeerConfig config) {
        clientPortAddress = config.getClientPortAddress();
        dataDir = config.getDataDir();
        dataLogDir = config.getDataLogDir();
        tickTime = config.getTickTime();
        maxClientCnxns = config.getMaxClientCnxns();
        minSessionTimeout = config.getMinSessionTimeout();
        maxSessionTimeout = config.getMaxSessionTimeout();
    }

    public InetSocketAddress getClientPortAddress() {
        return clientPortAddress;
    }
    public String getDataDir() { return dataDir; }
    public String getDataLogDir() { return dataLogDir; }
    public int getTickTime() { return tickTime; }
    public int getMaxClientCnxns() { return maxClientCnxns; }
    public int getMinSessionTimeout() { return minSessionTimeout; }
    public int getMaxSessionTimeout() { return maxSessionTimeout; }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "clientPortAddress=" + clientPortAddress +
                ", dataDir='" + dataDir + '\'' +
                ", dataLogDir='" + dataLogDir + '\'' +
                ", tickTime=" + tickTime +
                ", maxClientCnxns=" + maxClientCnxns +
                ", minSessionTimeout=" + minSessionTimeout +
                ", maxSessionTimeout=" + maxSessionTimeout +
                '}';
    }
}
