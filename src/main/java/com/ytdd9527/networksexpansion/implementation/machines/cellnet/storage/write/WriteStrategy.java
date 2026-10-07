package com.ytdd9527.networksexpansion.implementation.machines.cellnet.storage.write;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
public class WriteStrategy {

    private static final long TIMEOUT_SECONDS_DEFAULT = 10L;

    private final long timeoutSeconds;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NetworksExpansion-Cellnet-Write");
        t.setDaemon(true);
        return t;
    });

    public WriteStrategy() {
        long configured = io.github.sefiraat.networks.Networks.getConfigManager().getCellnetStorageWriteTimeout();
        this.timeoutSeconds = configured > 0 ? configured : TIMEOUT_SECONDS_DEFAULT;
    }

    public <T> T runExclusive(Supplier<T> task) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(task, executor);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("网拓元件网络写操作被中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("网拓元件网络写操作失败", cause);
        } catch (TimeoutException e) {
            try {
                return future.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("网拓元件网络写操作被中断", ie);
            } catch (TimeoutException te) {
                throw new IllegalStateException("网拓元件网络写操作超时，任务仍在后台运行", te);
            } catch (ExecutionException ee) {
                Throwable cause = ee.getCause();
                if (cause instanceof RuntimeException re) {
                    throw re;
                }
                throw new IllegalStateException("网拓元件网络写操作失败", cause);
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
