package com.xicp;

import com.xicp.config.QuorumPeerConfig;
import com.xicp.server.FileTxnSnapLog;
import com.xicp.server.quorum.QuorumPeer;
import com.xicp.server.quorum.XiCPDatabase;
import com.xicp.server.quorum.XiCPServerMain;

import javax.management.JMException;
import javax.security.sasl.SaslException;
import java.io.File;
import java.io.IOException;

/**
 * @description: QuorumPeerMain
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class QuorumPeerMain {

    protected QuorumPeer quorumPeer;

    public static void main(String[] args) {
        QuorumPeerMain main = new QuorumPeerMain();
        try {
            main.initializeAndRun(args);
        } catch (Exception e) {
            System.err.println(e);
            System.exit(2);
        }
        System.exit(0);
    }


    protected void initializeAndRun(String[] args) throws Exception {
        QuorumPeerConfig config = new QuorumPeerConfig();
        if (args.length == 1) {
            config.parse(args[0]);
        }
        DatadirCleanupManager purgeMgr = new DatadirCleanupManager(
                config.getDataDir(), config.getDataLogDir(), 2, 30);
        purgeMgr.start();

        if (!standalone(config)) {
            runFromConfig(config);
        } else {
            System.out.println("Either no config or no quorum defined in config, running in standalone mode");
            XiCPServerMain.main(args);
        }
    }

    protected boolean standalone(QuorumPeerConfig config){
        if (config == null) {
            return true;
        }
        String startModle = config.getStartModle();
        if (startModle != null && "standalone".equalsIgnoreCase(startModle)) {
            return true;
        }
        return false;
    }

    /**
     * 启动流程：
     * 1）加载配置文件
     * 2）初始化网络
     * 3）节点选举【加载本地数据 - 发起投票 - 数据同步】
     * 4）进入角色开始服务
     * @param config
     * @throws IOException
     */
    public void runFromConfig(QuorumPeerConfig config) throws IOException {
        System.out.println("Starting quorum peer");
        try {
            ServerCnxnFactory cnxnFactory = ServerCnxnFactory.createFactory();
            cnxnFactory.configure(config.getClientPortAddress(),
                    config.getMaxClientCnxns());
            quorumPeer = getQuorumPeer();
            quorumPeer.setTxnFactory(new FileTxnSnapLog(
                    new File(config.getDataLogDir()),
                    new File(config.getDataDir())));
            quorumPeer.setElectionType(config.getElectionAlg());
            quorumPeer.setQuorumPeers(config.getServers());
            quorumPeer.setMyid(config.getServerId());
            quorumPeer.setTickTime(config.getTickTime());
            quorumPeer.setInitLimit(config.getInitLimit());
            quorumPeer.setSyncLimit(config.getSyncLimit());
            quorumPeer.setCnxnFactory(cnxnFactory);
            quorumPeer.setMinSessionTimeout(config.getMinSessionTimeout());
            quorumPeer.setMaxSessionTimeout(config.getMaxSessionTimeout());
            quorumPeer.setXiDb(new XiCPDatabase(quorumPeer.getTxnFactory()));
            quorumPeer.setLearnerType(config.getPeerType());
            quorumPeer.setSyncEnabled(config.getSyncEnabled());
            quorumPeer.initialize();
            quorumPeer.start();
            quorumPeer.join();
        } catch (Exception e) {
            System.err.println("Quorum Peer interrupted" + e);
        }
    }

    protected QuorumPeer getQuorumPeer() throws Exception {
        return new QuorumPeer();
    }
}
