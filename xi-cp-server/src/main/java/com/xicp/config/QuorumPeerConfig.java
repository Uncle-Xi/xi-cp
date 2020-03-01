package com.xicp.config;

import com.xicp.server.quorum.QuorumPeer;
import com.xicp.server.quorum.QuorumServer;
import com.xicp.util.TransferFile;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @description: QuorumPeerConfig
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class QuorumPeerConfig {

    protected InetSocketAddress clientPortAddress;
    private int tickTime;
    protected int initLimit;
    private int syncLimit;
    private int clientPort;
    protected long serverId;
    protected int myId;
    private String dataDir;
    private String startModle;
    protected String dataLogDir;
    protected int electionAlg = 3;
    protected int minSessionTimeout = -1;
    protected int maxSessionTimeout = -1;
    protected int maxClientCnxns = 60;
    private List<String> serverIds;
    public final HashMap<Long, QuorumServer> servers = new HashMap<>();
    protected final HashMap<Long, QuorumServer> observers = new HashMap<>();
    protected QuorumPeer.LearnerType peerType = QuorumPeer.LearnerType.PARTICIPANT;
    protected boolean syncEnabled = true;

    public QuorumPeerConfig() {

    }

    public void parse(String path) throws Exception {
        File configFile = TransferFile.getTransferFile(new File(path), path);
        System.out.println("Reading configuration from: " + configFile);
        try {
            if (!configFile.exists()) {
                throw new IllegalArgumentException(configFile.toString() + " file is missing");
            }
            Properties cfg = new Properties();
            FileInputStream in = new FileInputStream(configFile);
            try {
                cfg.load(in);
            } finally {
                in.close();
            }
            parseProperties(cfg);
        } catch (Exception e) {
            //throw new RuntimeException("Error processing " + path, e);
            e.printStackTrace();
        } finally {
            TransferFile.deleteFile(configFile);
        }
    }

    public void parseProperties(Properties cpProp) throws Exception {
        int clientPort = 0;
        String clientPortAddress = null;
        for (Map.Entry<Object, Object> entry : cpProp.entrySet()) {
            String key = entry.getKey().toString().trim();
            String value = entry.getValue().toString().trim();
            if (key.equals("dataDir")) {
                dataDir = value;
            } else if (key.equals("clientPort")) {
                clientPort = Integer.parseInt(value);
            } else if (key.equals("clientPortAddress")) {
                clientPortAddress = value.trim();
            } else if (key.equals("startModle")) {
                startModle = value.trim();
            } else if (key.equals("tickTime")) {
                tickTime = Integer.parseInt(value);
            } else if (key.equals("initLimit")) {
                initLimit = Integer.parseInt(value);
            } else if (key.equals("my.id")) {
                myId = Integer.parseInt(value);
                serverId = myId;
            } else if (key.equals("minSessionTimeout")) {
                minSessionTimeout = Integer.parseInt(value);
            } else if (key.equals("maxSessionTimeout")) {
                maxSessionTimeout = Integer.parseInt(value);
            } else if (key.equals("syncLimit")) {
                syncLimit = Integer.parseInt(value);
            } else if (key.startsWith("server.")) {
                int dot = key.indexOf('.');
                long sid = Long.parseLong(key.substring(dot + 1));
                String parts[] = splitWithLeadingHostname(value);
                if (parts.length > 4 || parts.length < 2) {
                    System.out.println(value + " does not have the form host:port or host:port:port or host:port:port:type");
                }
                QuorumPeer.LearnerType type = null;
                String hostname = parts[0];
                Integer port = Integer.parseInt(parts[1]);
                Integer electionPort = null;
                if (parts.length > 2) {
                    electionPort = Integer.parseInt(parts[2]);
                }
                if (parts.length > 3) {
                    if (parts[3].toLowerCase().equals("observer")) {
                        type = QuorumPeer.LearnerType.OBSERVER;
                    } else if (parts[3].toLowerCase().equals("participant")) {
                        type = QuorumPeer.LearnerType.PARTICIPANT;
                    } else {
                        throw new Exception("Unrecognised peertype: " + value);
                    }
                }
                if (type == QuorumPeer.LearnerType.OBSERVER) {
                    observers.put(Long.valueOf(sid), new QuorumServer(sid, hostname, port, electionPort, type));
                } else {
                    servers.put(Long.valueOf(sid), new QuorumServer(sid, hostname, port, electionPort, type));
                }
            } else {
                System.setProperty("zookeeper." + key, value);
            }
        }
        if (dataDir == null) {
            throw new IllegalArgumentException("dataDir is not set");
        }
        if (dataLogDir == null) {
            dataLogDir = dataDir;
        }
        if (clientPort == 0) {
            throw new IllegalArgumentException("clientPort is not set");
        }
        if (clientPortAddress != null) {
            this.clientPortAddress = new InetSocketAddress(
                    InetAddress.getByName(clientPortAddress), clientPort);
        } else {
            this.clientPortAddress = new InetSocketAddress(clientPort);
        }
        if (tickTime == 0) {
            throw new IllegalArgumentException("tickTime is not set");
        }
        if (minSessionTimeout > maxSessionTimeout) {
            throw new IllegalArgumentException("minSessionTimeout must not be larger than maxSessionTimeout");
        }
        if (servers.size() == 0) {
            if (observers.size() > 0) {
                throw new IllegalArgumentException("Observers w/o participants is an invalid configuration");
            }
            return;
        } else if (servers.size() == 1) {
            if (observers.size() > 0) {
                throw new IllegalArgumentException("Observers w/o quorum is an invalid configuration");
            }
            System.out.println("Invalid configuration, only one server specified (ignoring)");
            servers.clear();
        } else if (servers.size() > 1) {
            if (servers.size() == 2) {
                System.out.println("No server failure will be tolerated. " + "You need at least 3 servers.");
            } else if (servers.size() % 2 == 0) {
                System.out.println("Non-optimial configuration, consider an odd number of servers.");
            }
            if (initLimit == 0) {
                throw new IllegalArgumentException("initLimit is not set");
            }
            if (syncLimit == 0) {
                throw new IllegalArgumentException("syncLimit is not set");
            }
            servers.putAll(observers);
            QuorumPeer.LearnerType roleByServersList = observers.containsKey(serverId) ?
                    QuorumPeer.LearnerType.OBSERVER : QuorumPeer.LearnerType.PARTICIPANT;
            if (roleByServersList != peerType) {
                System.out.println("Peer type from servers list (" + roleByServersList
                        + ") doesn't match peerType (" + peerType + "). Defaulting to servers list.");
                peerType = roleByServersList;
            }
        }
    }

    private static String[] splitWithLeadingHostname(String s) throws Exception {
        if (s.startsWith("[")) {
            int i = s.indexOf("]:");
            if (i < 0) {
                throw new RuntimeException(s + " starts with '[' but has no matching ']:'");
            }
            String[] sa = s.substring(i + 2).split(":");
            String[] nsa = new String[sa.length + 1];
            nsa[0] = s.substring(1, i);
            System.arraycopy(sa, 0, nsa, 1, sa.length);
            return nsa;
        } else {
            return s.split(":");
        }
    }

    public boolean getSyncEnabled() { return syncEnabled; }

    public InetSocketAddress getClientPortAddress() {
        return clientPortAddress;
    }

    public int getTickTime() {
        return tickTime;
    }

    public int getInitLimit() {
        return initLimit;
    }

    public int getElectionAlg() { return electionAlg; }

    public int getSyncLimit() {
        return syncLimit;
    }

    public long getServerId() {
        return serverId;
    }

    public String getDataDir() {
        return dataDir;
    }

    public String getDataLogDir() {
        return dataLogDir;
    }

    public int getMinSessionTimeout() {
        return minSessionTimeout;
    }

    public int getMaxSessionTimeout() {
        return maxSessionTimeout;
    }

    public HashMap<Long, QuorumServer> getServers() {
        return servers;
    }

    public QuorumPeer.LearnerType getPeerType() {
        return peerType;
    }

    public int getMaxClientCnxns() {
        return maxClientCnxns;
    }

    public String getStartModle() {
        return startModle;
    }

    public void setStartModle(String startModle) {
        this.startModle = startModle;
    }
}
