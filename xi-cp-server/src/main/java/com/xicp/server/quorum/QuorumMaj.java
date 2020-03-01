package com.xicp.server.quorum;

import java.util.Set;

/**
 * @description: QuorumMaj
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class QuorumMaj implements QuorumVerifier{

    int half;

    public QuorumMaj(int n){
        this.half = n/2;
    }

    @Override
    public long getWeight(long id){
        return (long) 1;
    }

    @Override
    public boolean containsQuorum(Set<Long> set){
        return (set.size() > half);
    }

    public static boolean validQuorum(int valid, int machine){
        return valid > (int)(machine / 2);
    }
}
