package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence;

import io.github.sefiraat.networks.Networks;

import java.io.File;

public class StorageConfig {

    private final File sqliteFile;
    private final boolean walMode = true;
    private final int busyTimeout = 5000;
    private final long journalRetentionMinutes = 30;
    private final boolean archiveEnabled = true;
    private final int archiveRetentionDays = 7;
    private final int archiveMaxRows = 2000000;
    private final int checkpointInterval = 60;
    private final int checkpointThreshold = 5000;
    private final boolean backupEnabled = true;
    private final int backupIntervalHours = 24;
    private final int maxBackups = 7;

    public StorageConfig() {
        File dataFolder = Networks.getInstance().getDataFolder();
        this.sqliteFile = new File(new File(dataFolder, "data"), "ae_storage.db");
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

    public int getMaxBackups() {
        return maxBackups;
    }
}