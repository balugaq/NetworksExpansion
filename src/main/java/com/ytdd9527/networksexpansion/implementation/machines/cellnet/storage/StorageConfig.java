package com.ytdd9527.networksexpansion.implementation.machines.cellnet.storage;

import com.ytdd9527.networksexpansion.core.managers.ConfigManager;
import io.github.sefiraat.networks.Networks;
import lombok.Getter;

import java.io.File;

@Getter
public class StorageConfig {

    private final File sqliteFile;
    private final boolean walMode;
    private final int busyTimeout;
    private final long journalRetentionMinutes;
    private final boolean archiveEnabled;
    private final int archiveRetentionDays;
    private final int archiveMaxRows;
    private final int checkpointInterval;
    private final int checkpointThreshold;
    private final boolean backupEnabled;
    private final int backupIntervalHours;
    private final int backupKeep;

    public StorageConfig() {
        ConfigManager cm = Networks.getConfigManager();
        File dataFolder = Networks.getInstance().getDataFolder();
        this.sqliteFile = new File(new File(dataFolder, "data"), "cellnet_storage.db");
        File parent = sqliteFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        this.walMode = cm.getCellnetStorageWalMode();
        this.busyTimeout = cm.getCellnetStorageBusyTimeout();
        this.journalRetentionMinutes = cm.getCellnetStorageJournalRetentionMinutes();
        this.archiveEnabled = cm.getCellnetStorageArchiveEnabled();
        this.archiveRetentionDays = cm.getCellnetStorageArchiveRetentionDays();
        this.archiveMaxRows = cm.getCellnetStorageArchiveMaxRows();
        this.checkpointInterval = cm.getCellnetStorageCheckpointInterval();
        this.checkpointThreshold = cm.getCellnetStorageCheckpointThreshold();
        this.backupEnabled = cm.getCellnetStorageBackupEnabled();
        this.backupIntervalHours = cm.getCellnetStorageBackupIntervalHours();
        this.backupKeep = cm.getCellnetStorageBackupKeep();
    }
}
