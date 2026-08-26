package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.journal;

import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.util.CRC32Utils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class DirtyTracker {

    private final AtomicReference<ConcurrentHashMap<UUID, ConcurrentHashMap<Long, DirtyEntry>>> dirtyMapRef =
        new AtomicReference<>(new ConcurrentHashMap<>());
    private final AtomicLong fallbackSequence = new AtomicLong();
    private volatile Map<UUID, Map<Long, DirtyEntry>> pendingFlush = null;
    private final ReentrantLock flushLock = new ReentrantLock();

    public record DirtyEntry(char op, long newAmount, long timestamp, long sequence) {
    }

    public void record(@NotNull UUID cellUuid, long tplId, long newAmount, char op) {
        record(cellUuid, tplId, newAmount, op, fallbackSequence.incrementAndGet());
    }

    public void record(@NotNull UUID cellUuid, long tplId, long newAmount, char op, long sequence) {
        DirtyEntry entry = new DirtyEntry(op, newAmount, System.currentTimeMillis(), sequence);
        dirtyMapRef.get()
            .computeIfAbsent(cellUuid, k -> new ConcurrentHashMap<>())
            .merge(tplId, entry, DirtyTracker::newerEntry);
    }

    public List<JournalRow> drainPhase1() {
        flushLock.lock();
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
            flushLock.unlock();
        }
    }

    public void commitFlush() {
        flushLock.lock();
        try {
            pendingFlush = null;
        } finally {
            flushLock.unlock();
        }
    }

    public void rollbackFlush() {
        flushLock.lock();
        try {
            if (pendingFlush == null) {
                return;
            }
            for (var cellEntry : pendingFlush.entrySet()) {
                UUID cellUuid = cellEntry.getKey();
                ConcurrentHashMap<Long, DirtyEntry> cellMap =
                    dirtyMapRef.get().computeIfAbsent(cellUuid, k -> new ConcurrentHashMap<>());
                for (var itemEntry : cellEntry.getValue().entrySet()) {
                    cellMap.merge(itemEntry.getKey(), itemEntry.getValue(), DirtyTracker::newerEntry);
                }
            }
            pendingFlush = null;
        } finally {
            flushLock.unlock();
        }
    }

    private static DirtyEntry newerEntry(DirtyEntry a, DirtyEntry b) {
        if (a.sequence() != b.sequence()) {
            return a.sequence() > b.sequence() ? a : b;
        }
        return a.timestamp() >= b.timestamp() ? a : b;
    }

    private List<JournalRow> toJournalRows(Map<UUID, Map<Long, DirtyEntry>> data) {
        java.util.List<JournalRow> rows = new java.util.ArrayList<>();
        for (var cellEntry : data.entrySet()) {
            String cellUuid = cellEntry.getKey().toString();
            for (var itemEntry : cellEntry.getValue().entrySet()) {
                DirtyEntry de = itemEntry.getValue();
                Long tplId = itemEntry.getKey() == -1L ? null : itemEntry.getKey();
                Long newAmount = (de.op() == 'D' || de.op() == 'R') ? null : de.newAmount();
                int crc = CRC32Utils.computeJournal(cellUuid, de.op(), tplId, newAmount);
                rows.add(new JournalRow(cellUuid, de.op(), tplId, newAmount, crc, de.timestamp()));
            }
        }
        return rows;
    }
}