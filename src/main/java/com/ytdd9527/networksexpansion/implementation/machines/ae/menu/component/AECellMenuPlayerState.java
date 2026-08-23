package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component;

import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellPersistence;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class AECellMenuPlayerState {

    private AECellMenuPlayerState() {
    }

    @Nullable
    public static ItemStack findCellInPlayer(@NotNull Player player, @NotNull UUID uuid) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (matches(main, uuid)) {
            return main;
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (matches(off, uuid)) {
            return off;
        }
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (matches(item, uuid)) {
                return item;
            }
        }
        return null;
    }

    public static void updateHeldCell(@NotNull Player player, @NotNull ItemStack cell) {
        UUID cellUuid = AECellPersistence.getCellUUID(cell);
        if (matches(player.getInventory().getItemInMainHand(), cellUuid)) {
            player.getInventory().setItemInMainHand(cell);
            return;
        }
        if (matches(player.getInventory().getItemInOffHand(), cellUuid)) {
            player.getInventory().setItemInOffHand(cell);
            return;
        }
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (matches(item, cellUuid)) {
                player.getInventory().setItem(i, cell);
                return;
            }
        }
    }

    private static boolean matches(@Nullable ItemStack item, @NotNull UUID uuid) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        UUID cellUuid = AECellPersistence.getCellUUID(item);
        return uuid.equals(cellUuid);
    }
}