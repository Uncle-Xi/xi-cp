package com.demo.xi.cp.client;

import com.xicp.client.lock.SequenceLock;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @description: UseSequenceLock
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class UseSequenceLock {

    private static final String connect = "127.0.0.1:2181";
    private static final String LOCK = "lock";

    public static void main(String[] args) throws IOException, InterruptedException {
        CountDownLatch count = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    count.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                SequenceLock lock = null;
                try {
                    lock = new SequenceLock(connect);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                SequenceLock finalLock = lock;
                finalLock.acquireLock(LOCK, new SequenceLock.PostProcess() {
                    @Override
                    public void handler() {
                        System.out.println("【获得锁成功】");
                        System.out.println("【完成业务逻辑处理】");
                        finalLock.releaseLock(LOCK);
                        finalLock.close();
                        System.out.println("【释放锁成功】\n\n");
                    }
                });
            }).start();
            count.countDown();
        }
    }
}
