package com.ytdd9527.networksexpansion.implementation.machines.cellnet.storage.schema;
public class DDLProvider {

    public String createSchemaInfoTable() {
        return "CREATE TABLE IF NOT EXISTS schema_info ("
            + "config_key VARCHAR(64) PRIMARY KEY, "
            + "config_value TEXT NOT NULL)";
    }

    public String createItemTemplatesTable() {
        return "CREATE TABLE IF NOT EXISTS item_templates ("
            + "tpl_id INTEGER PRIMARY KEY, "
            + "item_data TEXT NOT NULL, "
            + "item_data_hash BIGINT NOT NULL, "
            + "crc32 INT NOT NULL, "
            + "created_at BIGINT NOT NULL)";
    }

    public String createItemTemplatesDedup() {
        return "CREATE UNIQUE INDEX IF NOT EXISTS idx_tpl_dedup ON item_templates(item_data_hash)";
    }

    public String createCellMetaTable() {
        return "CREATE TABLE IF NOT EXISTS cell_meta ("
            + "cell_uuid CHAR(36) PRIMARY KEY, "
            + "whitelist_enabled INTEGER NOT NULL DEFAULT 0, "
            + "whitelist BLOB, "
            + "stored BIGINT NOT NULL DEFAULT 0, "
            + "snapshot_hash VARCHAR(64), "
            + "updated_at BIGINT NOT NULL, "
            + "created_at BIGINT NOT NULL)";
    }

    public String createCellItemsTable() {
        return "CREATE TABLE IF NOT EXISTS cell_items ("
            + "cell_uuid CHAR(36) NOT NULL, "
            + "tpl_id BIGINT NOT NULL, "
            + "amount BIGINT NOT NULL DEFAULT 0, "
            + "crc32 INT NOT NULL, "
            + "updated_at BIGINT NOT NULL, "
            + "PRIMARY KEY (cell_uuid, tpl_id))";
    }

    public String createJournalTable() {
        return "CREATE TABLE IF NOT EXISTS journal ("
            + "journal_id INTEGER PRIMARY KEY, "
            + "cell_uuid CHAR(36) NOT NULL, "
            + "op CHAR(1) NOT NULL, "
            + "tpl_id BIGINT, "
            + "new_amount BIGINT, "
            + "crc32 INT NOT NULL, "
            + "timestamp BIGINT NOT NULL, "
            + "applied TINYINT NOT NULL DEFAULT 0)";
    }

    public String createJournalIndex() {
        return "CREATE INDEX IF NOT EXISTS idx_journal_applied ON journal(applied, timestamp)";
    }

    public String createJournalArchiveTable() {
        return "CREATE TABLE IF NOT EXISTS journal_archive ("
            + "journal_id BIGINT NOT NULL, "
            + "cell_uuid CHAR(36) NOT NULL, "
            + "op CHAR(1) NOT NULL, "
            + "tpl_id BIGINT, "
            + "new_amount BIGINT, "
            + "crc32 INT NOT NULL, "
            + "timestamp BIGINT NOT NULL)";
    }

    public String createArchiveIndexes() {
        return "CREATE INDEX IF NOT EXISTS idx_archive_cell_tpl ON journal_archive(cell_uuid, tpl_id, timestamp DESC)";
    }

    public String createArchiveTimestampIndex() {
        return "CREATE INDEX IF NOT EXISTS idx_archive_timestamp ON journal_archive(timestamp)";
    }

    public String insertIgnore() {
        return "INSERT OR IGNORE";
    }

    public String upsertCellItem() {
        return "INSERT OR REPLACE INTO cell_items (cell_uuid, tpl_id, amount, crc32, updated_at) "
            + "VALUES (?, ?, ?, ?, ?)";
    }
}
