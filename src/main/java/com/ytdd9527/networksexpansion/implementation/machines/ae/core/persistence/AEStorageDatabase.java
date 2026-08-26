/*
 * AE 存储持久化层（WAL + Journal + Checkpoint + 模板去重 + 备份），架构参考 SlimeAE（MIT）。
 */
package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence;

import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.connection.ConnectionManager;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.dao.AEStorageCellController;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.journal.CheckpointTask;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.journal.DirtyTracker;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.journal.JournalWriter;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.schema.DDLProvider;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.schema.SchemaManager;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.template.ItemKeyTplIdBridge;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.template.ItemTemplateRegistry;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.write.WriteStrategy;
import io.github.sefiraat.networks.Networks;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class AEStorageDatabase {

    private static final Logger LOGGER = Networks.getInstance().getLogger();
    private static final AtomicLong FLUSH_COUNTER = new AtomicLong(0);

    private final StorageConfig storageConfig;
    private final ConnectionManager connMgr;
    private final SchemaManager schemaManager;
    private final ItemTemplateRegistry templateRegistry;
    private final ItemKeyTplIdBridge bridge;
    private final DirtyTracker dirtyTracker;
    private final WriteStrategy writeStrategy;
    private final JournalWriter journalWriter;
    private final CheckpointTask checkpointTask;
    private final AEStorageCellController storageController;
    private final BackupTask backupTask;
    private ScheduledExecutorService checkpointExecutor;
    private ScheduledExecutorService backupExecutor;

    public AEStorageDatabase() {
        this.storageConfig = new StorageConfig();
        this.connMgr = new ConnectionManager(storageConfig);
        this.schemaManager = new SchemaManager(connMgr);
        DDLProvider ddl = schemaManager.getDDL();
        this.templateRegistry = new ItemTemplateRegistry(connMgr, ddl);
        this.bridge = new ItemKeyTplIdBridge(templateRegistry);
        this.dirtyTracker = new DirtyTracker();
        this.writeStrategy = new WriteStrategy();
        this.journalWriter = new JournalWriter(writeStrategy, dirtyTracker, connMgr);
        this.checkpointTask = new CheckpointTask(connMgr, writeStrategy, ddl, storageConfig);
        this.storageController = new AEStorageCellController(connMgr, bridge, dirtyTracker);
        this.backupTask = new BackupTask(storageConfig, connMgr);
    }

    public void init() {
        schemaManager.initSchema();
        schemaManager.markSchemaVersion();
        templateRegistry.preloadAll();
        checkpointTask.replayPendingJournal();

        checkpointExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NetworksExpansion-AE-Checkpoint");
            t.setDaemon(true);
            return t;
        });
        long checkpointInterval = storageConfig.getCheckpointInterval();
        if (checkpointInterval > 0) {
            checkpointExecutor.scheduleWithFixedDelay(
                checkpointTask,
                checkpointInterval,
                checkpointInterval,
                TimeUnit.SECONDS);
        }

        if (storageConfig.isBackupEnabled()) {
            backupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "NetworksExpansion-AE-Backup");
                t.setDaemon(true);
                return t;
            });
            long backupIntervalSeconds = storageConfig.getBackupIntervalHours() * 3600L;
            if (backupIntervalSeconds > 0) {
                backupExecutor.scheduleWithFixedDelay(
                    backupTask,
                    backupIntervalSeconds,
                    backupIntervalSeconds,
                    TimeUnit.SECONDS);
            }
        }

        LOGGER.info(Networks.getLocalizationService().getString("messages.ae.persistence.db_initialized"));
    }

    public void saveAllAsync() {
        journalWriter.flush();
        long count = FLUSH_COUNTER.incrementAndGet();
        if (storageConfig.getCheckpointThreshold() > 0 && count % storageConfig.getCheckpointThreshold() == 0 && checkpointExecutor != null) {
            checkpointExecutor.submit(checkpointTask);
        }
    }

    public void shutdown() {
        stopExecutor(backupExecutor, 5);
        stopExecutor(checkpointExecutor, 15);
        journalWriter.flush();
        checkpointTask.doCheckpoint();
        writeStrategy.shutdown();
        connMgr.shutdown();
        LOGGER.info(Networks.getLocalizationService().getString("messages.ae.persistence.db_closed"));
    }

    private static void stopExecutor(ScheduledExecutorService executor, long timeoutSeconds) {
        if (executor == null) {
            return;
        }
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

    public AEStorageCellController getStorageController() {
        return storageController;
    }
}