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
        CompletableFuture<T> future = CompletableFuture.supplyAsync(task, executor);
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
            // 超时后任务仍在后台运行且可能已提交；继续等待它到终态，
            // 避免调用方在该批写已落库后仍回滚，从而产生重复行。
            try {
                return future.get();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("AE 写操作被中断", ie);
            } catch (ExecutionException ee) {
                Throwable cause = ee.getCause();
                if (cause instanceof RuntimeException re) {
                    throw re;
                }
                throw new IllegalStateException("AE 写操作失败", cause);
            }
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
