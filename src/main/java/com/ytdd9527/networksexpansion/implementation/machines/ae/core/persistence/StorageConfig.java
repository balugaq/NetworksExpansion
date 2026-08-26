package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence;

import com.ytdd9527.networksexpansion.core.managers.ConfigManager;
import io.github.sefiraat.networks.Networks;

import java.io.File;

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

    public StorageConfig() {
        ConfigManager cm = Networks.getConfigManager();
        File dataFolder = Networks.getInstance().getDataFolder();
        this.sqliteFile = new File(new File(dataFolder, "data"), "ae_storage.db");
        this.walMode = cm.getAeStorageWalMode();
        this.busyTimeout = cm.getAeStorageBusyTimeout();
        this.journalRetentionMinutes = cm.getAeStorageJournalRetentionMinutes();
        this.archiveEnabled = cm.getAeStorageArchiveEnabled();
        this.archiveRetentionDays = cm.getAeStorageArchiveRetentionDays();
        this.archiveMaxRows = cm.getAeStorageArchiveMaxRows();
        this.checkpointInterval = cm.getAeStorageCheckpointInterval();
        this.checkpointThreshold = cm.getAeStorageCheckpointThreshold();
        this.backupEnabled = cm.getAeStorageBackupEnabled();
        this.backupIntervalHours = cm.getAeStorageBackupIntervalHours();
    }

    public File getSqliteFile() {
        return sqliteFile;
    }

    public boolean isWalMode() {
        return walMode;
    }

    public int getBusyTimeout() {
        return busyTimeout;
    }

    public long getJournalRetentionMinutes() {
        return journalRetentionMinutes;
    }

    public boolean isArchiveEnabled() {
        return archiveEnabled;
    }

    public int getArchiveRetentionDays() {
        return archiveRetentionDays;
    }

    public int getArchiveMaxRows() {
        return archiveMaxRows;
    }

    public int getCheckpointInterval() {
        return checkpointInterval;
    }

    public int getCheckpointThreshold() {
        return checkpointThreshold;
    }

    public boolean isBackupEnabled() {
        return backupEnabled;
    }

    public int getBackupIntervalHours() {
        return backupIntervalHours;
    }
}
