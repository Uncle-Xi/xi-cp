package com.xicp.server.quorum;

import com.xicp.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @description: FastLeaderElection
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class FastLeaderElection implements Election {

    final static int finalizeWait = 200;
    final static int maxNotificationInterval = 60000;
    QuorumCnxManager manager;
    volatile boolean stop;
    QuorumPeer self;

    long proposedLeader;
    long proposedZxid;
    long proposedEpoch;

    public FastLeaderElection(QuorumPeer self, QuorumCnxManager manager) {
        this.stop = false;
        this.manager = manager;
        this.self = self;
        proposedLeader = self.getMyid();
        proposedZxid = getInitLastLoggedZxid();
        proposedEpoch = getPeerEpoch();
        System.out.printf("票据信息：proposedLeader = [%s], proposedZxid = [%s], proposedEpoch = [%s].\n",
                proposedLeader, proposedZxid, proposedEpoch);
    }

    AtomicLong logicalclock = new AtomicLong();

    private long getInitId() {
        if (self.getLearnerType() == QuorumPeer.LearnerType.PARTICIPANT)
            return self.getId();
        else return Long.MIN_VALUE;
    }

    /**
     * Returns initial last logged zxid.
     *
     * @return long
     */
    private long getInitLastLoggedZxid() {
        if (self.getLearnerType() == QuorumPeer.LearnerType.PARTICIPANT)
            return self.getLastLoggedZxid();
        else return Long.MIN_VALUE;
    }

    /**
     * Returns the initial vote value of the peer epoch.
     *
     * @return long
     */
    private long getPeerEpoch() {
        if (self.getLearnerType() == QuorumPeer.LearnerType.PARTICIPANT)
            try {
                return self.getCurrentEpoch();
            } catch (IOException e) {
                RuntimeException re = new RuntimeException(e.getMessage());
                re.setStackTrace(e.getStackTrace());
                throw re;
            }
        else return Long.MIN_VALUE;
    }

    private long peerEpochIncrAndGet(){
        try {
            return self.getAndIncreaseCurrentEpoch();
        } catch (IOException e) {
            RuntimeException re = new RuntimeException(e.getMessage());
            re.setStackTrace(e.getStackTrace());
            throw re;
        }
    }

    @Override
    public Vote lookForLeader() throws InterruptedException {
        System.out.println("FastLeaderElection lookForLeader...");
        HashMap<Long, Vote> recvset = new HashMap<>();
        manager.starter();

        while ((self.getPeerState() == ServerState.LOOKING) && (!stop)) {
            recvset.clear();
            int vm = manager.connectAll();
            int machine = self.getQuorumPeersMap().size();
            if (!QuorumMaj.validQuorum(vm, machine)) {
                System.out.printf("机器节点不符合预期：validMachine = [%s], machine = [%s] \n", vm, machine);
                Thread.sleep(1000);
                continue;
            }
            proposedEpoch = peerEpochIncrAndGet();
            Vote vote = new Vote(proposedLeader, proposedZxid, proposedEpoch);
            System.out.println("开始选举，发出票据：" + StringUtils.getString(vote));
            manager.sendVote(vote);
            while (vm-- > 0) {
                Vote v = manager.takeVote();
                recvset.put((long) vm, v);
                totalOrderPredicate(v, vote);
            }
            recvset.put(self.getMyid(), vote);
            if (termPredicate(recvset, vote, machine)) {
                System.out.println("选出了主节点：");
                self.setPeerState((proposedLeader == self.getMyid()) ? ServerState.LEADING : learningState());
                manager.sendVote(vote);
                return null;
            }
            System.out.println("开始下一轮选举：");
        }
        return null;
    }

    private ServerState learningState() {
        if (self.getLearnerType() == QuorumPeer.LearnerType.PARTICIPANT) {
            System.out.println("I'm a participant: " + self.getMyid());
            return ServerState.FOLLOWING;
        } else {
            System.out.println("I'm an observer: " + self.getMyid());
            return ServerState.OBSERVING;
        }
    }

    private boolean validVoter(long sid) {
        return self.getVotingView().containsKey(sid);
    }

    @Override
    public void shutdown() {
        stop = true;
    }

    void totalOrderPredicate(Vote other, Vote self) {
        if (totalOrderPredicate(other.getId(), other.getZxid(), other.getElectionEpoch(),
                self.getId(), self.getZxid(), self.getElectionEpoch())) {
            System.out.println("更新成对方的票据...");
            updateProposal(other.getId(), other.getZxid(), other.getElectionEpoch());
        }
    }

    synchronized void updateProposal(long leader, long zxid, long epoch) {
        System.out.println("Updating proposal: " + leader + " (newleader), 0x"
                + Long.toHexString(zxid) + " (newzxid), " + epoch + " (newepoch), ");
        proposedLeader = leader;
        proposedZxid = zxid;
        proposedEpoch = epoch;
    }

    protected boolean totalOrderPredicate(long newId, long newZxid, long newEpoch, long curId, long curZxid, long curEpoch) {
        System.out.println("newId: " + newId + ", proposed id: " + curId + ", newZxid: 0x" +
                Long.toHexString(newZxid) + ", proposed zxid: 0x" + Long.toHexString(curZxid));
        return ((newEpoch > curEpoch) || ((newEpoch == curEpoch) &&
                ((newZxid > curZxid) || ((newZxid == curZxid) && (newId > curId)))));
    }

    protected boolean termPredicate(HashMap<Long, Vote> votes, Vote vote, int machine) {
        HashSet<Long> set = new HashSet<>();
        for (Map.Entry<Long, Vote> entry : votes.entrySet()) {
            if (vote.getId() == entry.getValue().getId()) {
                set.add(entry.getKey());
            }
        }
        boolean result = QuorumMaj.validQuorum(set.size(), machine);
        System.out.printf("termPredicate set.size() = [%s], machine = [%s], result = [%s]\n", set.size(), machine, result);
        if (result) {
            setMaster(vote);
        }
        return result;
    }

    protected void setMaster(Vote vote){
        Map<Long, QuorumServer> quorumPeersMap = self.getQuorumPeersMap();
        for (Long sid : quorumPeersMap.keySet()) {
            if (sid == vote.getId()) {
                self.setMaster(quorumPeersMap.get(sid));
                return;
            }
        }
    }

    public static void main(String[] args) {
        long a = 10000;
        long b = 10000;
        System.out.println(a == b);
    }
}
