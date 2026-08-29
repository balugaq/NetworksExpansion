package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.drive;

import com.balugaq.netex.utils.Lang;
import io.github.sefiraat.networks.Networks;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 白名单输入监听：玩家处于"输入玩家名"状态时拦截聊天并解析名称，
 * 其余聊天不受影响；玩家退出时清理残留状态。
 */
public class AEDriveWhitelistListener implements Listener {

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent e) {
        AEDriveWhitelist.stopAdding(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(@NotNull AsyncChatEvent e) {
        Player player = e.getPlayer();
        UUID playerUuid = player.getUniqueId();
        Location location = AEDriveWhitelist.getAddingLocation(playerUuid);
        if (location == null) {
            return;
        }
        e.setCancelled(true);
        AEDriveWhitelist.stopAdding(playerUuid);

        String name = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        if (name.isEmpty()) {
            player.sendMessage(Lang.getString("messages.ae.drive.whitelist_name_invalid"));
            return;
        }
        if (name.length() > 16) {
            player.sendMessage(Lang.getString("messages.ae.drive.whitelist_name_too_long"));
            return;
        }

        Bukkit.getScheduler().runTask(Networks.getInstance(), () -> {
            if (!player.isOnline()) {
                return;
            }
            AEDriveWhitelist.addWhitelistPlayer(player, location, name);
        });
    }
}