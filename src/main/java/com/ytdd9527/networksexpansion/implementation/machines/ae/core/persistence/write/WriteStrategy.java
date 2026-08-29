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

    private static final long TIMEOUT_SECONDS_DEFAULT = 10L;

    private final long timeoutSeconds;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NetworksExpansion-AE-Write");
        t.setDaemon(true);
        return t;
    });

    public WriteStrategy() {
        long configured = io.github.sefiraat.networks.Networks.getConfigManager().getAeStorageWriteTimeout();
        this.timeoutSeconds = configured > 0 ? configured : TIMEOUT_SECONDS_DEFAULT;
    }

    public <T> T runExclusive(Supplier<T> task) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(task, executor);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("网拓 AE 写操作被中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("网拓 AE 写操作失败", cause);
        } catch (TimeoutException e) {
            // 首次超时后任务仍在后台运行；再等一段有限时间让它在落库前完成，
            // 避免调用方在写已提交后仍回滚产生重复行；若仍超时则放弃并让上层停止，
            // 防止 checkpoint 无限阻塞或插件关闭卡死。
            try {
                return future.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("网拓 AE 写操作被中断", ie);
            } catch (TimeoutException te) {
                // 任务仍未完成：标记但不再无限等待，交由上层(如 checkpoint)据此停止
                throw new IllegalStateException("网拓 AE 写操作超时，任务仍在后台运行", te);
            } catch (ExecutionException ee) {
                Throwable cause = ee.getCause();
                if (cause instanceof RuntimeException re) {
                    throw re;
                }
                throw new IllegalStateException("网拓 AE 写操作失败", cause);
            }
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
