package com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive;

import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellHandle;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemHashMap;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网络级 AE 缓存：由 NetworkRoot 按网络各持一份，供驱动器存取时复用与增量修正。
 *
 * <p>notIncluded 缓存已确认缺失的物品避免无效遍历；pushCache / takeCache 记录上次成功存取
 * 的元件 uuid 供优先命中；cachedStorage / itemToStorageIndex 为全量库存快照与"物品 → uuid"
 * 索引，有效期 {@value #STORAGE_CACHE_INTERVAL_MS} ms，期间经 {@link #adjust} 增量修正，
 * 过期后重新聚合；flatView 为带 uuid 索引的展平元件列表，驱动器集合或代数变化时重建。
 */
public class AENetworkCache {

    public static final long STORAGE_CACHE_INTERVAL_MS = 200L;

    private final Set<ItemKey> notIncluded = ConcurrentHashMap.newKeySet();
    private final Map<ItemKey, UUID> pushCache = new ConcurrentHashMap<>();
    private final Map<ItemKey, UUID> takeCache = new ConcurrentHashMap<>();
    private volatile ItemHashMap<Long> cachedStorage = null;
    private volatile Map<ItemKey, List<UUID>> itemToStorageIndex = null;
    private volatile long lastCacheTime = 0;
    private volatile AEDriveStorage.FlatView flatView = null;

    public Set<ItemKey> getNotIncluded() {
        return notIncluded;
    }

    public Map<ItemKey, UUID> getPushCache() {
        return pushCache;
    }

    public Map<ItemKey, UUID> getTakeCache() {
        return takeCache;
    }

    @Nullable
    public Map<ItemKey, List<UUID>> getItemToStorageIndex() {
        return itemToStorageIndex;
    }

    @Nullable
    public AEDriveStorage.FlatView getFlatView() {
        return flatView;
    }

    public void setFlatView(@NotNull AEDriveStorage.FlatView flatView) {
        this.flatView = flatView;
    }

    public synchronized ItemHashMap<Long> getStorage(@NotNull List<AECellHandle> cells) {
        ItemHashMap<Long> cached = cachedStorage;
        if (cached != null && System.currentTimeMillis() - lastCacheTime < STORAGE_CACHE_INTERVAL_MS) {
            return cached;
        }

        ItemHashMap<Long> result = new ItemHashMap<>();
        Map<ItemKey, List<UUID>> newIndex = new HashMap<>();
        for (AECellHandle cell : cells) {
            cell.accumulateInto(result, newIndex);
        }
        itemToStorageIndex = newIndex;
        cachedStorage = result;
        lastCacheTime = System.currentTimeMillis();
        return result;
    }

    public synchronized void adjust(@NotNull ItemKey key, long delta) {
        ItemHashMap<Long> cached = cachedStorage;
        if (cached == null) {
            return;
        }
        Long current = cached.getKey(key);
        long newValue = (current != null ? current : 0L) + delta;
        if (newValue <= 0) {
            cached.removeKey(key);
        } else {
            cached.putKey(key, newValue);
        }
    }

    public synchronized void clearItemCaches() {
        cachedStorage = null;
        itemToStorageIndex = null;
        lastCacheTime = 0;
        notIncluded.clear();
    }
}
