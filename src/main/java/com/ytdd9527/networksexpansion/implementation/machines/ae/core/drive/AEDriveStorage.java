package com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive;

import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellHandle;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemHashMap;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemKey;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.utils.StackUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 驱动器存储元件的统一存取入口：pushItems / takeItem / getAllCellItems / getAmount
 * 均汇总驱动器内全部元件执行.
 *
 * <p>缓存分三层：{@link #PLAIN_KEY_CACHE} 按物品种类缓存无 meta 原版物品的 ItemKey；
 * {@link #cellCache} 缓存"驱动器位置 → 元件列表"（位置全局唯一，跨网络共享），
 * 增删元件时经 {@link #invalidateCellCache} 失效并递增代数；
 * {@link NetworkCache} 由 NetworkRoot 按网络各持一份，其中 FlatView 为带 uuid 索引的
 * 展平元件列表，驱动器集合或代数变化时重建并清空条目级缓存；
 * pushCache / takeCache 记录上次成功存取的元件 uuid 供优先命中，
 * notIncluded 缓存已确认缺失的物品避免无效遍历，
 * cachedStorage / itemToStorageIndex 为全量库存快照与"物品 → uuid"索引，
 * 有效期 {@value #STORAGE_CACHE_INTERVAL_MS} ms，期间经 {@link NetworkCache#adjust}
 * 增量修正，过期后重新聚合。
 */
public class AEDriveStorage {

    private static final long STORAGE_CACHE_INTERVAL_MS = 200L;
    private static final Map<Material, ItemKey> PLAIN_KEY_CACHE = new ConcurrentHashMap<>();

    private final Map<Location, List<AECellHandle>> cellCache = new ConcurrentHashMap<>();
    private final AtomicLong cellCacheGeneration = new AtomicLong(0);

    @NotNull
    private static ItemKey getKey(@NotNull ItemStack itemStack) {
        if (itemStack.hasItemMeta()) {
            return new ItemKey(itemStack);
        }
        return PLAIN_KEY_CACHE.computeIfAbsent(itemStack.getType(), m -> new ItemKey(new ItemStack(m)));
    }

    @NotNull
    public List<AECellHandle> getCells(@NotNull BlockMenu menu) {
        Location location = menu.getLocation();
        List<AECellHandle> cached = cellCache.get(location);
        if (cached != null) {
            return cached;
        }
        List<AECellHandle> cells = AEDriveCellManager.collectCells(menu);
        cellCache.put(location, cells);
        AECellUniquenessManager.registerDrive(menu);
        return cells;
    }

    public void invalidateCellCache(@NotNull Location location) {
        cellCache.remove(location);
        cellCacheGeneration.incrementAndGet();
    }

    @NotNull
    private FlatView getFlatView(@NotNull NetworkCache cache, @NotNull Collection<BlockMenu> menus) {
        Set<Location> driveLocations = new HashSet<>();
        for (BlockMenu menu : menus) {
            driveLocations.add(menu.getLocation());
        }
        long generation = cellCacheGeneration.get();
        FlatView cached = cache.getFlatView();
        if (cached != null && cached.locations.equals(driveLocations) && cached.generation == generation) {
            return cached;
        }

        List<AECellHandle> cells = new ArrayList<>();
        Map<UUID, AECellHandle> byUuid = new HashMap<>();
        for (BlockMenu menu : menus) {
            for (AECellHandle cell : getCells(menu)) {
                cells.add(cell);
                byUuid.put(cell.getUuid(), cell);
            }
        }

        cache.clearItemCaches();
        FlatView view = new FlatView(new HashSet<>(driveLocations), cells, byUuid, generation);
        cache.setFlatView(view);
        return view;
    }

    public boolean pushItems(@NotNull NetworkCache cache, @NotNull Collection<BlockMenu> menus, @NotNull Map<ItemStack, Long> items) {
        FlatView view = getFlatView(cache, menus);
        List<AECellHandle> cells = view.cells;
        if (cells.isEmpty()) {
            return false;
        }

        boolean anyPushed = false;

        for (Map.Entry<ItemStack, Long> entry : items.entrySet()) {
            long remaining = entry.getValue();
            if (remaining <= 0) {
                continue;
            }

            ItemStack template = entry.getKey();
            if (template.getType().isAir() || StackUtils.isBlacklisted(template) || AEStorageCell.isStorageCell(template)) {
                continue;
            }

            ItemKey itemKey = getKey(template);
            cache.getNotIncluded().remove(itemKey);

            for (AECellHandle cell : cells) {
                if (remaining <= 0) {
                    break;
                }
                if (!cell.canReceiveItem(itemKey)) {
                    continue;
                }
                int toPush = (int) Math.min(remaining, Integer.MAX_VALUE);
                int pushed = cell.pushItem(itemKey, toPush);
                if (pushed > 0) {
                    anyPushed = true;
                    remaining -= pushed;
                    cache.getPushCache().put(itemKey, cell.getUuid());
                    cache.adjust(itemKey, pushed);
                }
            }

            entry.setValue(remaining);
        }

        return anyPushed;
    }

    @Nullable
    public ItemStack takeItem(@NotNull NetworkCache cache, @NotNull Collection<BlockMenu> menus, @NotNull ItemRequest request) {
        ItemStack requested = request.getItemStack();
        if (requested == null) {
            return null;
        }

        int requestedAmount = request.getAmount();
        if (requestedAmount <= 0) {
            return null;
        }

        ItemKey itemKey = getKey(requested);
        if (cache.getNotIncluded().contains(itemKey)) {
            return null;
        }

        FlatView view = getFlatView(cache, menus);
        List<AECellHandle> cells = view.cells;
        Map<UUID, AECellHandle> byUuid = view.byUuid;
        if (cells.isEmpty()) {
            cache.getNotIncluded().add(itemKey);
            return null;
        }

        List<AECellHandle> cellsToTry = new ArrayList<>();

        UUID cachedUuid = cache.getTakeCache().get(itemKey);
        if (cachedUuid != null) {
            AECellHandle cell = byUuid.get(cachedUuid);
            if (cell != null && cell.contains(itemKey)) {
                cellsToTry.add(cell);
            }
        }

        Map<ItemKey, List<UUID>> index = cache.getItemToStorageIndex();
        if (index != null) {
            List<UUID> uuids = index.get(itemKey);
            if (uuids != null) {
                for (UUID uuid : uuids) {
                    AECellHandle cell = byUuid.get(uuid);
                    if (cell != null && !cellsToTry.contains(cell) && cell.contains(itemKey)) {
                        cellsToTry.add(cell);
                    }
                }
            }
        }

        for (AECellHandle cell : cells) {
            if (!cellsToTry.contains(cell) && cell.contains(itemKey)) {
                cellsToTry.add(cell);
            }
        }

        long remaining = requestedAmount;
        ItemStack result = null;

        for (AECellHandle cell : cellsToTry) {
            if (remaining <= 0) {
                break;
            }

            ItemStack taken = cell.takeItem(itemKey, remaining);
            if (taken != null) {
                if (result == null) {
                    result = taken;
                } else {
                    result.setAmount(result.getAmount() + taken.getAmount());
                }
                remaining -= taken.getAmount();
                cache.getTakeCache().put(itemKey, cell.getUuid());
                cache.adjust(itemKey, -taken.getAmount());
            }
        }

        if (result != null) {
            cache.getNotIncluded().remove(itemKey);
        } else {
            cache.getNotIncluded().add(itemKey);
        }

        return result;
    }

    @NotNull
    public Map<ItemStack, Long> getAllCellItems(@NotNull NetworkCache cache, @NotNull Collection<BlockMenu> menus) {
        FlatView view = getFlatView(cache, menus);
        ItemHashMap<Long> snapshot = cache.getStorage(view.cells);
        Map<ItemStack, Long> result = new HashMap<>();
        for (Map.Entry<ItemKey, Long> entry : snapshot.keyEntrySet()) {
            result.put(entry.getKey().getItemStack(), entry.getValue());
        }
        return result;
    }

    @NotNull
    public Map<ItemStack, Long> getAllCellItems(@NotNull List<AECellHandle> cells) {
        ItemHashMap<Long> snapshot = new ItemHashMap<>();
        for (AECellHandle cell : cells) {
            for (Map.Entry<ItemStack, Long> entry : cell.getAllItems().entrySet()) {
                ItemKey key = new ItemKey(entry.getKey());
                Long existing = snapshot.getKey(key);
                snapshot.putKey(key, existing != null ? existing + entry.getValue() : entry.getValue());
            }
        }
        Map<ItemStack, Long> result = new HashMap<>();
        for (Map.Entry<ItemKey, Long> entry : snapshot.keyEntrySet()) {
            result.put(entry.getKey().getItemStack(), entry.getValue());
        }
        return result;
    }

    public int getAmount(@NotNull NetworkCache cache, @NotNull Collection<BlockMenu> menus, @NotNull ItemStack itemStack) {
        ItemKey itemKey = getKey(itemStack);
        if (cache.getNotIncluded().contains(itemKey)) {
            return 0;
        }
        FlatView view = getFlatView(cache, menus);
        long total = 0;
        for (AECellHandle cell : view.cells) {
            total += cell.getAmount(itemKey);
        }
        if (total > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) total;
    }

    public static final class NetworkCache {
        private final Set<ItemKey> notIncluded = ConcurrentHashMap.newKeySet();
        private final Map<ItemKey, UUID> pushCache = new ConcurrentHashMap<>();
        private final Map<ItemKey, UUID> takeCache = new ConcurrentHashMap<>();
        private volatile ItemHashMap<Long> cachedStorage = null;
        private volatile Map<ItemKey, List<UUID>> itemToStorageIndex = null;
        private volatile long lastCacheTime = 0;
        private volatile FlatView flatView = null;

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
        public FlatView getFlatView() {
            return flatView;
        }

        public void setFlatView(@NotNull FlatView flatView) {
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

    @Getter
    @RequiredArgsConstructor
    public static final class FlatView {
        private final Set<Location> locations;
        private final List<AECellHandle> cells;
        private final Map<UUID, AECellHandle> byUuid;
        private final long generation;
    }
}
