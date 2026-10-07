package com.ytdd9527.networksexpansion.implementation.machines.cellnet.support;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NetworkUtil {

    public static final long CACHE_TTL_MS = 5_000L;

    private record CacheEntry(@NotNull NetworkRoot root, long cachedAt) {
    }

    private static final Map<Location, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private NetworkUtil() {
    }

    @Nullable
    public static NetworkRoot findRoot(@NotNull Location location) {
        NodeDefinition self = NetworkStorage.getNode(location);
        NetworkRoot directRoot = extractRoot(self);
        if (directRoot != null) {
            return directRoot;
        }

        Location key = location.clone();
        CacheEntry cached = CACHE.get(key);
        if (cached != null && System.currentTimeMillis() - cached.cachedAt() <= CACHE_TTL_MS) {
            return cached.root();
        }

        NetworkRoot scanned = scanNeighbors(location);
        if (scanned != null) {
            CACHE.put(key, new CacheEntry(scanned, System.currentTimeMillis()));
        } else {
            CACHE.remove(key);
        }
        return scanned;
    }

    public static void invalidate(@Nullable Location location) {
        if (location != null) {
            CACHE.remove(location);
        }
    }

    @Nullable
    private static NetworkRoot scanNeighbors(@NotNull Location location) {
        for (BlockFace face : BlockFace.values()) {
            if (!face.isCartesian()) {
                continue;
            }
            Location neighbor = location.clone().add(face.getDirection());
            NodeDefinition definition = NetworkStorage.getNode(neighbor);
            NetworkRoot root = extractRoot(definition);
            if (root != null) {
                return root;
            }
        }
        return null;
    }

    @Nullable
    private static NetworkRoot extractRoot(@Nullable NodeDefinition definition) {
        if (definition == null || definition.getNode() == null) {
            return null;
        }
        NetworkRoot root = definition.getNode().getRoot();
        return root != null ? root : null;
    }
}
