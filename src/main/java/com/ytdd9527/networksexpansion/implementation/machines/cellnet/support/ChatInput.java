package com.ytdd9527.networksexpansion.implementation.machines.cellnet.support;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatInput {

    public enum InputType {
        TARGET, DRIVE_NAME, MANAGER_DRIVE_NAME, MONITOR_SEARCH
    }

    public enum SearchTarget {
        MONITOR, DRIVE_BROWSER, CELL_MENU, CELL_CLEANER, CELL_CONVERTER, DRIVE_MANAGER
    }

    public record TargetContext(@NotNull Location location, int blueprintSlot, boolean keep) {
    }

    public record DriveNameContext(@NotNull Location location) {
    }

    public record SearchContext(
            @NotNull SearchTarget target,
            @NotNull Location location,
            @Nullable UUID playerUuid,
            @Nullable UUID cellUuid) {
    }

    public record Pending(@NotNull InputType type, @NotNull Object context, long createdAt) {
    }

    public static final long TIMEOUT_MS = 30_000L;

    private static final ConcurrentHashMap<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private ChatInput() {
    }

    public static boolean request(@NotNull Player player, @NotNull InputType type, @NotNull Object context) {
        Pending previous = PENDING.put(player.getUniqueId(), new Pending(type, context, System.currentTimeMillis()));
        return previous != null;
    }

    @Nullable
    public static Pending pollPending(@NotNull UUID playerUuid) {
        Pending pending = PENDING.remove(playerUuid);
        if (pending == null) {
            return null;
        }
        if (System.currentTimeMillis() - pending.createdAt() > TIMEOUT_MS) {
            return null;
        }
        return pending;
    }

    public static void clear(@NotNull UUID playerUuid) {
        PENDING.remove(playerUuid);
    }
}
