package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.connection;

import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.StorageConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionManager {

    private final String jdbcUrl;

    public ConnectionManager(StorageConfig config) {
        File file = config.getSqliteFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        this.jdbcUrl = "jdbc:sqlite:" + file.getAbsolutePath();
        if (config.isWalMode()) {
            try (Connection conn = DriverManager.getConnection(jdbcUrl);
                 Statement stat = conn.createStatement()) {
                stat.execute("PRAGMA journal_mode=WAL");
                stat.execute("PRAGMA synchronous=NORMAL");
                stat.execute("PRAGMA busy_timeout=" + config.getBusyTimeout());
                stat.execute("PRAGMA foreign_keys=ON");
            } catch (SQLException ignored) {
            }
        }
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement stat = conn.createStatement()) {
            stat.execute("PRAGMA busy_timeout=5000");
        } catch (SQLException ignored) {
        }
        return conn;
    }

    public void shutdown() {
    }
}