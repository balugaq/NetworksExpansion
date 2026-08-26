package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.cell;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellPersistence;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component.AECellLore;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component.AECellMenuPlayerState;
import io.github.sefiraat.networks.Networks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class AECellMenuListener implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrop(@NotNull PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        if (!AECellMenu.isOpening(player.getUniqueId())) {
            return;
        }
        if (AEStorageCell.isStorageCell(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
            player.sendMessage(Lang.getString("messages.ae.cell.cannot_drop"));
        }
    }

    @EventHandler
    public void onClose(@NotNull InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) {
            return;
        }
        if (!AECellMenu.isOpening(player.getUniqueId())) {
            return;
        }
        Bukkit.getScheduler().runTask(Networks.getInstance(), () -> {
            InventoryView view = player.getOpenInventory();
            if (view == null) {
                AECellMenu.stopOpening(player.getUniqueId());
                return;
            }
            switch (view.getType()) {
                case CHEST, ENDER_CHEST, SHULKER_BOX -> {
                }
                default -> AECellMenu.stopOpening(player.getUniqueId());
            }
        });
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent e) {
        AECellMenu.stopOpening(e.getPlayer().getUniqueId());
        AECellMenu.stopRenaming(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(@NotNull AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        UUID cellUuid = AECellMenu.getRenamingCell(player.getUniqueId());
        if (cellUuid == null) {
            return;
        }
        e.setCancelled(true);
        AECellMenu.stopRenaming(player.getUniqueId());

        String name = e.getMessage().trim();
        if (name.isEmpty()) {
            player.sendMessage(Lang.getString("messages.ae.cell.rename_cancelled"));
            return;
        }

        ItemStack cell = AECellMenuPlayerState.findCellInPlayer(player, cellUuid);
        if (cell == null) {
            player.sendMessage(Lang.getString("messages.ae.cell.rename_failed"));
            return;
        }

        AECellPersistence.setCustomName(cell, name);
        long per = AEStorageCell.getPerTypeLimit(cell);
        long cur = AEStorageCell.getCurrentPerTypeLimit(cell);
        AECellLore.applySpecLore(cell, per, cur);
        AECellMenuPlayerState.updateHeldCell(player, cell);
        AECellMenu.open(player, cell);
        player.sendMessage(Lang.getString("messages.ae.cell.rename_success", name));
    }
}
