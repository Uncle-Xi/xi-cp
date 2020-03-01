package com.xicp.server.quorum;

/**
 * @description: QuorumStats
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class QuorumStats {

    private final Provider provider;

    public interface Provider {
        String UNKNOWN_STATE = "unknown";
        String LOOKING_STATE = "leaderelection";
        String LEADING_STATE = "leading";
        String FOLLOWING_STATE = "following";
        String OBSERVING_STATE = "observing";

        String[] getQuorumPeers();

        String getServerState();
    }

    protected QuorumStats(Provider provider) {
        this.provider = provider;
    }

    public String getServerState() {
        return provider.getServerState();
    }

    public String[] getQuorumPeers() {
        return provider.getQuorumPeers();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        String state = getServerState();
        if (state.equals(Provider.LEADING_STATE)) {
            sb.append("Followers:");
            for (String f : getQuorumPeers()) {
                sb.append(" ").append(f);
            }
            sb.append("\n");
        } else if (state.equals(Provider.FOLLOWING_STATE)
                || state.equals(Provider.OBSERVING_STATE)) {
            sb.append("Leader: ");
            String[] ldr = getQuorumPeers();
            if (ldr.length > 0) {
                sb.append(ldr[0]);
            } else {
                sb.append("not connected");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

}
