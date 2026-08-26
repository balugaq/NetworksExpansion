package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.write;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class WriteStrategy {

    private final ReentrantLock writeLock = new ReentrantLock();

    public boolean acquire(long timeout, TimeUnit unit) throws InterruptedException {
        return writeLock.tryLock(timeout, unit);
    }

    public void release() {
        writeLock.unlock();
    }
}