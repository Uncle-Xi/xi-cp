package com.demo.xi.cp.client;

import com.xicp.client.lock.CompeteLock;

import java.io.IOException;

/**
 * @description: UseLock
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class UseCompeteLock {

    private static final String CONNECT_ADDR = "127.0.0.1:2181";
    private static final String LOCK = "lock";
    public static void main(String[] args) throws IOException {
        CompeteLock lock = new CompeteLock(CONNECT_ADDR);
        if (lock.lock(LOCK, 5 * 1000)) {
            System.out.println("获取锁成功");
        }
        if (lock.unLock(LOCK)) {
            System.out.println("释放锁成功.");
        }
    }
}
