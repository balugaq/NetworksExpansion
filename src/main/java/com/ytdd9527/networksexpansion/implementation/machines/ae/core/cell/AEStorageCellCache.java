package com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell;

import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemHashMap;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemKey;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.AEStorageDatabase;
import com.ytdd9527.networksexpansion.utils.itemstacks.ItemStackUtil;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.utils.StackUtils;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储元件的内存缓存，{@link #ACTIVE_CACHES} 以元件 UUID 为键全局共享.
 *
 * <p>持有单格容量（perTypeLimit / currentPerTypeLimit）、已存物品（{@code storage}，
 * 按 {@link ItemKey} 归一化）、物品白名单与自定义名；
 * 写入经 {@link #markItemDirty} 标记脏区，由 AEStorageDatabase 异步落盘。
 * {@link #getStoredItems()} 返回的 {@link CellEntry} 持有净版物品引用，
 * 供菜单展示与量子存储转换复用。
 */
public class AEStorageCellCache {

    private static volatile long maxUnitCount = 0L;

    private static final Map<UUID, AEStorageCellCache> ACTIVE_CACHES = new ConcurrentHashMap<>();

    @Getter
    private final UUID uuid;
    @Getter
    private final long perTypeLimit;
    private final ItemHashMap<Long> storage = new ItemHashMap<>();
    private final List<ItemStack> whitelist = new ArrayList<>();

    private long stored;
    @Getter
    @Setter
    private volatile long currentPerTypeLimit;
    private boolean whitelistEnabled;
    private volatile String customName;

    /** 无限元件：单元数(种类)不受全局 config 上限钳制，与 Long.MAX_VALUE 单格容量配合实现"无限"。 */
    @Setter
    private volatile boolean unlimited;

    public AEStorageCellCache(@NotNull UUID uuid, long perTypeLimit, long currentPerTypeLimit) {
        this.uuid = uuid;
        this.perTypeLimit = perTypeLimit;
        this.currentPerTypeLimit = currentPerTypeLimit;
        this.stored = 0;
    }

    @NotNull
    public static AEStorageCellCache getOrCreate(@NotNull UUID uuid, long perTypeLimit, long currentPerTypeLimit) {
        return ACTIVE_CACHES.computeIfAbsent(uuid, k -> new AEStorageCellCache(uuid, perTypeLimit, currentPerTypeLimit));
    }

    @NotNull
    public static Map<UUID, AEStorageCellCache> getActiveCaches() {
        return ACTIVE_CACHES;
    }

    public static long getMaxUnitCount() {
        long cached = maxUnitCount;
        if (cached <= 0) {
            cached = Math.max(1L, Math.min(64L, Networks.getConfigManager().getAeMaxItemTypes()));
            maxUnitCount = cached;
        }
        return cached;
    }

    public long getMaxUnits() {
        if (unlimited) {
            return Long.MAX_VALUE;
        }
        return Math.min(perTypeLimit, getMaxUnitCount());
    }

    /** 无限元件判定：单格容量为 Long.MAX_VALUE 视作无限。 */
    public static boolean isUnlimitedPerType(long perTypeLimit) {
        return perTypeLimit >= Long.MAX_VALUE / 2;
    }

    public static long getMaxUnitsFor(long perTypeLimit) {
        if (isUnlimitedPerType(perTypeLimit)) {
            return Long.MAX_VALUE;
        }
        return Math.min(perTypeLimit, getMaxUnitCount());
    }

    public synchronized long getStored() {
        return stored;
    }

    @Nullable
    public synchronized ItemStack takeItem(@NotNull ItemStack itemStack, long amount) {
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
    }

    @NotNull
    public synchronized List<CellEntry> getStoredItems() {
        List<CellEntry> result = new ArrayList<>(storage.size());
        for (Map.Entry<ItemKey, Long> entry : storage.keyEntrySet()) {
            result.add(new CellEntry(entry.getKey().getItemStack(), entry.getValue()));
        }
        return result;
    }

    @NotNull
    public synchronized Map<ItemStack, Long> getAllItems() {
        Map<ItemStack, Long> result = new HashMap<>();
        for (Map.Entry<ItemKey, Long> entry : storage.keyEntrySet()) {
            result.put(entry.getKey().getItemStack(), entry.getValue());
        }
        return result;
    }

    public synchronized void accumulateInto(@NotNull ItemHashMap<Long> target, @NotNull Map<ItemKey, List<UUID>> index) {
        for (Map.Entry<ItemKey, Long> entry : storage.keyEntrySet()) {
            ItemKey key = entry.getKey();
            Long existing = target.getKey(key);
            target.putKey(key, (existing != null ? existing : 0L) + entry.getValue());
            index.computeIfAbsent(key, k -> new ArrayList<>()).add(uuid);
        }
    }

    public synchronized void loadItemSilently(@NotNull ItemStack sample, long amount) {
        if (amount <= 0 || sample.getType().isAir()) {
            return;
        }
        ItemKey key = new ItemKey(sample);
        long existing = storage.getOrDefault(key, 0L);
        storage.putKey(key, existing + amount);
        stored += amount;
    }

    public synchronized void loadMetaSilently(boolean enabled, @NotNull List<ItemStack> whitelistItems) {
        this.whitelistEnabled = enabled;
        this.whitelist.clear();
        for (ItemStack item : whitelistItems) {
            ItemStack normalized = item.clone();
            normalized.setAmount(1);
            this.whitelist.add(normalized);
        }
    }

    public synchronized boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    public synchronized void updateWhitelist(boolean whitelistEnabled, @NotNull List<ItemStack> items) {
        this.whitelistEnabled = whitelistEnabled;
        whitelist.clear();
        for (ItemStack item : items) {
            ItemStack normalized = item.clone();
            normalized.setAmount(1);
            whitelist.add(normalized);
        }
        saveWhitelist();
    }

    @NotNull
    public synchronized List<ItemStack> getWhitelist() {
        List<ItemStack> result = new ArrayList<>(whitelist.size());
        for (ItemStack item : whitelist) {
            result.add(item.clone());
        }
        return result;
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

    public synchronized boolean canReceiveItem(@NotNull ItemKey key) {
        Long existing = storage.getKey(key);
        if (existing != null) {
            return existing < perTypeLimit;
        }
        if (whitelistEnabled && !inWhitelist(key.getItemStack())) {
            return false;
        }
        return storage.size() < Math.min(currentPerTypeLimit, getMaxUnits());
    }

    public synchronized int pushItem(@NotNull ItemKey key, int amount) {
        return (int) Math.min(pushItemLong(key, amount), Integer.MAX_VALUE);
    }

    public synchronized long pushItemLong(@NotNull ItemKey key, long amount) {
        long existing = storage.getOrDefault(key, 0L);
        if (existing > 0) {
            long toAdd = Math.min(amount, Math.max(0L, perTypeLimit - existing));
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

        long toAdd = Math.min(amount, perTypeLimit);
        if (toAdd <= 0) {
            return 0;
        }
        storage.putKey(key, toAdd);
        stored += toAdd;
        markItemDirty(key, toAdd);
        return toAdd;
    }

    @Nullable
    public synchronized ItemStack takeItem(@NotNull ItemKey key, long amount) {
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
    }

    public synchronized boolean contains(@NotNull ItemKey key, long amount) {
        Long existing = storage.getKey(key);
        return existing != null && existing >= amount;
    }

    public synchronized long getAmount(@NotNull ItemKey key) {
        Long existing = storage.getKey(key);
        return existing == null ? 0 : existing;
    }

    public static class CellEntry {
        public final ItemStack sample;
        public final long amount;

        public CellEntry(@NotNull ItemStack sample, long amount) {
            this.sample = ItemStackUtil.getCleanItem(sample);
            this.amount = amount;
        }
    }
}
