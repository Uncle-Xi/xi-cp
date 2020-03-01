package com.xicp.server.quorum;

import com.xicp.ServerCnxnFactory;
import com.xicp.server.FileTxnSnapLog;
import com.xicp.server.XiCPThread;

import javax.security.sasl.SaslException;
import java.io.*;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @description: QuorumPeer
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class QuorumPeer extends XiCPThread implements QuorumStats.Provider {

    protected long electionTimeTaken = -1;
    protected int minSessionTimeout = -1;
    protected int maxSessionTimeout = -1;
    protected int initLimit;
    protected int syncLimit;
    protected boolean syncEnabled = true;
    protected AtomicInteger tick = new AtomicInteger();
    protected int tickTime;
    protected XiCPDatabase xiDb;
    private long myid;
    private FileTxnSnapLog logFactory = null;
    protected Map<Long, QuorumServer> quorumPeers;
    Election electionAlg;
    protected QuorumServer master;
    ServerCnxnFactory cnxnFactory;
    private QuorumStats quorumStats;
    private LearnerType learnerType = LearnerType.PARTICIPANT;
    private long acceptedEpoch = -1;
    private long currentEpoch = -1;
    public static final String CURRENT_EPOCH_FILENAME = "currentEpoch";
    public static final String ACCEPTED_EPOCH_FILENAME = "acceptedEpoch";
    public static final String UPDATING_EPOCH_FILENAME = "updatingEpoch";

    private InetSocketAddress myQuorumAddr;
    private QuorumServer myServer;
    private int electionType;
    DatagramSocket udpSocket;
    ResponderThread responder;
    QuorumCnxManager qcm;
    volatile boolean running = true;
    volatile private Vote currentVote;
    private QuorumVerifier quorumConfig;

    public QuorumPeer() throws SaslException {
        super("QuorumPeer");
        quorumStats = new QuorumStats(this);
        initialize();
    }

    public void setLearnerType(LearnerType p) {
        learnerType = p;
        if (quorumPeers.containsKey(this.myid)) {
            this.quorumPeers.get(myid).type = p;
        } else {
            System.out.println("Setting LearnerType to " + p + " but " + myid + " not in QuorumPeers. ");
        }
    }

    public void initialize() throws SaslException {
        // TODO 构建安全网络
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    volatile boolean ready = false;

    @Override
    public synchronized void start() {
        loadDataBase();
        cnxnFactory.start();
        startLeaderElection();
        super.start();
    }

    private void loadDataBase() {
        File updating = new File(getTxnFactory().getSnapDir(), UPDATING_EPOCH_FILENAME);
        try {
            // TODO
            System.out.println(" TODO loadDataBase -> 将快照加载到内存，同时启动定时文件快照功能...");
            xiDb.loadDataBase();
        } catch (IOException ie) {
            throw new RuntimeException("Unable to run quorum server ", ie);
        }
    }

    @Override
    public String getServerState() {
        switch (getPeerState()) {
            case LOOKING:
                return QuorumStats.Provider.LOOKING_STATE;
            case LEADING:
                return QuorumStats.Provider.LEADING_STATE;
            case FOLLOWING:
                return QuorumStats.Provider.FOLLOWING_STATE;
            case OBSERVING:
                return QuorumStats.Provider.OBSERVING_STATE;
        }
        return QuorumStats.Provider.UNKNOWN_STATE;
    }

    private ServerState state = ServerState.LOOKING;

    public synchronized void setPeerState(ServerState newState) {
        state = newState;
    }

    public synchronized ServerState getPeerState() {
        return state;
    }

    public enum LearnerType {
        PARTICIPANT, OBSERVER;
    }

    public long getLastLoggedZxid() {
        if (!xiDb.isInitialized()) {
            loadDataBase();
        }
        return xiDb.getDataTreeLastProcessedZxid();
    }

    synchronized public void startLeaderElection() {
        try {
            currentVote = new Vote(myid, getLastLoggedZxid(), getCurrentEpoch());
        } catch (IOException e) {
            RuntimeException re = new RuntimeException(e.getMessage());
            re.setStackTrace(e.getStackTrace());
            throw re;
        }
        for (QuorumServer server : getView().values()) {
            System.out.println("startLeaderElection -> " + server.addr);
            if (server.id == myid) {
                myQuorumAddr = server.addr;
                myServer = server;
                break;
            }
        }
        if (myQuorumAddr == null) {
            throw new RuntimeException("My id " + myid + " not in the peer list");
        }
        this.electionAlg = createElectionAlgorithm(electionType);
    }

    protected Election createElectionAlgorithm(int electionAlgorithm) {
        Election le = null;
        try {
            switch (electionAlgorithm) {
                case 3:
                    qcm = new QuorumCnxManager(this);
                    QuorumCnxManager.Listener listener = qcm.listener;
                    if (listener != null) {
                        listener.start();
                        le = new FastLeaderElection(this, qcm);
                    } else {
                        System.err.println("Null listener when initializing cnx manager");
                    }
                    break;
                default:
                    assert false;
            }
        } catch (Exception e){
            System.out.println("createElectionAlgorithm QuorumCnxManager -> ");
            e.printStackTrace();
        }
        return le;
    }

    public Map<Long, QuorumServer> getVotingView() {
        return QuorumPeer.viewToVotingView(getView());
    }

    static Map<Long, QuorumServer> viewToVotingView(Map<Long, QuorumServer> view) {
        Map<Long, QuorumServer> ret = new HashMap<>();
        for (QuorumServer server : view.values()) {
            if (server.type == LearnerType.PARTICIPANT) {
                ret.put(server.id, server);
            }
        }
        return ret;
    }

    public Map<Long, QuorumServer> getView() {
        return Collections.unmodifiableMap(this.quorumPeers);
    }

    public long getCurrentEpoch() throws IOException {
        if (currentEpoch == -1) {
            currentEpoch = readLongFromFile(CURRENT_EPOCH_FILENAME);
        }
        return currentEpoch;
    }

    private long readLongFromFile(String name) throws IOException {
        File file = new File(logFactory.getSnapDir(), name);
        System.out.println(file.getAbsolutePath());
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = "";
        try {
            line = br.readLine();
            return Long.parseLong(line);
        } catch (NumberFormatException e) {
            throw new IOException("Found " + line + " in " + file);
        } finally {
            br.close();
        }
    }

    public long getAndIncreaseCurrentEpoch() throws IOException {
        return writeLongFromFile(CURRENT_EPOCH_FILENAME);
    }

    private long writeLongFromFile(String name) throws IOException {
        File file = new File(logFactory.getSnapDir(), name);
        System.out.println(file.getAbsolutePath());
        BufferedWriter br = new BufferedWriter(new FileWriter(file));
        try {
            br.write(String.valueOf(++currentEpoch));
            return currentEpoch;
        } catch (NumberFormatException e) {
            throw new IOException("Found " + currentEpoch + " in " + file);
        } finally {
            br.close();
        }
    }


    public Follower follower;
    public Leader leader;
    public Observer observer;

    @Override
    public void run() {
        setName("QuorumPeer" + "[myid=" + getId() + "]" + cnxnFactory.getLocalAddress());
        System.out.println("Starting quorum peer");
        try {
            while (running) {
                switch (getPeerState()) {
                    case LOOKING:
                        System.out.println("LOOKING");
                        try {
                            System.out.println("Starting lookForLeader...");
                            setCurrentVote(electionAlg.lookForLeader());
                        } catch (Exception e) {
                            System.err.println("Unexpected exception" + e);
                            setPeerState(ServerState.LOOKING);
                            e.printStackTrace();
                        }
                        break;
                    case OBSERVING:
                        try {
                            System.out.println("OBSERVING");
                            setObserver(makeObserver(logFactory));
                            observer.observeLeader();
                        } catch (Exception e) {
                            System.err.println("Unexpected exception" + e);
                        } finally {
                            setObserver(null);
                            setPeerState(ServerState.LOOKING);
                        }
                        break;
                    case FOLLOWING:
                        try {
                            System.out.println("FOLLOWING");
                            setFollower(makeFollower(logFactory));
                            follower.followLeader();
                        } catch (Exception e) {
                            System.err.println("Unexpected exception" + e);
                        } finally {
                            setFollower(null);
                            setPeerState(ServerState.LOOKING);
                        }
                        break;
                    case LEADING:
                        try {
                            System.out.println("LEADING");
                            setLeader(makeLeader(logFactory));
                            leader.lead();
                        } catch (Exception e) {
                            System.out.println("Unexpected exception" + e);
                        } finally {
                            setLeader(null);
                            setPeerState(ServerState.LOOKING);
                        }
                        break;
                }
            }
        } finally {
            System.out.println("QuorumPeer main thread exited");
        }
    }

    protected Follower makeFollower(FileTxnSnapLog logFactory) throws IOException {
        return new Follower(this, new FollowerXiCPServer(logFactory, this, this.xiDb));
    }

    protected Leader makeLeader(FileTxnSnapLog logFactory) throws IOException {
        return new Leader(this, new LeaderXiCPServer(logFactory, this, this.xiDb));
    }

    protected Observer makeObserver(FileTxnSnapLog logFactory) throws IOException {
        return new Observer(this, new ObserverXiCPServer(logFactory, this, this.xiDb));
    }

    synchronized protected void setLeader(Leader newLeader){
        leader=newLeader;
    }

    synchronized protected void setFollower(Follower newFollower){ follower=newFollower; }

    synchronized protected void setObserver(Observer newObserver){
        observer=newObserver;
    }

    public void setQuorumPeers(Map<Long, QuorumServer> quorumPeers) {
        this.quorumPeers = quorumPeers;
    }

    public long getElectionTimeTaken() {
        return electionTimeTaken;
    }

    public void setElectionTimeTaken(long electionTimeTaken) {
        this.electionTimeTaken = electionTimeTaken;
    }

    public int getMinSessionTimeout() {
        return minSessionTimeout;
    }

    public void setMinSessionTimeout(int minSessionTimeout) {
        this.minSessionTimeout = minSessionTimeout;
    }

    public int getMaxSessionTimeout() {
        return maxSessionTimeout;
    }

    public void setMaxSessionTimeout(int maxSessionTimeout) {
        this.maxSessionTimeout = maxSessionTimeout;
    }

    public int getInitLimit() {
        return initLimit;
    }

    public void setInitLimit(int initLimit) {
        this.initLimit = initLimit;
    }

    public int getSyncLimit() {
        return syncLimit;
    }

    public void setSyncLimit(int syncLimit) {
        this.syncLimit = syncLimit;
    }

    public boolean isSyncEnabled() {
        return syncEnabled;
    }

    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }

    public void setElectionType(int electionType) {
        this.electionType = electionType;
    }

    public AtomicInteger getTick() {
        return tick;
    }

    public void setTick(AtomicInteger tick) {
        this.tick = tick;
    }

    public int getTickTime() {
        return tickTime;
    }

    public void setTickTime(int tickTime) {
        this.tickTime = tickTime;
    }

    public long getMyid() {
        return myid;
    }

    public void setMyid(long myid) {
        this.myid = myid;
    }

    @Override
    public String[] getQuorumPeers() {
        List<String> l = new ArrayList<String>();
        synchronized (this) {
            for (Long sid : quorumPeers.keySet()) {
                l.add(quorumPeers.get(sid).toString());
            }
        }
        return l.toArray(new String[0]);
    }

    public Map<Long, QuorumServer> getQuorumPeersMap(){
        return this.quorumPeers;
    }

    public QuorumServer getMyServer() {
        return myServer;
    }

    public void setMyServer(QuorumServer myServer) {
        this.myServer = myServer;
    }

    public XiCPDatabase getXiDb() {
        return xiDb;
    }

    public void setXiDb(XiCPDatabase xiDb) {
        this.xiDb = xiDb;
    }

    public void setTxnFactory(FileTxnSnapLog factory) {
        this.logFactory = factory;
    }

    public FileTxnSnapLog getTxnFactory() {
        return this.logFactory;
    }

    public ServerCnxnFactory getCnxnFactory() {
        return cnxnFactory;
    }

    public void setCnxnFactory(ServerCnxnFactory cnxnFactory) {
        this.cnxnFactory = cnxnFactory;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isRunning() {
        return running;
    }

    public synchronized void setCurrentVote(Vote v){
        currentVote = v;
    }

    public int getElectionType() {
        return electionType;
    }

    public LearnerType getLearnerType() {
        return learnerType;
    }

    public QuorumServer getMaster() {
        return master;
    }

    public void setMaster(QuorumServer master) {
        this.master = master;
    }
}
