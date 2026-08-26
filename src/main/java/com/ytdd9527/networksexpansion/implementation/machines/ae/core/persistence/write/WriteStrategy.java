package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.write;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

// 所有写都排到单线程 executor，用 future.get(timeout) 等结果，不手写锁
public class WriteStrategy {

    private static final long TIMEOUT_SECONDS = 10L;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NetworksExpansion-AE-Write");
        t.setDaemon(true);
        return t;
    });

    public <T> T runExclusive(Supplier<T> task) {
        try {
            return CompletableFuture.supplyAsync(task, executor).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AE 写操作被中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("AE 写操作失败", cause);
        } catch (TimeoutException e) {
            throw new IllegalStateException("AE 写操作超时", e);
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
