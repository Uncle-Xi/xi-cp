package com.xicp.client.lock;

public abstract class Lock {

    private static final String DEFUALT_LOCK = "/locks";

    protected String getValidLock(String lock){
        if (lock == null) {
            throw new RuntimeException("[Lock] [getValidLock][lock==nulll]");
        }
        if (lock.indexOf("/") == -1) {
            lock = "/" + lock;
        }
        if (lock.lastIndexOf("/") == lock.length() - 1) {
            lock = lock.substring(0, lock.length() - 1);
        }
        return lock;
    }

    protected String getValidLock(String lock, String preffix){
        if (lock == null) {
            lock = DEFUALT_LOCK;
        }
        if (lock.indexOf("/") == -1) {
            lock = "/" + lock;
        }
        if (lock.lastIndexOf("/") == lock.length() - 1) {
            lock = lock.substring(0, lock.length() - 1);
        }
        lock = preffix + "-" +lock.substring(1, lock.length());
        return lock;
    }
}
