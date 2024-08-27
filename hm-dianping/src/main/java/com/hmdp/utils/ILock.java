package com.hmdp.utils;

public interface ILock {
    /**
     * 尝试获取锁
     * @param timeoutSec 锁的TTL
     * @return  true代表获取锁成功
     */
    boolean tryLock(long timeoutSec);

    void unlock();
}
