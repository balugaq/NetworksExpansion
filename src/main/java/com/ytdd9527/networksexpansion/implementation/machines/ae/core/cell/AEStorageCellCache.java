package com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell;

import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.AEStorageDatabase;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.utils.StackUtils;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemHashMap;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AEStorageCellCache {

    private static volatile long maxUnitCount = 0L;

    private static final Map<UUID, AEStorageCellCache> ACTIVE_CACHES = new ConcurrentHashMap<>();

    private final UUID uuid;
    private final long perTypeLimit;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ItemHashMap<Long> storage = new ItemHashMap<>();
    private final List<ItemStack> whitelist = new ArrayList<>();

    private long stored;
    private volatile long currentPerTypeLimit;
    private boolean whitelistEnabled;
    private volatile String customName;

    public AEStorageCellCache(@NotNull UUID uuid, long perTypeLimit, long currentPerTypeLimit) {
        this.uuid = uuid;
        this.perTypeLimit = perTypeLimit;
        this.currentPerTypeLimit = currentPerTypeLimit;
        this.stored = 0;
        ACTIVE_CACHES.put(uuid, this);
    }

    @NotNull
    public static AEStorageCellCache getOrCreate(@NotNull UUID uuid, long perTypeLimit, long currentPerTypeLimit) {
        AEStorageCellCache cache = ACTIVE_CACHES.get(uuid);
        if (cache != null) {
            return cache;
        }
        return new AEStorageCellCache(uuid, perTypeLimit, currentPerTypeLimit);
    }

    @NotNull
    public static Map<UUID, AEStorageCellCache> getActiveCaches() {
        return ACTIVE_CACHES;
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    public static long getMaxUnitCount() {
        long cached = maxUnitCount;
        if (cached <= 0) {
            cached = Math.max(1L, Math.min(64L, Networks.getConfigManager().getAeMaxItemTypes()));
            maxUnitCount = cached;
        }
        return cached;
    }

    public long getPerTypeLimit() {
        return perTypeLimit;
    }

    public long getMaxUnits() {
        return Math.min(perTypeLimit, getMaxUnitCount());
    }

    public long getCurrentPerTypeLimit() {
        return currentPerTypeLimit;
    }

    public void setCurrentPerTypeLimit(long currentPerTypeLimit) {
        this.currentPerTypeLimit = currentPerTypeLimit;
    }

    public long getStored() {
        lock.readLock().lock();
        try {
            return stored;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean canAccept(@NotNull ItemStack sample) {
        if (sample.getType().isAir() || StackUtils.isBlacklisted(sample) || isStorageCell(sample)) {
            return false;
        }
        lock.readLock().lock();
        try {
            Long existing = storage.getKey(new ItemKey(sample));
            if (existing != null) {
                return existing < perTypeLimit;
            }
            if (whitelistEnabled && !inWhitelist(sample)) {
                return false;
            }
            return storage.size() < Math.min(currentPerTypeLimit, getMaxUnits());
        } finally {
            lock.readLock().unlock();
        }
    }

    public int pushItem(@NotNull ItemStack itemStack) {
        if (itemStack.getType().isAir() || StackUtils.isBlacklisted(itemStack) || isStorageCell(itemStack)) {
            return 0;
        }

        int amount = itemStack.getAmount();
        lock.writeLock().lock();
        try {
            ItemKey key = new ItemKey(itemStack);
            long existing = storage.getOrDefault(key, 0L);
            if (existing > 0) {
                int toAdd = (int) Math.min(amount, Math.max(0L, perTypeLimit - existing));
                if (toAdd <= 0) {
                    return 0;
                }
                storage.putKey(key, existing + toAdd);
                stored += toAdd;
                itemStack.setAmount(amount - toAdd);
                markItemDirty(key, existing + toAdd);
                return toAdd;
            }

            if (whitelistEnabled && !inWhitelist(itemStack)) {
                return 0;
            }
            if (storage.size() >= Math.min(currentPerTypeLimit, getMaxUnits())) {
                return 0;
            }

            int toAdd = (int) Math.min((long) amount, perTypeLimit);
            if (toAdd <= 0) {
                return 0;
            }
            storage.putKey(key, (long) toAdd);
            stored += toAdd;
            itemStack.setAmount(amount - toAdd);
            markItemDirty(key, toAdd);
            return toAdd;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Nullable
    public ItemStack takeItem(@NotNull ItemStack itemStack, long amount) {
        lock.writeLock().lock();
        try {
            ItemKey key = new ItemKey(itemStack);
            Long existing = storage.getKey(key);
            if (existing == null || existing <= 0) {
                return null;
            }

            long take = Math.min(amount, existing);
            if (take <= 0) {
                return null;
            }

            long remaining = existing - take;
            if (remaining > 0) {
                storage.putKey(key, remaining);
            } else {
                storage.removeKey(key);
            }
            stored -= take;
            markItemDirty(key, remaining);

            ItemStack result = key.getItemStack().clone();
            result.setAmount((int) Math.min(take, Integer.MAX_VALUE));
            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean contains(@NotNull ItemStack itemStack, long amount) {
        lock.readLock().lock();
        try {
            Long existing = storage.getKey(new ItemKey(itemStack));
            return existing != null && existing >= amount;
        } finally {
            lock.readLock().unlock();
        }
    }

    public long getAmount(@NotNull ItemStack itemStack) {
        lock.readLock().lock();
        try {
            Long existing = storage.getKey(new ItemKey(itemStack));
            return existing == null ? 0 : existing;
        } finally {
            lock.readLock().unlock();
        }
    }

    @NotNull
    public List<CellEntry> getStoredItems() {
        lock.readLock().lock();
        try {
            List<CellEntry> result = new ArrayList<>(storage.size());
            for (Map.Entry<ItemKey, Long> entry : storage.keyEntrySet()) {
                result.add(new CellEntry(entry.getKey().getItemStack(), entry.getValue()));
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    @NotNull
    public Map<ItemStack, Long> getAllItems() {
        lock.readLock().lock();
        try {
            Map<ItemStack, Long> result = new HashMap<>();
            for (Map.Entry<ItemKey, Long> entry : storage.keyEntrySet()) {
                result.put(entry.getKey().getItemStack(), entry.getValue());
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Merges this cell's items directly into the target storage/index maps, reusing the immutable
     * {@link ItemKey} instances held by this cell instead of cloning ItemStacks or re-creating keys.
     */
    public void accumulateInto(@NotNull ItemHashMap<Long> target, @NotNull Map<ItemKey, List<UUID>> index) {
        lock.readLock().lock();
        try {
            for (Map.Entry<ItemKey, Long> entry : storage.keyEntrySet()) {
                ItemKey key = entry.getKey();
                Long existing = target.getKey(key);
                target.putKey(key, (existing != null ? existing : 0L) + entry.getValue());
                index.computeIfAbsent(key, k -> new ArrayList<>()).add(uuid);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void loadItemSilently(@NotNull ItemStack sample, long amount) {
        if (amount <= 0 || sample.getType().isAir()) {
            return;
        }
        lock.writeLock().lock();
        try {
            ItemKey key = new ItemKey(sample);
            long existing = storage.getOrDefault(key, 0L);
            storage.putKey(key, existing + amount);
            stored += amount;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void loadMetaSilently(boolean enabled, @NotNull List<ItemStack> whitelistItems) {
        lock.writeLock().lock();
        try {
            this.whitelistEnabled = enabled;
            this.whitelist.clear();
            for (ItemStack item : whitelistItems) {
                ItemStack normalized = item.clone();
                normalized.setAmount(1);
                this.whitelist.add(normalized);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isWhitelistEnabled() {
        lock.readLock().lock();
        try {
            return whitelistEnabled;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setWhitelistEnabled(boolean whitelistEnabled) {
        lock.writeLock().lock();
        try {
            if (this.whitelistEnabled == whitelistEnabled) {
                return;
            }
            this.whitelistEnabled = whitelistEnabled;
        } finally {
            lock.writeLock().unlock();
        }
        saveWhitelist();
    }

    @NotNull
    public List<ItemStack> getWhitelist() {
        lock.readLock().lock();
        try {
            List<ItemStack> result = new ArrayList<>(whitelist.size());
            for (ItemStack item : whitelist) {
                result.add(item.clone());
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setWhitelist(@NotNull List<ItemStack> items) {
        lock.writeLock().lock();
        try {
            whitelist.clear();
            for (ItemStack item : items) {
                ItemStack normalized = item.clone();
                normalized.setAmount(1);
                whitelist.add(normalized);
            }
        } finally {
            lock.writeLock().unlock();
        }
        saveWhitelist();
    }

    @Nullable
    public String getCustomName() {
        return customName;
    }

    public void setCustomName(@Nullable String customName) {
        this.customName = customName;
    }

    private void markItemDirty(@NotNull ItemKey key, long finalAmount) {
        AEStorageDatabase db = Networks.getAeStorageDatabase();
        if (db != null) {
            db.getStorageController().markDirty(uuid, key, finalAmount);
        }
    }

    private void saveWhitelist() {
        AEStorageDatabase db = Networks.getAeStorageDatabase();
        if (db != null) {
            db.getStorageController().saveWhitelist(uuid, isWhitelistEnabled(), getWhitelist());
        }
    }

    private boolean inWhitelist(@NotNull ItemStack sample) {
        for (ItemStack template : whitelist) {
            if (StackUtils.itemsMatch(template, sample)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStorageCell(@NotNull ItemStack sample) {
        return AEStorageCell.isStorageCell(sample);
    }

    public boolean canAccept(@NotNull ItemKey key) {
        lock.readLock().lock();
        try {
            Long existing = storage.getKey(key);
            if (existing != null) {
                return existing < perTypeLimit;
            }
            if (whitelistEnabled && !inWhitelist(key.getItemStack())) {
                return false;
            }
            return storage.size() < Math.min(currentPerTypeLimit, getMaxUnits());
        } finally {
            lock.readLock().unlock();
        }
    }

    public int pushItem(@NotNull ItemKey key, int amount) {
        lock.writeLock().lock();
        try {
            long existing = storage.getOrDefault(key, 0L);
            if (existing > 0) {
                int toAdd = (int) Math.min(amount, Math.max(0L, perTypeLimit - existing));
                if (toAdd <= 0) {
                    return 0;
                }
                storage.putKey(key, existing + toAdd);
                stored += toAdd;
                markItemDirty(key, existing + toAdd);
                return toAdd;
            }

            if (whitelistEnabled && !inWhitelist(key.getItemStack())) {
                return 0;
            }
            if (storage.size() >= Math.min(currentPerTypeLimit, getMaxUnits())) {
                return 0;
            }

            int toAdd = (int) Math.min((long) amount, perTypeLimit);
            if (toAdd <= 0) {
                return 0;
            }
            storage.putKey(key, (long) toAdd);
            stored += toAdd;
            markItemDirty(key, toAdd);
            return toAdd;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Nullable
    public ItemStack takeItem(@NotNull ItemKey key, long amount) {
        lock.writeLock().lock();
        try {
            Long existing = storage.getKey(key);
            if (existing == null || existing <= 0) {
                return null;
            }

            long take = Math.min(amount, existing);
            if (take <= 0) {
                return null;
            }

            long remaining = existing - take;
            if (remaining > 0) {
                storage.putKey(key, remaining);
            } else {
                storage.removeKey(key);
            }
            stored -= take;
            markItemDirty(key, remaining);

            ItemStack result = key.getItemStack().clone();
            result.setAmount((int) Math.min(take, Integer.MAX_VALUE));
            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean contains(@NotNull ItemKey key, long amount) {
        lock.readLock().lock();
        try {
            Long existing = storage.getKey(key);
            return existing != null && existing >= amount;
        } finally {
            lock.readLock().unlock();
        }
    }

    public long getAmount(@NotNull ItemKey key) {
        lock.readLock().lock();
        try {
            Long existing = storage.getKey(key);
            return existing == null ? 0 : existing;
        } finally {
            lock.readLock().unlock();
        }
    }

    public static class CellEntry {
        public final ItemStack sample;
        public final long amount;

        public CellEntry(@NotNull ItemStack sample, long amount) {
            this.sample = sample;
            this.amount = amount;
        }
    }
}