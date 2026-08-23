package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.drive;

import com.balugaq.netex.utils.Lang;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.implementation.machines.ae.blockentity.AEDrive;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive.AEDriveCellManager;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive.AEDriveStorage;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive.AECellUniquenessManager;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component.AECellMenuButtons;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.slot.AEDriveMenuSlots;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AEDriveMenuHandler {

    private AEDriveMenuHandler() {
    }

    public static void setupMainMenuHandlers(@NotNull BlockMenu menu, @NotNull Block block,
                                             @NotNull AEDrive drive, @NotNull AEDriveStorage storage) {
        menu.addMenuClickHandler(AEDriveMenuSlots.DISPLAY_SLOT, (p, s, i, a) -> false);

        menu.addMenuClickHandler(AEDriveMenuSlots.ACCESS_SLOT, (player, clickedSlot, item, action) -> {
            openItemAccessMenu(drive, menu, block, player);
            return false;
        });

        menu.addMenuCloseHandler(p -> {
            storage.invalidateCellCache(menu.getLocation());
            AECellUniquenessManager.registerDrive(menu);
        });
        menu.addMenuOpeningHandler(player -> {
            for (int slot : AEDriveMenuSlots.CELL_SLOTS) {
                AEDriveCellManager.refreshCellLore(menu, slot);
            }
            AECellUniquenessManager.scanAndEjectDuplicates(menu, player);
        });
    }

    public static void openItemAccessMenu(@NotNull AEDrive drive, @NotNull BlockMenu driveMenu,
                                          @NotNull Block block, @NotNull Player player) {
        Location location = block.getLocation();
        drive.getItemAccessPageCache().put(location, 0);

        ChestMenu menu = new ChestMenu(Lang.getString("messages.ae.drive.preview_title"));
        menu.setPlayerInventoryClickable(true);

        for (int slot : AEDriveMenuSlots.ITEM_ACCESS_BACKGROUND_SLOTS) {
            menu.addItem(slot, AECellMenuButtons.border(), (p, s, i, a) -> false);
        }

        renderBrowser(menu, drive, driveMenu, location);
        menu.setEmptySlotsClickable(false);
        menu.open(player);
    }

    private static void renderBrowser(@NotNull ChestMenu menu, @NotNull AEDrive drive,
                                      @NotNull BlockMenu driveMenu, @NotNull Location location) {
        Map<ItemStack, Long> allItems = drive.getAllCellItems(driveMenu);
        List<Map.Entry<ItemStack, Long>> itemList = new ArrayList<>(allItems.entrySet());

        Map<Location, Integer> pageCache = drive.getItemAccessPageCache();
        int page = pageCache.getOrDefault(location, 0);
        int totalPages = Math.max(1, (int) Math.ceil((double) itemList.size() / AEDriveMenuSlots.ITEMS_PER_PAGE));
        if (page >= totalPages) {
            page = totalPages - 1;
            pageCache.put(location, page);
        }

        int start = page * AEDriveMenuSlots.ITEMS_PER_PAGE;
        int end = Math.min(start + AEDriveMenuSlots.ITEMS_PER_PAGE, itemList.size());

        for (int i = 0; i < AEDriveMenuSlots.ITEM_DISPLAY_SLOTS.length; i++) {
            int slot = AEDriveMenuSlots.ITEM_DISPLAY_SLOTS[i];
            if (i < end - start) {
                Map.Entry<ItemStack, Long> entry = itemList.get(start + i);
                menu.addItem(slot, AECellMenuButtons.displayItem(entry.getKey(), entry.getValue()));
            } else {
                menu.addItem(slot, new ItemStack(Material.AIR));
            }
            menu.addMenuClickHandler(slot, (p, s, it, a) -> false);
        }

        menu.addItem(AEDriveMenuSlots.ITEM_PAGE_PREVIOUS,
                AECellMenuButtons.pageButton(page > 0 ? Lang.getString("messages.ae.drive.prev_page") : Lang.getString("messages.ae.drive.first_page")));
        menu.addMenuClickHandler(AEDriveMenuSlots.ITEM_PAGE_PREVIOUS, (p, s, i, a) -> {
            int current = pageCache.getOrDefault(location, 0);
            if (current > 0) {
                pageCache.put(location, current - 1);
                renderBrowser(menu, drive, driveMenu, location);
            }
            return false;
        });

        menu.addItem(AEDriveMenuSlots.ITEM_PAGE_NEXT,
                AECellMenuButtons.pageButton(page < totalPages - 1 ? Lang.getString("messages.ae.drive.next_page") : Lang.getString("messages.ae.drive.last_page")));
        menu.addMenuClickHandler(AEDriveMenuSlots.ITEM_PAGE_NEXT, (p, s, i, a) -> {
            int current = pageCache.getOrDefault(location, 0);
            if (current < totalPages - 1) {
                pageCache.put(location, current + 1);
                renderBrowser(menu, drive, driveMenu, location);
            }
            return false;
        });

        menu.addItem(AEDriveMenuSlots.ITEM_RETURN, AECellMenuButtons.backButton());
        menu.addMenuClickHandler(AEDriveMenuSlots.ITEM_RETURN, (p, s, i, a) -> {
            BlockMenu dm = StorageCacheUtils.getMenu(location);
            if (dm != null) {
                dm.open(p);
            }
            return false;
        });
    }
}