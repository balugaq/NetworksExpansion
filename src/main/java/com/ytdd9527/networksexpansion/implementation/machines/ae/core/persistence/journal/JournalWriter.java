package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.journal;

import com.balugaq.netex.utils.Debug;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.connection.ConnectionManager;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.write.WriteStrategy;
import io.github.sefiraat.networks.Networks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class JournalWriter {

    private static final Logger LOGGER = Networks.getInstance().getLogger();
    private static final int FLUSH_BATCH_SIZE = 2000;
    private final WriteStrategy writeStrategy;
    private final DirtyTracker dirtyTracker;
    private final ConnectionManager connMgr;

    public JournalWriter(WriteStrategy writeStrategy, DirtyTracker dirtyTracker, ConnectionManager connMgr) {
        this.writeStrategy = writeStrategy;
        this.dirtyTracker = dirtyTracker;
        this.connMgr = connMgr;
    }

    public void flush() {
        List<JournalRow> allRows = dirtyTracker.drainPhase1();
        if (allRows.isEmpty()) {
            return;
        }

        List<List<JournalRow>> batches = partition(allRows, FLUSH_BATCH_SIZE);
        boolean allSuccess = true;
        int flushedCount = 0;

        for (List<JournalRow> batch : batches) {
            try {
                if (!writeStrategy.acquire(5, TimeUnit.SECONDS)) {
                    allSuccess = false;
                    break;
                }
                try {
                    batchInsertJournal(batch);
                    flushedCount += batch.size();
                } finally {
                    writeStrategy.release();
                }
            } catch (Exception e) {
                allSuccess = false;
                LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.journal_flush_failed", e.getMessage()));
                Debug.trace(e);
                break;
            }
        }

        if (allSuccess) {
            dirtyTracker.commitFlush();
        } else {
            dirtyTracker.rollbackFlush();
            LOGGER.warning(Networks.getLocalizationService().getString("messages.ae.persistence.journal_flush_partial", allRows.size() - flushedCount));
        }
    }

    private void batchInsertJournal(List<JournalRow> rows) throws SQLException {
        String sql = "INSERT INTO ae_journal (cell_uuid, op, tpl_id, new_amount, crc32, timestamp, applied) "
            + "VALUES (?, ?, ?, ?, ?, ?, 0)";
        try (Connection conn = connMgr.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JournalRow row : rows) {
                    ps.setString(1, row.cellUuid());
                    ps.setString(2, String.valueOf(row.op()));
                    if (row.tplId() != null) {
                        ps.setLong(3, row.tplId());
                    } else {
                        ps.setNull(3, Types.BIGINT);
                    }
                    if (row.newAmount() != null) {
                        ps.setLong(4, row.newAmount());
                    } else {
                        ps.setNull(4, Types.BIGINT);
                    }
                    ps.setInt(5, row.crc32());
                    ps.setLong(6, row.timestamp());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static <T> List<List<T>> partition(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }
}