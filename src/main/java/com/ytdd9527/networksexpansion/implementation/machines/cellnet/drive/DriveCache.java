package com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive;

import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.CellHandle;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ItemHashMap;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ItemKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.Cell;
public class DriveCache {

    public static final long STORAGE_CACHE_INTERVAL_MS = 200L;

    private final Set<ItemKey> notIncluded = ConcurrentHashMap.newKeySet();
    private final Map<ItemKey, UUID> pushCache = new ConcurrentHashMap<>();
    private final Map<ItemKey, UUID> takeCache = new ConcurrentHashMap<>();
    private volatile ItemHashMap<Long> cachedStorage = null;

    private volatile Map<ItemKey, List<UUID>> itemToStorageIndex = null;
    private volatile long lastCacheTime = 0;
    private volatile DriveStorage.FlatView inputFlatView = null;
    private volatile DriveStorage.FlatView outputFlatView = null;

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
    public DriveStorage.FlatView getFlatView(@NotNull DriveStorage.DriveViewRole role) {
        return role == DriveStorage.DriveViewRole.INPUT ? inputFlatView : outputFlatView;
    }

    public void setFlatView(@NotNull DriveStorage.DriveViewRole role, @NotNull DriveStorage.FlatView flatView) {
        if (role == DriveStorage.DriveViewRole.INPUT) {
            this.inputFlatView = flatView;
        } else {
            this.outputFlatView = flatView;
        }
    }

    public synchronized ItemHashMap<Long> getStorage(@NotNull List<CellHandle> cells) {
        ItemHashMap<Long> cached = cachedStorage;
        if (cached != null && System.currentTimeMillis() - lastCacheTime < STORAGE_CACHE_INTERVAL_MS) {
            return cached;
        }

        ItemHashMap<Long> result = new ItemHashMap<>();
        Map<ItemKey, List<UUID>> newIndex = new ConcurrentHashMap<>();
        for (CellHandle cell : cells) {
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
