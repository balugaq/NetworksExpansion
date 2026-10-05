package com.ytdd9527.networksexpansion.implementation.machines.cellnet.chain;

import com.balugaq.netex.api.data.VanillaInventoryWrapper;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public final class ChainTargetCache {

    private static final long TICK_MS = 50L;
    private static final int MAX_EMPTY_SCANS = 3;
    private static final int CACHE_CAPACITY = 4096;

    private record CacheKey(@NotNull Location origin, @NotNull BlockFace direction, boolean vanilla) {
    }

    private static final Map<CacheKey, CachedLine> CACHE = new ConcurrentHashMap<>();

    private record Target(@NotNull Location location, boolean vanilla) {
    }

    private static final class CachedLine {
        private final int distance;
        private volatile List<Target> targets = List.of();
        private volatile List<Location> locationsView = List.of();
        private volatile long expiresAtMs;
        private int emptyScans;

        private CachedLine(int distance) {
            this.distance = distance;
        }
    }

    private ChainTargetCache() {
    }

    public static @NotNull List<Location> targets(
            @NotNull Location origin,
            @NotNull BlockFace direction,
            int distance,
            boolean vanillaTargets,
            int workTtlTicks,
            int idleTtlTicks) {
        long nowMs = System.currentTimeMillis();
        CacheKey key = new CacheKey(origin, direction, vanillaTargets);
        CachedLine line = CACHE.computeIfAbsent(key, k -> new CachedLine(distance));
        if (line.distance != distance) {
            line = new CachedLine(distance);
            CACHE.put(key, line);
        }
        if (nowMs < line.expiresAtMs) {
            return line.locationsView;
        }
        List<Target> scanned = scan(origin, direction, distance, vanillaTargets);
        line.targets = scanned;
        List<Location> view = new ArrayList<>(scanned.size());
        for (Target target : scanned) {
            view.add(target.location());
        }
        line.locationsView = view;
        if (scanned.isEmpty()) {
            line.emptyScans++;
        } else {
            line.emptyScans = 0;
        }
        long ttlTicks = line.emptyScans >= MAX_EMPTY_SCANS ? idleTtlTicks : workTtlTicks;
        line.expiresAtMs = nowMs + ttlTicks * TICK_MS;
        if (CACHE.size() > CACHE_CAPACITY) {
            CACHE.values().removeIf(entry -> nowMs >= entry.expiresAtMs);
        }
        return view;
    }

    @Nullable
    public static BlockMenu resolveMenu(@NotNull Location location, boolean vanilla) {
        if (!vanilla) {
            return StorageCacheUtils.getMenu(location);
        }
        BlockState state = location.getBlock().getState(false);
        if (state instanceof InventoryHolder holder) {
            return new VanillaInventoryWrapper(holder.getInventory(), state);
        }
        return null;
    }

    private static @NotNull List<Target> scan(
        @NotNull Location origin,
        @NotNull BlockFace direction,
        int distance,
        boolean vanillaTargets) {
        List<Target> targets = new ArrayList<>();
        Location cursor = origin.clone();
        for (int i = 0; i < distance; i++) {
            cursor.set(cursor.getBlockX() + direction.getModX(),
                cursor.getBlockY() + direction.getModY(),
                cursor.getBlockZ() + direction.getModZ());
            if (StorageCacheUtils.getMenu(cursor) != null) {
                targets.add(new Target(cursor.clone(), false));
                continue;
            }
            if (vanillaTargets && isVanillaContainer(cursor)) {
                targets.add(new Target(cursor.clone(), true));
                continue;
            }
            break;
        }
        return targets;
    }

    private static boolean isVanillaContainer(@NotNull Location location) {
        BlockState state = location.getBlock().getState(false);
        return state instanceof InventoryHolder;
    }
}
