package com.ytdd9527.networksexpansion.implementation.machines.cellnet.storage.journal;

import com.ytdd9527.networksexpansion.implementation.machines.cellnet.storage.util.CRC32Utils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
public class DirtyTracker {

    private final AtomicReference<ConcurrentHashMap<UUID, ConcurrentHashMap<Long, DirtyEntry>>> dirtyMapRef =
        new AtomicReference<>(new ConcurrentHashMap<>());
    private final AtomicLong fallbackSequence = new AtomicLong();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<UUID, Map<Long, DirtyEntry>> pendingFlush = null;

    public record DirtyEntry(JournalOp op, long newAmount, long timestamp, long sequence) {
    }

    public void record(@NotNull UUID cellUuid, long tplId, long newAmount, @NotNull JournalOp op) {
        record(cellUuid, tplId, newAmount, op, fallbackSequence.incrementAndGet());
    }

    public void record(@NotNull UUID cellUuid, long tplId, long newAmount, @NotNull JournalOp op, long sequence) {
        DirtyEntry entry = new DirtyEntry(op, newAmount, System.currentTimeMillis(), sequence);
        lock.readLock().lock();
        try {
            dirtyMapRef.get()
                .computeIfAbsent(cellUuid, k -> new ConcurrentHashMap<>())
                .merge(tplId, entry, DirtyTracker::newerEntry);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<JournalRow> drainPhase1() {
        lock.writeLock().lock();
        try {
            ConcurrentHashMap<UUID, ConcurrentHashMap<Long, DirtyEntry>> dirtyMap =
                dirtyMapRef.getAndSet(new ConcurrentHashMap<>());
            if (dirtyMap.isEmpty()) {
                return Collections.emptyList();
            }
            Map<UUID, Map<Long, DirtyEntry>> snapshot = new HashMap<>();
            for (var entry : dirtyMap.entrySet()) {
                snapshot.put(entry.getKey(), new HashMap<>(entry.getValue()));
            }
            pendingFlush = snapshot;
            return toJournalRows(snapshot);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void commitFlush() {
        lock.writeLock().lock();
        try {
            pendingFlush = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void rollbackFlush(@NotNull List<JournalRow> failedRows) {
        lock.writeLock().lock();
        try {
            if (pendingFlush == null) {
                return;
            }
            Map<UUID, Map<Long, DirtyEntry>> snapshot = pendingFlush;
            for (JournalRow row : failedRows) {
                Long tplId = row.tplId();
                if (tplId == null) {
                    continue;
                }
                UUID cellUuid = UUID.fromString(row.cellUuid());
                Map<Long, DirtyEntry> cellMap = snapshot.get(cellUuid);
                if (cellMap == null) {
                    continue;
                }
                DirtyEntry original = cellMap.get(tplId);
                if (original == null) {
                    continue;
                }
                dirtyMapRef.get()
                    .computeIfAbsent(cellUuid, k -> new ConcurrentHashMap<>())
                    .merge(tplId, original, DirtyTracker::newerEntry);
            }
            pendingFlush = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static DirtyEntry newerEntry(DirtyEntry a, DirtyEntry b) {
        if (a.sequence() != b.sequence()) {
            return a.sequence() > b.sequence() ? a : b;
        }
        return a.timestamp() >= b.timestamp() ? a : b;
    }

    private List<JournalRow> toJournalRows(Map<UUID, Map<Long, DirtyEntry>> data) {
        List<JournalRow> rows = new ArrayList<>();
        for (var cellEntry : data.entrySet()) {
            String cellUuid = cellEntry.getKey().toString();
            for (var itemEntry : cellEntry.getValue().entrySet()) {
                DirtyEntry de = itemEntry.getValue();
                JournalOp op = de.op();
                Long tplId = itemEntry.getKey();
                Long newAmount = (op == JournalOp.REMOVE) ? null : de.newAmount();
                int crc = CRC32Utils.computeJournal(cellUuid, op.code(), tplId, newAmount);
                rows.add(new JournalRow(cellUuid, op.code(), tplId, newAmount, crc, de.timestamp()));
            }
        }
        return rows;
    }
}
