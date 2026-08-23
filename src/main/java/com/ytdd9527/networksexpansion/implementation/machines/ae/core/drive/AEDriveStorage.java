package com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive;

import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellHandle;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.utils.StackUtils;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemHashMap;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemKey;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AEDriveStorage {

    private static final long STORAGE_CACHE_INTERVAL_MS = 200L;
    private static final Map<Material, ItemKey> PLAIN_KEY_CACHE = new ConcurrentHashMap<>();

    @NotNull
    private static ItemKey getKey(@NotNull ItemStack itemStack) {
        if (itemStack.hasItemMeta()) {
            return new ItemKey(itemStack);
        }
        return PLAIN_KEY_CACHE.computeIfAbsent(itemStack.getType(), m -> new ItemKey(new ItemStack(m)));
    }

    /**
     * Drive-location → handled cell list. A drive location is globally unique and its cells
     * only change through menu interaction, so this cache is safe to share across networks.
     */
    private final Map<Location, List<AECellHandle>> cellCache = new ConcurrentHashMap<>();

    /**
     * Network-identity (controller location) → network-scoped item caches. Item-level caches
     * describe "what does this network currently hold", so they must be isolated per network.
     */
    private final Map<Location, NetworkCache> networkCaches = new ConcurrentHashMap<>();

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
        networkCaches.clear();
    }

    @NotNull
    private NetworkCache getNetworkCache(@NotNull Location key) {
        return networkCaches.computeIfAbsent(key, k -> new NetworkCache());
    }

    /**
     * Returns the flat cell list plus its UUID index for the given network, cached against the
     * current set of drive locations so the result is only rebuilt when a drive is added/removed.
     */
    @NotNull
    private FlatView getFlatView(@NotNull Location key, @NotNull Collection<BlockMenu> menus) {
        NetworkCache cache = getNetworkCache(key);
        Set<Location> driveLocations = new HashSet<>();
        for (BlockMenu menu : menus) {
            driveLocations.add(menu.getLocation());
        }
        FlatView cached = cache.flatView;
        if (cached != null && cached.locations.equals(driveLocations)) {
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
        FlatView view = new FlatView(new HashSet<>(driveLocations), cells, byUuid);
        cache.flatView = view;
        return view;
    }

    public boolean pushItems(@NotNull Location key, @NotNull Collection<BlockMenu> menus, @NotNull Map<ItemStack, Long> items) {
        NetworkCache cache = getNetworkCache(key);
        FlatView view = getFlatView(key, menus);
        List<AECellHandle> cells = view.cells;
        Map<UUID, AECellHandle> byUuid = view.byUuid;
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
            cache.notIncluded.remove(itemKey);

            AECellHandle target = null;

            UUID cachedUuid = cache.pushCache.get(itemKey);
            if (cachedUuid != null) {
                AECellHandle cell = byUuid.get(cachedUuid);
                if (cell != null && cell.canAccept(itemKey)) {
                    target = cell;
                }
            }

            if (target == null) {
                Map<ItemKey, List<UUID>> index = cache.itemToStorageIndex;
                List<UUID> uuids = index != null ? index.get(itemKey) : null;
                if (uuids != null) {
                    for (UUID uuid : uuids) {
                        AECellHandle cell = byUuid.get(uuid);
                        if (cell != null && cell.canAccept(itemKey)) {
                            target = cell;
                            break;
                        }
                    }
                }
            }

            if (target == null) {
                for (AECellHandle cell : cells) {
                    if (cell.canAccept(itemKey)) {
                        target = cell;
                        break;
                    }
                }
            }

            if (target != null && remaining > 0) {
                int toPush = (int) Math.min(remaining, Integer.MAX_VALUE);
                int pushed = target.pushItem(itemKey, toPush);
                if (pushed > 0) {
                    anyPushed = true;
                    remaining -= pushed;
                    cache.pushCache.put(itemKey, target.getUuid());
                    adjustCache(cache, itemKey, pushed);
                }
            }

            entry.setValue(remaining);
        }

        return anyPushed;
    }

    @Nullable
    public ItemStack takeItem(@NotNull Location key, @NotNull Collection<BlockMenu> menus, @NotNull ItemRequest request) {
        NetworkCache cache = getNetworkCache(key);
        ItemStack requested = request.getItemStack();
        if (requested == null) {
            return null;
        }

        int requestedAmount = request.getAmount();
        if (requestedAmount <= 0) {
            return null;
        }

        ItemKey itemKey = getKey(requested);
        if (cache.notIncluded.contains(itemKey)) {
            return null;
        }

        FlatView view = getFlatView(key, menus);
        List<AECellHandle> cells = view.cells;
        Map<UUID, AECellHandle> byUuid = view.byUuid;
        if (cells.isEmpty()) {
            cache.notIncluded.add(itemKey);
            return null;
        }

        List<AECellHandle> orderedCells = new ArrayList<>();

        UUID cachedUuid = cache.takeCache.get(itemKey);
        if (cachedUuid != null) {
            AECellHandle cell = byUuid.get(cachedUuid);
            if (cell != null && cell.contains(itemKey)) {
                orderedCells.add(cell);
            }
        }

        Map<ItemKey, List<UUID>> index = cache.itemToStorageIndex;
        if (index != null) {
            List<UUID> uuids = index.get(itemKey);
            if (uuids != null) {
                for (UUID uuid : uuids) {
                    AECellHandle cell = byUuid.get(uuid);
                    if (cell != null && !orderedCells.contains(cell) && cell.contains(itemKey)) {
                        orderedCells.add(cell);
                    }
                }
            }
        }

        for (AECellHandle cell : cells) {
            if (!orderedCells.contains(cell) && cell.contains(itemKey)) {
                orderedCells.add(cell);
            }
        }

        long remaining = requestedAmount;
        ItemStack result = null;

        for (AECellHandle cell : orderedCells) {
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
                cache.takeCache.put(itemKey, cell.getUuid());
                adjustCache(cache, itemKey, -taken.getAmount());
            }
        }

        if (result != null) {
            cache.notIncluded.remove(itemKey);
        } else {
            cache.notIncluded.add(itemKey);
        }

        return result;
    }

    @NotNull
    public Map<ItemStack, Long> getAllCellItems(@NotNull Location key, @NotNull Collection<BlockMenu> menus) {
        NetworkCache cache = getNetworkCache(key);
        FlatView view = getFlatView(key, menus);
        ItemHashMap<Long> snapshot = getStorageUnsafe(cache, view.cells);
        Map<ItemStack, Long> result = new LinkedHashMap<>();
        for (Map.Entry<ItemKey, Long> entry : snapshot.keyEntrySet()) {
            result.put(entry.getKey().getItemStack(), entry.getValue());
        }
        return result;
    }

    /**
     * Non-cached aggregate for a single drive's menu display. The display shows one drive's cells
     * only and is player-facing/infrequent, so it must not touch the network-scoped storage cache.
     */
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
        Map<ItemStack, Long> result = new LinkedHashMap<>();
        for (Map.Entry<ItemKey, Long> entry : snapshot.keyEntrySet()) {
            result.put(entry.getKey().getItemStack(), entry.getValue());
        }
        return result;
    }

    public int getAmount(@NotNull Location key, @NotNull Collection<BlockMenu> menus, @NotNull ItemStack itemStack) {
        NetworkCache cache = getNetworkCache(key);
        ItemKey itemKey = getKey(itemStack);
        if (cache.notIncluded.contains(itemKey)) {
            return 0;
        }
        FlatView view = getFlatView(key, menus);
        long total = 0;
        for (AECellHandle cell : view.cells) {
            total += cell.getAmount(itemKey);
        }
        if (total > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) total;
    }

    private ItemHashMap<Long> getStorageUnsafe(@NotNull NetworkCache cache, @NotNull List<AECellHandle> cells) {
        ItemHashMap<Long> cached = cache.cachedStorage;
        if (cached != null && System.currentTimeMillis() - cache.lastCacheTime < STORAGE_CACHE_INTERVAL_MS) {
            return cached;
        }

        synchronized (cache.cacheLock) {
            cached = cache.cachedStorage;
            if (cached != null && System.currentTimeMillis() - cache.lastCacheTime < STORAGE_CACHE_INTERVAL_MS) {
                return cached;
            }

            ItemHashMap<Long> result = new ItemHashMap<>();
            Map<ItemKey, List<UUID>> newIndex = new HashMap<>();

            for (AECellHandle cell : cells) {
                cell.accumulateInto(result, newIndex);
            }

            cache.itemToStorageIndex = newIndex;
            cache.cachedStorage = result;
            cache.lastCacheTime = System.currentTimeMillis();
            return result;
        }
    }

    private void adjustCache(@NotNull NetworkCache cache, @NotNull ItemKey key, long delta) {
        synchronized (cache.cacheLock) {
            ItemHashMap<Long> cached = cache.cachedStorage;
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
    }

    private static final class NetworkCache {
        private final Set<ItemKey> notIncluded = ConcurrentHashMap.newKeySet();
        private final Map<ItemKey, UUID> pushCache = new ConcurrentHashMap<>();
        private final Map<ItemKey, UUID> takeCache = new ConcurrentHashMap<>();
        private final Object cacheLock = new Object();
        private volatile ItemHashMap<Long> cachedStorage = null;
        private volatile Map<ItemKey, List<UUID>> itemToStorageIndex = null;
        private volatile long lastCacheTime = 0;
        private volatile FlatView flatView = null;
    }

    private static final class FlatView {
        private final Set<Location> locations;
        private final List<AECellHandle> cells;
        private final Map<UUID, AECellHandle> byUuid;

        private FlatView(@NotNull Set<Location> locations, @NotNull List<AECellHandle> cells, @NotNull Map<UUID, AECellHandle> byUuid) {
            this.locations = locations;
            this.cells = cells;
            this.byUuid = byUuid;
        }
    }
}