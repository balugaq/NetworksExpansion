package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.schema;

public class DDLProvider {

    public String createSchemaInfoTable() {
        return "CREATE TABLE IF NOT EXISTS ae_schema_info ("
            + "config_key VARCHAR(64) PRIMARY KEY, "
            + "config_value TEXT NOT NULL)";
    }

    public String createItemTemplatesTable() {
        return "CREATE TABLE IF NOT EXISTS ae_item_templates ("
            + "tpl_id INTEGER PRIMARY KEY, "
            + "item_id VARCHAR(256) NOT NULL, "
            + "item_data TEXT, "
            + "item_data_hash BIGINT NOT NULL, "
            + "crc32 INT NOT NULL, "
            + "created_at BIGINT NOT NULL)";
    }

    public String createItemTemplatesDedup() {
        return "CREATE UNIQUE INDEX IF NOT EXISTS idx_tpl_dedup ON ae_item_templates(item_id, item_data_hash)";
    }

    public String createItemTemplatesItemIdIndex() {
        return "CREATE INDEX IF NOT EXISTS idx_tpl_item_id ON ae_item_templates(item_id)";
    }

    public String createCellMetaTable() {
        return "CREATE TABLE IF NOT EXISTS ae_cell_meta ("
            + "cell_uuid CHAR(36) PRIMARY KEY, "
            + "whitelist_enabled INTEGER NOT NULL DEFAULT 0, "
            + "whitelist BLOB, "
            + "stored BIGINT NOT NULL DEFAULT 0, "
            + "snapshot_hash VARCHAR(64), "
            + "updated_at BIGINT NOT NULL, "
            + "created_at BIGINT NOT NULL)";
    }

    public String createCellItemsTable() {
        return "CREATE TABLE IF NOT EXISTS ae_cell_items ("
            + "cell_uuid CHAR(36) NOT NULL, "
            + "tpl_id BIGINT NOT NULL, "
            + "amount BIGINT NOT NULL DEFAULT 0, "
            + "crc32 INT NOT NULL, "
            + "updated_at BIGINT NOT NULL, "
            + "PRIMARY KEY (cell_uuid, tpl_id))";
    }

    public String createCellItemsIndex() {
        return "CREATE INDEX IF NOT EXISTS idx_cell_items_uuid ON ae_cell_items(cell_uuid)";
    }

    public String createJournalTable() {
        return "CREATE TABLE IF NOT EXISTS ae_journal ("
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
        return "CREATE INDEX IF NOT EXISTS idx_journal_applied ON ae_journal(applied, timestamp)";
    }

    public String createJournalArchiveTable() {
        return "CREATE TABLE IF NOT EXISTS ae_journal_archive ("
            + "journal_id BIGINT NOT NULL, "
            + "cell_uuid CHAR(36) NOT NULL, "
            + "op CHAR(1) NOT NULL, "
            + "tpl_id BIGINT, "
            + "new_amount BIGINT, "
            + "crc32 INT NOT NULL, "
            + "timestamp BIGINT NOT NULL)";
    }

    public String createArchiveIndexes() {
        return "CREATE INDEX IF NOT EXISTS idx_archive_cell_tpl ON ae_journal_archive(cell_uuid, tpl_id, timestamp DESC)";
    }

    public String createArchiveTimestampIndex() {
        return "CREATE INDEX IF NOT EXISTS idx_archive_timestamp ON ae_journal_archive(timestamp)";
    }

    public String insertIgnore() {
        return "INSERT OR IGNORE";
    }

    public String upsertCellItem() {
        return "INSERT OR REPLACE INTO ae_cell_items (cell_uuid, tpl_id, amount, crc32, updated_at) "
            + "VALUES (?, ?, ?, ?, ?)";
    }
}