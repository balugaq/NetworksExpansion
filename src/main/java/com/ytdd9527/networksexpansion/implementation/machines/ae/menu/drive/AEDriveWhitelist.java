package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.drive;

import com.balugaq.netex.utils.Lang;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.implementation.machines.ae.blockentity.AEDrive;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive.AEDriveWhitelistManager;
import io.github.sefiraat.networks.Networks;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 驱动器白名单菜单：owner 在此查看、添加、移除被授权玩家.
 *
 * <p>添加流程要求 owner 在聊天栏输入玩家名，输入状态记录于 {@link #ADDING_WHITELIST}，
 * 供 {@link AEDriveWhitelistListener} 拦截聊天；{@value #WHITELIST_ADD_TIMEOUT_TICKS}
 * ticks 无响应或玩家退出时自动清除。白名单数据由 {@link AEDriveWhitelistManager} 读写。
 */
public final class AEDriveWhitelist {

    public static final int MAX_WHITELIST_SIZE = 14;
    private static final long WHITELIST_ADD_TIMEOUT_TICKS = 1200L;

    private static final int[] WHITELIST_MENU_BACKGROUND = new int[]{
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        9, 10, 11, 12, 14, 15, 16, 17,
        18, 19, 20, 21, 23, 24, 25, 26,
        27, 35, 36, 44,
        45, 46, 47, 48, 50, 51, 52, 53
    };
    private static final int WHITELIST_OWNER_SLOT = 13;
    private static final int WHITELIST_ADD_BUTTON_SLOT = 22;
    private static final int[] WHITELIST_PLAYER_SLOTS = new int[]{
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    private static final int WHITELIST_BACK_BUTTON_SLOT = 49;

    private static final Map<UUID, Location> ADDING_WHITELIST = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ADDING_TIMERS = new ConcurrentHashMap<>();

    private AEDriveWhitelist() {
    }

    public static boolean canManageWhitelist(@NotNull Player player, @NotNull Location location) {
        if (player.isOp() || player.hasPermission("slimefun.inventory.bypass")) {
            return true;
        }
        UUID ownerUuid = AEDrive.getOwnerUuid(location);
        return ownerUuid != null && ownerUuid.equals(player.getUniqueId());
    }

    public static void open(@NotNull BlockMenu driveMenu, @NotNull Location location, @NotNull Player player) {
        if (!canManageWhitelist(player, location)) {
            player.sendMessage(Lang.getString("messages.ae.drive.whitelist_not_owner"));
            return;
        }

        ChestMenu menu = new ChestMenu(Lang.getString("messages.ae.drive.whitelist_title"));
        menu.setPlayerInventoryClickable(true);

        for (int slot : WHITELIST_MENU_BACKGROUND) {
            menu.addItem(slot, ChestMenuUtils.getBackground(), (p, s, i, a) -> false);
        }

        menu.addItem(WHITELIST_ADD_BUTTON_SLOT, buildAddButton(), (p, s, i, a) -> {
            startAddingWhitelist(p, location);
            return false;
        });

        menu.addItem(WHITELIST_BACK_BUTTON_SLOT, AEDrive.backButton(), (p, s, i, a) -> {
            driveMenu.open(p);
            return false;
        });

        menu.setEmptySlotsClickable(false);

        render(menu, location);
        menu.open(player);
    }

    @NotNull
    public static ItemStack buildWhitelistButton(@NotNull Location location) {
        UUID ownerUuid = AEDrive.getOwnerUuid(location);
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        item.editMeta(meta -> {
            List<String> lore = new ArrayList<>();
            meta.setDisplayName(Lang.getString("messages.ae.drive.whitelist_button"));
            if (ownerUuid != null) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(ownerUuid);
                // 离线玩家名字可能解析不出（null），此时设 owner 会 NPE（GameProfile name null）
                if (op != null && op.getName() != null && meta instanceof SkullMeta skull) {
                    skull.setOwningPlayer(op);
                }
                String ownerName = op != null && op.getName() != null ? op.getName() : ownerUuid.toString();
                lore.add(Lang.getString("messages.ae.drive.whitelist_owner", ownerName));
                lore.add(Lang.getString("messages.ae.drive.whitelist_count", AEDriveWhitelistManager.getWhitelistSize(ownerUuid)));
            } else {
                lore.add(Lang.getString("messages.ae.drive.whitelist_no_owner"));
            }
            lore.add("");
            lore.add(Lang.getString("messages.ae.drive.whitelist_open_hint"));
            meta.setLore(lore);
        });
        return item;
    }

    public static void addWhitelistPlayer(@NotNull Player player, @NotNull Location location, @NotNull String name) {
        if (!canManageWhitelist(player, location)) {
            player.sendMessage(Lang.getString("messages.ae.drive.whitelist_not_owner"));
            return;
        }
        UUID ownerUuid = AEDrive.getOwnerUuid(location);
        if (ownerUuid == null) {
            return;
        }
        OfflinePlayer target = Bukkit.getPlayerExact(name);
        if (target == null) {
            target = Bukkit.getOfflinePlayer(name);
            if (!target.hasPlayedBefore()) {
                player.sendMessage(Lang.getString("messages.ae.drive.whitelist_player_not_found"));
                return;
            }
        }
        UUID targetUuid = target.getUniqueId();
        if (ownerUuid.equals(targetUuid)) {
            player.sendMessage(Lang.getString("messages.ae.drive.whitelist_is_owner"));
            return;
        }
        if (AEDriveWhitelistManager.isWhitelisted(ownerUuid, targetUuid)) {
            player.sendMessage(Lang.getString("messages.ae.drive.whitelist_already"));
            return;
        }
        if (AEDriveWhitelistManager.getWhitelistSize(ownerUuid) >= MAX_WHITELIST_SIZE) {
            player.sendMessage(Lang.getString("messages.ae.drive.whitelist_full"));
            reopenWhitelistMenu(player, location);
            return;
        }
        AEDriveWhitelistManager.add(ownerUuid, targetUuid);
        String targetName = target.getName() == null ? name : target.getName();
        player.sendMessage(Lang.getString("messages.ae.drive.whitelist_added", targetName));
        reopenWhitelistMenu(player, location);
    }

    @Nullable
    public static Location getAddingLocation(@NotNull UUID playerUuid) {
        return ADDING_WHITELIST.get(playerUuid);
    }

    public static void stopAdding(@NotNull UUID playerUuid) {
        ADDING_WHITELIST.remove(playerUuid);
        Integer taskId = ADDING_TIMERS.remove(playerUuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private static void startAddingWhitelist(@NotNull Player player, @NotNull Location location) {
        UUID playerUuid = player.getUniqueId();
        stopAdding(playerUuid);
        ADDING_WHITELIST.put(playerUuid, location);
        player.closeInventory();
        player.sendMessage(Lang.getString("messages.ae.drive.whitelist_enter_name"));

        int taskId = Bukkit.getScheduler().runTaskLater(Networks.getInstance(), () -> {
            if (ADDING_WHITELIST.remove(playerUuid) == null) {
                return;
            }
            ADDING_TIMERS.remove(playerUuid);
            Player timeoutPlayer = Bukkit.getPlayer(playerUuid);
            if (timeoutPlayer != null) {
                timeoutPlayer.sendMessage(Lang.getString("messages.ae.drive.whitelist_add_timeout"));
            }
        }, WHITELIST_ADD_TIMEOUT_TICKS).getTaskId();
        ADDING_TIMERS.put(playerUuid, taskId);
    }

    private static void reopenWhitelistMenu(@NotNull Player player, @NotNull Location location) {
        BlockMenu driveMenu = StorageCacheUtils.getMenu(location);
        if (driveMenu == null) {
            return;
        }
        open(driveMenu, location, player);
    }

    private static void render(@NotNull ChestMenu menu, @NotNull Location location) {
        menu.replaceExistingItem(WHITELIST_OWNER_SLOT, buildOwnerInfo(location));
        menu.addMenuClickHandler(WHITELIST_OWNER_SLOT, (p, s, i, a) -> false);

        UUID ownerUuid = AEDrive.getOwnerUuid(location);
        List<UUID> whitelist = ownerUuid == null ? List.of() : AEDriveWhitelistManager.getWhitelist(ownerUuid);
        for (int i = 0; i < WHITELIST_PLAYER_SLOTS.length; i++) {
            int slot = WHITELIST_PLAYER_SLOTS[i];
            if (i < whitelist.size()) {
                UUID targetUuid = whitelist.get(i);
                menu.replaceExistingItem(slot, buildPlayerHead(targetUuid));
                menu.addMenuClickHandler(slot, (p, s, it, a) -> {
                    removeWhitelistPlayer(menu, location, p, targetUuid);
                    return false;
                });
            } else {
                menu.replaceExistingItem(slot, new ItemStack(Material.AIR));
                menu.addMenuClickHandler(slot, (p, s, it, a) -> false);
            }
        }
    }

    private static void removeWhitelistPlayer(@NotNull ChestMenu menu, @NotNull Location location,
                                              @NotNull Player player, @NotNull UUID targetUuid) {
        if (!canManageWhitelist(player, location)) {
            player.sendMessage(Lang.getString("messages.ae.drive.whitelist_not_owner"));
            return;
        }
        UUID ownerUuid = AEDrive.getOwnerUuid(location);
        if (ownerUuid == null) {
            return;
        }
        if (!AEDriveWhitelistManager.isWhitelisted(ownerUuid, targetUuid)) {
            return;
        }
        AEDriveWhitelistManager.remove(ownerUuid, targetUuid);
        OfflinePlayer removedPlayer = Bukkit.getOfflinePlayer(targetUuid);
        String removedName = removedPlayer.getName() == null ? targetUuid.toString() : removedPlayer.getName();
        player.sendMessage(Lang.getString("messages.ae.drive.whitelist_removed", removedName));
        render(menu, location);
    }

    @NotNull
    private static ItemStack buildOwnerInfo(@NotNull Location location) {
        UUID ownerUuid = AEDrive.getOwnerUuid(location);
        OfflinePlayer op = ownerUuid == null ? null : Bukkit.getOfflinePlayer(ownerUuid);
        String ownerName = ownerUuid == null ? "?"
            : (op != null && op.getName() != null ? op.getName() : ownerUuid.toString());
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        item.editMeta(meta -> {
            if (op != null && op.getName() != null && meta instanceof SkullMeta skull) {
                skull.setOwningPlayer(op);
            }
            meta.setDisplayName(Lang.getString("messages.ae.drive.whitelist_owner_head", ownerName));
            meta.setLore(List.of(Lang.getString("messages.ae.drive.whitelist_owner_head_lore")));
        });
        return item;
    }

    @NotNull
    private static ItemStack buildAddButton() {
        return Lang.getIcon("ae-drive-whitelist-add", Material.NAME_TAG);
    }

    @NotNull
    private static ItemStack buildPlayerHead(@NotNull UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        String name = player.getName() == null ? uuid.toString() : player.getName();
        item.editMeta(meta -> {
            if (player.getName() != null && meta instanceof SkullMeta skull) {
                skull.setOwningPlayer(player);
            }
            meta.setDisplayName(name);
            meta.setLore(List.of(Lang.getString("messages.ae.drive.whitelist_remove_hint")));
        });
        return item;
    }
}