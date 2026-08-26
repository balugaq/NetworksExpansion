package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.schema;

import com.balugaq.netex.utils.Debug;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.connection.ConnectionManager;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.util.AESerializeUtils;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.util.CRC32Utils;
import io.github.sefiraat.networks.Networks;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SchemaManager {

    private static final Logger LOGGER = Networks.getInstance().getLogger();
    private final ConnectionManager connMgr;
    private final DDLProvider ddl = new DDLProvider();

    public SchemaManager(ConnectionManager connMgr) {
        this.connMgr = connMgr;
    }

    public DDLProvider getDDL() {
        return ddl;
    }

    public void initSchema() {
        try (Connection conn = connMgr.getConnection()) {
            exec(conn, ddl.createSchemaInfoTable());
            initItemTemplatesTable(conn);
            exec(conn, ddl.createCellMetaTable());
            exec(conn, ddl.createCellItemsTable());
            exec(conn, ddl.createJournalTable());
            exec(conn, ddl.createJournalIndex());
            exec(conn, ddl.createJournalArchiveTable());
            exec(conn, ddl.createArchiveIndexes());
            exec(conn, ddl.createArchiveTimestampIndex());

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT config_value FROM ae_schema_info WHERE config_key = 'created_at'")) {
                if (!rs.next()) {
                    long now = System.currentTimeMillis();
                    exec(conn, "INSERT INTO ae_schema_info (config_key, config_value) VALUES ('created_at', '" + now + "')");
                    exec(conn, "INSERT INTO ae_schema_info (config_key, config_value) VALUES ('last_migrated', '" + now + "')");
                }
            }
        } catch (SQLException e) {
            LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.schema_init_failed", e.getMessage()));
            Debug.trace(e);
            throw new RuntimeException("Schema initialization failed", e);
        }
    }

    // 建模板表；老库（按 item_id 存）先迁成 base64
    private void initItemTemplatesTable(Connection conn) throws SQLException {
        boolean legacy = hasColumn(conn, "ae_item_templates", "item_id");
        if (legacy) {
            List<Object[]> legacyRows = readLegacyTemplates(conn);
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                exec(conn, "DROP TABLE ae_item_templates");
                exec(conn, ddl.createItemTemplatesTable());
                migrateLegacyRows(conn, legacyRows);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } else {
            exec(conn, ddl.createItemTemplatesTable());
        }
        exec(conn, ddl.createItemTemplatesDedup());
    }

    private boolean hasColumn(Connection conn, String table, String column) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equals(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Object[]> readLegacyTemplates(Connection conn) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT tpl_id, item_id, item_data FROM ae_item_templates")) {
            while (rs.next()) {
                rows.add(new Object[]{rs.getLong("tpl_id"), rs.getString("item_id"), rs.getString("item_data")});
            }
        }
        return rows;
    }

    private void migrateLegacyRows(Connection conn, List<Object[]> rows) throws SQLException {
        String sql = "INSERT OR IGNORE INTO ae_item_templates (tpl_id, item_data, item_data_hash, crc32, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] row : rows) {
                String itemId = (String) row[1];
                String itemData = (String) row[2];
                ItemStack item = decodeLegacyTemplate(itemId, itemData);
                if (item == null) {
                    continue;
                }
                String data = AESerializeUtils.object2String(item);
                // 保留原 tpl_id，ae_cell_items 引用才不失效
                ps.setLong(1, (Long) row[0]);
                ps.setString(2, data);
                ps.setLong(3, dataHash(data));
                ps.setInt(4, CRC32Utils.compute(data));
                ps.setLong(5, System.currentTimeMillis());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // 老版本模板 id 解码，只给迁移用
    @Nullable
    private static ItemStack decodeLegacyTemplate(String itemId, String itemData) {
        if (itemId == null) {
            return null;
        }
        if (itemId.startsWith("SLIMEFUN_")) {
            SlimefunItem sf = SlimefunItem.getById(itemId.substring("SLIMEFUN_".length()));
            return sf == null ? null : sf.getItem().clone();
        }
        if (itemId.startsWith("VANILLA_")) {
            Material mat = Material.getMaterial(itemId.substring("VANILLA_".length()));
            return mat == null ? null : new ItemStack(mat);
        }
        if ("CUSTOM".equals(itemId) && itemData != null) {
            return AESerializeUtils.string2Object(itemData);
        }
        return null;
    }

    private static long dataHash(String itemData) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(itemData.getBytes(StandardCharsets.UTF_8));
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xff);
            }
            return hash;
        } catch (NoSuchAlgorithmException e) {
            return CRC32Utils.compute(itemData) & 0xffffffffL;
        }
    }

    public void markSchemaVersion() {
        try (Connection conn = connMgr.getConnection()) {
            exec(conn, "INSERT OR REPLACE INTO ae_schema_info (config_key, config_value) VALUES ('schema_version', '1')");
        } catch (SQLException e) {
            LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.schema_mark_version_failed", e.getMessage()));
            Debug.trace(e);
        }
    }

    private void exec(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
