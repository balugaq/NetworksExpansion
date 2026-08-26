package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.drive;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.implementation.machines.ae.blockentity.AEDrive;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive.AECellUniquenessManager;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.slot.AEDriveMenuSlots;
import io.github.sefiraat.networks.Networks;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AEDriveUniquenessListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        BlockMenu menu = resolveDriveMenu(event.getView().getTopInventory());
        if (menu == null) {
            return;
        }
        ItemStack beingPlaced = resolvePlacedItem(event);
        if (beingPlaced == null || !AEStorageCell.isStorageCell(beingPlaced)) {
            return;
        }
        if (!AECellUniquenessManager.isDuplicate(menu, beingPlaced)) {
            AECellUniquenessManager.registerCell(menu.getLocation(), beingPlaced);
            return;
        }
        event.setCancelled(true);
        AECellUniquenessManager.notifyDuplicateRejected(player);
        Bukkit.getScheduler().runTask(Networks.getInstance(), () -> AECellUniquenessManager.scanAndEjectDuplicates(menu, player));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        BlockMenu menu = resolveDriveMenu(event.getView().getTopInventory());
        if (menu == null) {
            return;
        }
        ItemStack dragged = event.getOldCursor();
        if (dragged == null || !AEStorageCell.isStorageCell(dragged)) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        boolean placed = false;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize || !isCellSlot(rawSlot)) {
                continue;
            }
            placed = true;
            if (AECellUniquenessManager.isDuplicate(menu, dragged)) {
                event.setCancelled(true);
                AECellUniquenessManager.notifyDuplicateRejected(player);
                Bukkit.getScheduler().runTask(Networks.getInstance(), () -> AECellUniquenessManager.scanAndEjectDuplicates(menu, player));
                return;
            }
        }
        if (placed) {
            AECellUniquenessManager.registerCell(menu.getLocation(), dragged);
        }
    }

    @Nullable
    private static ItemStack resolvePlacedItem(@NotNull InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        int rawSlot = event.getRawSlot();
        boolean clickedTop = rawSlot >= 0 && rawSlot < top.getSize();
        ClickType click = event.getClick();

        if (click == ClickType.NUMBER_KEY) {
            int hotbar = event.getHotbarButton();
            if (hotbar < 0) {
                return null;
            }
            if (!clickedTop || !isCellSlot(rawSlot)) {
                return null;
            }
            return event.getWhoClicked().getInventory().getItem(hotbar);
        }

        if (click == ClickType.SWAP_OFFHAND) {
            if (!clickedTop || !isCellSlot(rawSlot)) {
                return null;
            }
            return event.getWhoClicked().getInventory().getItemInOffHand();
        }

        if (click.isShiftClick()) {
            if (clickedTop) {
                return null;
            }
            return event.getCurrentItem();
        }

        if (clickedTop && isCellSlot(rawSlot)) {
            return event.getCursor();
        }

        return null;
    }

    private static boolean isCellSlot(int rawSlot) {
        for (int slot : AEDriveMenuSlots.CELL_SLOTS) {
            if (slot == rawSlot) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static BlockMenu resolveDriveMenu(@NotNull Inventory topInventory) {
        if (!(topInventory.getHolder() instanceof BlockMenu menu)) {
            return null;
        }
        SlimefunItem sf = StorageCacheUtils.getSfItem(menu.getLocation());
        return sf instanceof AEDrive ? menu : null;
    }
}