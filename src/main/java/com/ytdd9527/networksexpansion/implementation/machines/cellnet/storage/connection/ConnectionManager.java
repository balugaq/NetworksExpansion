package com.ytdd9527.networksexpansion.implementation.machines.cellnet.storage.connection;

import com.balugaq.netex.utils.Debug;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.storage.StorageConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
public class ConnectionManager {

    private final String jdbcUrl;
    private final StorageConfig config;

    public ConnectionManager(StorageConfig config) {
        this.config = config;
        File file = config.getSqliteFile();
        this.jdbcUrl = "jdbc:sqlite:" + file.getAbsolutePath();
        if (config.isWalMode()) {
            try (Connection conn = DriverManager.getConnection(jdbcUrl);
                 Statement stat = conn.createStatement()) {
                stat.execute("PRAGMA journal_mode=WAL");
            } catch (SQLException e) {
                Debug.trace(e, "设置 WAL 模式失败");
            }
        }
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement stat = conn.createStatement()) {
            stat.execute("PRAGMA synchronous=NORMAL");
            stat.execute("PRAGMA busy_timeout=" + config.getBusyTimeout());
            stat.execute("PRAGMA foreign_keys=ON");
            if (config.isWalMode()) {
                stat.execute("PRAGMA journal_mode=WAL");
            }
        } catch (SQLException e) {
            Debug.trace(e, "设置连接级 PRAGMA 失败");
        }
        return conn;
    }

    public void shutdown() {
    }
}
