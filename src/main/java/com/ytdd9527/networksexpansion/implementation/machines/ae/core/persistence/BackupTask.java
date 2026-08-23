package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence;

import com.balugaq.netex.utils.Debug;
import io.github.sefiraat.networks.Networks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Logger;

public class BackupTask implements Runnable {

    private static final Logger LOGGER = Networks.getInstance().getLogger();
    private final StorageConfig config;

    public BackupTask(StorageConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        if (!config.isBackupEnabled()) {
            return;
        }
        try {
            doBackup();
        } catch (Exception e) {
            LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.backup_failed", e.getMessage()));
            Debug.trace(e);
        }
    }

    public void doBackup() {
        File dbFile = config.getSqliteFile();
        if (!dbFile.exists()) {
            LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.backup_file_missing", dbFile.getAbsolutePath()));
            return;
        }
        File backupDir = new File(dbFile.getParentFile(), "backups");
        backupDir.mkdirs();
        String timestamp = String.valueOf(System.currentTimeMillis());
        File backupFile = new File(backupDir, "ae_storage_" + timestamp + ".db");
        try {
            Files.copy(dbFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.backup_copy_failed", e.getMessage()));
            Debug.trace(e);
            return;
        }
        cleanupOldBackups(backupDir);
    }

    private void cleanupOldBackups(File backupDir) {
        File[] backups = backupDir.listFiles((dir, name) -> name.startsWith("ae_storage_") && name.endsWith(".db"));
        if (backups == null || backups.length <= config.getMaxBackups()) {
            return;
        }
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified));
        int toDelete = backups.length - config.getMaxBackups();
        for (int i = 0; i < toDelete; i++) {
            try {
                Files.deleteIfExists(backups[i].toPath());
            } catch (IOException e) {
                LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.backup_cleanup_failed", backups[i].getName(), e.getMessage()));
                Debug.trace(e);
            }
        }
    }
}