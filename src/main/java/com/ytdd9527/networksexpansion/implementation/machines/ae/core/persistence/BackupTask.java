package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence;

import com.balugaq.netex.utils.Debug;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.connection.ConnectionManager;
import io.github.sefiraat.networks.Networks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class BackupTask implements Runnable {

    private static final Logger LOGGER = Networks.getInstance().getLogger();
    private final StorageConfig config;
    private final ConnectionManager connMgr;

    public BackupTask(StorageConfig config, ConnectionManager connMgr) {
        this.config = config;
        this.connMgr = connMgr;
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
        // VACUUM INTO 要求目标文件不存在
        try {
            Files.deleteIfExists(backupFile.toPath());
        } catch (IOException e) {
            LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.backup_copy_failed", e.getMessage()));
            Debug.trace(e);
            return;
        }
        // 使用 SQLite 在线备份，避免直接复制正在写入的 WAL 数据库产生损坏副本。
        // 注意：不自动清理历史备份，交由服主自行管理。
        try (Connection conn = connMgr.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("VACUUM INTO '" + backupFile.getAbsolutePath().replace("'", "''") + "'");
        } catch (SQLException e) {
            LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.backup_copy_failed", e.getMessage()));
            Debug.trace(e);
        }
    }
}
