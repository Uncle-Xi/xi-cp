package com.xicp.server.quorum;

import java.util.logging.Logger;

/**
 * @description: Vote
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class Vote {

    public Vote(long id,
                long zxid) {
        this.version = 0x0;
        this.id = id;
        this.zxid = zxid;
        this.electionEpoch = -1;
        this.peerEpoch = -1;
        this.state = ServerState.LOOKING;
    }

    public Vote(long id,
                long zxid,
                long peerEpoch) {
        this.version = 0x0;
        this.id = id;
        this.zxid = zxid;
        this.electionEpoch = -1;
        this.peerEpoch = peerEpoch;
        this.state = ServerState.LOOKING;
    }

    public Vote(long id,
                long zxid,
                long electionEpoch,
                long peerEpoch) {
        this.version = 0x0;
        this.id = id;
        this.zxid = zxid;
        this.electionEpoch = electionEpoch;
        this.peerEpoch = peerEpoch;
        this.state = ServerState.LOOKING;
    }

    public Vote(int version,
                long id,
                long zxid,
                long electionEpoch,
                long peerEpoch,
                ServerState state) {
        this.version = version;
        this.id = id;
        this.zxid = zxid;
        this.electionEpoch = electionEpoch;
        this.state = state;
        this.peerEpoch = peerEpoch;
    }

    public Vote(long id,
                long zxid,
                long electionEpoch,
                long peerEpoch,
                ServerState state) {
        this.id = id;
        this.zxid = zxid;
        this.electionEpoch = electionEpoch;
        this.state = state;
        this.peerEpoch = peerEpoch;
        this.version = 0x0;
    }

    final private int version;

    final private long id;

    final private long zxid;

    final private long electionEpoch;

    final private long peerEpoch;

    public int getVersion() {
        return version;
    }

    public long getId() {
        return id;
    }

    public long getZxid() {
        return zxid;
    }

    public long getElectionEpoch() {
        return electionEpoch;
    }

    public long getPeerEpoch() {
        return peerEpoch;
    }

    public ServerState getState() {
        return state;
    }

    final private ServerState state;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Vote)) {
            return false;
        }
        Vote other = (Vote) o;

        if ((state == ServerState.LOOKING) ||
                (other.state == ServerState.LOOKING)) {
            return (id == other.id
                    && zxid == other.zxid
                    && electionEpoch == other.electionEpoch
                    && peerEpoch == other.peerEpoch);
        } else {
            if ((version > 0x0) ^ (other.version > 0x0)) {
                return id == other.id;
            } else {
                return (id == other.id
                        && peerEpoch == other.peerEpoch);
            }
        }
    }

    @Override
    public int hashCode() {
        return (int) (id & zxid);
    }

    @Override
    public String toString() {
        return String.format("(%d, %s, %s)",
                id,
                Long.toHexString(zxid),
                Long.toHexString(peerEpoch));
    }
}
