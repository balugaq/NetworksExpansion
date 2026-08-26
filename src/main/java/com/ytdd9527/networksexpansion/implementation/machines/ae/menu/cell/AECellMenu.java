package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.cell;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AEStorageCellCache;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component.AECellMenuPlayerState;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.slot.AECellMenuSlots;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component.AECellMenuButtons;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component.AECellMenuUpgrade;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component.AECellMenuWhitelist;

public final class AECellMenu {

    private static final Map<UUID, UUID> OPENING_CELL = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> RENAMING = new ConcurrentHashMap<>();

    private AECellMenu() {
    }

    public static void markOpening(@NotNull UUID playerUuid, @NotNull UUID cellUuid) {
        OPENING_CELL.put(playerUuid, cellUuid);
    }

    public static void stopOpening(@NotNull UUID playerUuid) {
        UUID cellUuid = OPENING_CELL.remove(playerUuid);
        if (cellUuid != null) {
            AEStorageCellCache cache = AEStorageCellCache.getActiveCaches().get(cellUuid);
            if (cache != null) {
                refreshHeldCell(playerUuid, cellUuid, cache);
            }
            AECellMenuSlots.removePage(cellUuid);
        }
    }

    private static void refreshHeldCell(@NotNull UUID playerUuid, @NotNull UUID cellUuid, @NotNull AEStorageCellCache cache) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            return;
        }
        ItemStack cell = AECellMenuPlayerState.findCellInPlayer(player, cellUuid);
        if (cell == null) {
            return;
        }
        AEStorageCell.applyLore(cell, cache.getPerTypeLimit(), cache.getCurrentPerTypeLimit());
        AECellMenuPlayerState.updateHeldCell(player, cell);
    }

    public static boolean isOpening(@NotNull UUID playerUuid) {
        return OPENING_CELL.containsKey(playerUuid);
    }

    public static boolean isRenaming(@NotNull UUID playerUuid) {
        return RENAMING.containsKey(playerUuid);
    }

    @Nullable
    public static UUID getRenamingCell(@NotNull UUID playerUuid) {
        return RENAMING.get(playerUuid);
    }

    public static void stopRenaming(@NotNull UUID playerUuid) {
        RENAMING.remove(playerUuid);
    }

    public static void open(@NotNull Player player, @NotNull ItemStack cellItem) {
        UUID uuid = AEStorageCell.getOrCreateCellUUID(cellItem);
        AECellMenuSlots.initPages(uuid);
        openView(player, uuid);
    }

    private static void openView(@NotNull Player player, @NotNull UUID uuid) {
        AEStorageCellCache cache = cache(uuid);
        if (cache == null) {
            return;
        }

        ChestMenu menu = new ChestMenu(Lang.getString("messages.ae.cell.menu.title_view"));
        menu.setPlayerInventoryClickable(true);

        for (int slot : AECellMenuSlots.MAIN_BACKGROUND) {
            menu.addItem(slot, AECellMenuButtons.border(), (p, s, i, a) -> false);
        }

        menu.addItem(AECellMenuSlots.WHITELIST_BUTTON, AECellMenuButtons.whitelistButton(cache), (p, s, i, a) -> {
            openWhitelist(p, uuid);
            return false;
        });

        menu.addItem(AECellMenuSlots.UPGRADE, AECellMenuButtons.upgradeButton(cache), (p, s, i, a) -> {
            AECellMenuUpgrade.handleClick(menu, p, uuid);
            menu.replaceExistingItem(AECellMenuSlots.WHITELIST_BUTTON, AECellMenuButtons.whitelistButton(cache));
            return false;
        });

        menu.addItem(AECellMenuSlots.RENAME, AECellMenuButtons.renameButton(cache), (p, s, i, a) -> {
            p.closeInventory();
            RENAMING.put(p.getUniqueId(), uuid);
            p.sendMessage(Lang.getString("messages.ae.cell.rename_prompt"));
            return false;
        });

        menu.setEmptySlotsClickable(false);

        renderItems(menu, uuid);
        markOpening(player.getUniqueId(), uuid);
        menu.open(player);
    }

    private static void openWhitelist(@NotNull Player player, @NotNull UUID uuid) {
        AEStorageCellCache cache = cache(uuid);
        if (cache == null) {
            return;
        }

        ChestMenu menu = new ChestMenu(Lang.getString("messages.ae.cell.menu.title_whitelist"));
        menu.setPlayerInventoryClickable(true);

        for (int slot : AECellMenuSlots.WHITELIST_BACKGROUND) {
            menu.addItem(slot, AECellMenuButtons.border(), (p, s, i, a) -> false);
        }

        menu.addItem(AECellMenuSlots.WHITELIST_TOGGLE, AECellMenuButtons.toggleButton(cache), (p, s, i, a) -> {
            boolean enabled = !cache.isWhitelistEnabled();
            AECellMenuWhitelist.apply(p, uuid, enabled, cache.getWhitelist());
            menu.replaceExistingItem(AECellMenuSlots.WHITELIST_TOGGLE, AECellMenuButtons.toggleButton(cache));
            AECellMenuWhitelist.render(menu, uuid);
            return false;
        });

        menu.addItem(AECellMenuSlots.WHITELIST_BACK, AECellMenuButtons.backButton(), (p, s, i, a) -> {
            openView(p, uuid);
            return false;
        });

        for (int slot : AECellMenuSlots.LIST_SLOTS) {
            menu.addMenuClickHandler(slot, (p, s, i, a) -> {
                AECellMenuWhitelist.handleSlotClick(menu, p, slot, uuid);
                return false;
            });
        }

        menu.setEmptySlotsClickable(false);

        AECellMenuWhitelist.render(menu, uuid);
        menu.open(player);
    }

    private static void renderItems(@NotNull ChestMenu menu, @NotNull UUID uuid) {
        AEStorageCellCache cache = cache(uuid);
        if (cache == null) {
            return;
        }

        List<Map.Entry<ItemStack, Long>> items = new ArrayList<>(cache.getAllItems().entrySet());
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / AECellMenuSlots.PAGE_SIZE));
        int page = Math.min(AECellMenuSlots.getItemPage(uuid), totalPages - 1);
        AECellMenuSlots.setItemPage(uuid, page);

        int start = page * AECellMenuSlots.PAGE_SIZE;
        int end = Math.min(start + AECellMenuSlots.PAGE_SIZE, items.size());

        for (int i = 0; i < AECellMenuSlots.LIST_SLOTS.length; i++) {
            int slot = AECellMenuSlots.LIST_SLOTS[i];
            if (i < end - start) {
                Map.Entry<ItemStack, Long> entry = items.get(start + i);
                menu.replaceExistingItem(slot, AECellMenuButtons.displayItem(entry.getKey(), entry.getValue()));
            } else {
                menu.replaceExistingItem(slot, new ItemStack(Material.AIR));
            }
            menu.addMenuClickHandler(slot, (p, s, it, a) -> false);
        }

        menu.replaceExistingItem(AECellMenuSlots.PREV, AECellMenuButtons.pageButton(page > 0 ? Lang.getString("messages.ae.cell.menu.prev_page") : Lang.getString("messages.ae.cell.menu.first_page")));
        menu.addMenuClickHandler(AECellMenuSlots.PREV, (p, s, i, a) -> {
            int cur = AECellMenuSlots.getItemPage(uuid);
            if (cur > 0) {
                AECellMenuSlots.setItemPage(uuid, cur - 1);
                renderItems(menu, uuid);
            }
            return false;
        });

        menu.replaceExistingItem(AECellMenuSlots.NEXT, AECellMenuButtons.pageButton(page < totalPages - 1 ? Lang.getString("messages.ae.cell.menu.next_page") : Lang.getString("messages.ae.cell.menu.last_page")));
        menu.addMenuClickHandler(AECellMenuSlots.NEXT, (p, s, i, a) -> {
            if (page < totalPages - 1) {
                AECellMenuSlots.setItemPage(uuid, page + 1);
                renderItems(menu, uuid);
            }
            return false;
        });
    }

    @Nullable
    private static AEStorageCellCache cache(@NotNull UUID uuid) {
        return AEStorageCellCache.getActiveCaches().get(uuid);
    }
}