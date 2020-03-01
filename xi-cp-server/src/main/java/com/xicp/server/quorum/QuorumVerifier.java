package com.xicp.server.quorum;

import java.util.Set;

public interface QuorumVerifier {
    long getWeight(long id);
    boolean containsQuorum(Set<Long> set);
}

