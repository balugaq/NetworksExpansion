package com.balugaq.netex.api.interfaces;

import com.balugaq.netex.api.enums.FeedbackType;
import com.balugaq.netex.utils.Lang;
import com.balugaq.netex.utils.LocationUtil;
import io.github.sefiraat.networks.Networks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public interface FeedbackSendable {
    Map<UUID, Set<Location>> SUBSCRIBED_LOCATIONS = new ConcurrentHashMap<>();

    static void subscribe(@NotNull Player player, Location location) {
        UUID key = player.getUniqueId();
        if (!SUBSCRIBED_LOCATIONS.containsKey(key)) {
            SUBSCRIBED_LOCATIONS.put(key, ConcurrentHashMap.newKeySet());
        }
        SUBSCRIBED_LOCATIONS.get(key).add(location);
    }

    static void unsubscribe(@NotNull Player player, Location location) {
        UUID key = player.getUniqueId();
        if (SUBSCRIBED_LOCATIONS.containsKey(key)) {
            SUBSCRIBED_LOCATIONS.get(key).remove(location);
        }
    }

    static boolean hasSubscribed(@NotNull Player player, Location location) {
        UUID key = player.getUniqueId();
        if (SUBSCRIBED_LOCATIONS.containsKey(key)) {
            return SUBSCRIBED_LOCATIONS.get(key).contains(location);
        }
        return false;
    }

    static void sendFeedback0(@NotNull Location location, @NotNull FeedbackType type) {
        for (UUID uuid : SUBSCRIBED_LOCATIONS.keySet()) {
            if (SUBSCRIBED_LOCATIONS.get(uuid).contains(location)) {
                dispatch(uuid, location, type.getMessage());
            }
        }
    }

    // Async machine ticks call this; messaging a player is main-thread-only work
    static void dispatch(@NotNull UUID uuid, @NotNull Location location, @NotNull String message) {
        Runnable task = () -> {
            Player player = Bukkit.getServer().getPlayer(uuid);
            if (player != null) {
                player.sendMessage(String.format(
                    Lang.getString("messages.debug.status_view"), LocationUtil.humanizeBlock(location), message));
            }
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(Networks.getInstance(), task);
        }
    }

    default void sendFeedback(@NotNull Location location, @NotNull FeedbackType type) {
        for (UUID uuid : SUBSCRIBED_LOCATIONS.keySet()) {
            if (SUBSCRIBED_LOCATIONS.get(uuid).contains(location)) {
                dispatch(uuid, location, type.getMessage());
            }
        }
    }
}
