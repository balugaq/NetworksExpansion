package com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive.menu;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.api.DriveType;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.StorageCell;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.VoidCell;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.menu.CellMenu;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.menu.VoidCellMenu;
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
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.Cell;
public class DriveCellOpenListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClick() != ClickType.RIGHT) {
            return;
        }
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof BlockMenu menu)) {
            return;
        }
        SlimefunItem sf = StorageCacheUtils.getSfItem(menu.getLocation());
        if (DriveType.of(sf) != DriveType.STANDARD) {
            return;
        }
        int rawSlot = event.getRawSlot();
        if (!DriveSlotGuard.isCellSlot(rawSlot)) {
            return;
        }
        ItemStack cell = event.getCurrentItem();
        if (cell == null || cell.getType().isAir()) {
            return;
        }
        if (openCellFromSlot(menu, rawSlot, cell, player)) {
            event.setCancelled(true);
        }
    }

    private static boolean openCellFromSlot(
            @NotNull BlockMenu menu, int rawSlot, @NotNull ItemStack cell, @NotNull Player player) {
        SlimefunItem cellItem = SlimefunItem.getByItem(cell);
        if (cellItem instanceof VoidCell) {
            Bukkit.getScheduler().runTask(
                Networks.getInstance(),
                () -> VoidCellMenu.openFromDrive(menu, rawSlot, player));
            return true;
        }
        if (cellItem instanceof StorageCell) {
            ItemStack workingCopy = cell.clone();
            Bukkit.getScheduler().runTask(
                Networks.getInstance(),
                () -> CellMenu.open(player, workingCopy));
            return true;
        }
        return false;
    }
}
