package com.xicp.server.quorum;

public interface Election {

    Vote lookForLeader() throws InterruptedException;

    void shutdown();
}
