package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence;

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

    public StorageConfig() {
        ConfigManager cm = Networks.getConfigManager();
        File dataFolder = Networks.getInstance().getDataFolder();
        this.sqliteFile = new File(new File(dataFolder, "data"), "ae_storage.db");
        // 数据库文件路径固定，父目录在初始化时就确保存在
        File parent = sqliteFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
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
}
