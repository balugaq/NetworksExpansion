package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.cell;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellPersistence;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component.AECellLore;
import io.github.sefiraat.networks.Networks;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AECellMenuListener implements Listener {

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent e) {
        AECellMenu.stopOpening(e.getPlayer().getUniqueId());
        AECellMenu.stopRenaming(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (isOpeningCell(player, e.getCurrentItem())) {
            closeCellView(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(@NotNull InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (isOpeningCell(player, e.getOldCursor())) {
            closeCellView(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(@NotNull PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        if (isOpeningCell(player, e.getItemDrop().getItemStack())) {
            closeCellView(player);
        }
    }

    private static boolean isOpeningCell(@NotNull Player player, @Nullable ItemStack item) {
        UUID cellUuid = AECellMenu.getOpeningCell(player.getUniqueId());
        return cellUuid != null && cellUuid.equals(AEStorageCell.getCellUUID(item));
    }

    private static void closeCellView(@NotNull Player player) {
        Bukkit.getScheduler().runTask(Networks.getInstance(), () -> {
            AECellMenu.stopOpening(player.getUniqueId());
            player.closeInventory();
        });
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onChat(@NotNull AsyncChatEvent e) {
        Player player = e.getPlayer();
        UUID cellUuid = AECellMenu.getRenamingCell(player.getUniqueId());
        if (cellUuid == null) {
            return;
        }
        // 聊天被其它插件先取消时，不消费消息，只清掉重命名状态以免卡住
        if (e.isCancelled()) {
            AECellMenu.stopRenaming(player.getUniqueId());
            player.sendMessage(Lang.getString("messages.ae.cell.rename_cancelled"));
            return;
        }
        e.setCancelled(true);
        AECellMenu.stopRenaming(player.getUniqueId());

        String name = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        if (name.isEmpty()) {
            player.sendMessage(Lang.getString("messages.ae.cell.rename_cancelled"));
            return;
        }

        // 重命名涉及物品 meta 与菜单打开，需在主线程执行
        Bukkit.getScheduler().runTask(Networks.getInstance(), () -> {
            ItemStack cell = AECellMenu.findCellInPlayer(player, cellUuid);
            if (cell == null) {
                player.sendMessage(Lang.getString("messages.ae.cell.rename_failed"));
                return;
            }
            AECellPersistence.setCustomName(cell, name);
            long per = AEStorageCell.getPerTypeLimit(cell);
            long cur = AEStorageCell.getCurrentPerTypeLimit(cell);
            AECellLore.applySpecLore(cell, per, cur);
            AECellMenu.updateHeldCell(player, cell);
            AECellMenu.open(player, cell);
            player.sendMessage(Lang.getString("messages.ae.cell.rename_success", name));
        });
    }
}
