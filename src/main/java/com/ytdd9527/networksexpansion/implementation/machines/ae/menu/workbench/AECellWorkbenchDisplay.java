package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.workbench;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.ae.blockentity.AECellWorkbench;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AEStorageCellCache;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.slot.AECellWorkbenchMenuSlots;
import com.ytdd9527.networksexpansion.implementation.machines.ae.utils.AENumberFormat;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public final class AECellWorkbenchDisplay {

    private AECellWorkbenchDisplay() {
    }

    public static void refresh(@NotNull BlockMenu menu, @NotNull AECellWorkbench workbench) {
        ItemStack cellItem = menu.getItemInSlot(AECellWorkbenchMenuSlots.CELL_SLOT);

        if (cellItem == null || !AEStorageCell.isStorageCell(cellItem)) {
            menu.replaceExistingItem(AECellWorkbenchMenuSlots.CELL_SLOT, AECellWorkbenchMenuHandler.createCellSlotMarker());
            clearDisplay(menu);
            menu.replaceExistingItem(AECellWorkbenchMenuSlots.INFO, buildInfoItem(null, 0));
            menu.replaceExistingItem(AECellWorkbenchMenuSlots.PREV, buildPageButton(false, false));
            menu.replaceExistingItem(AECellWorkbenchMenuSlots.NEXT, buildPageButton(false, true));
            menu.replaceExistingItem(AECellWorkbenchMenuSlots.CLOSE, buildCloseButton());
            return;
        }

        long per = AEStorageCell.getPerTypeLimit(cellItem);
        AEStorageCellCache cache = AEStorageCell.loadCellCache(cellItem, per);

        AEStorageCell.applyLore(cellItem, per, AEStorageCell.getCurrentPerTypeLimit(cellItem));
        menu.replaceExistingItem(AECellWorkbenchMenuSlots.CELL_SLOT, cellItem);

        List<AEStorageCellCache.CellEntry> entries = cache.getStoredItems();
        int page = workbench.getPageCache().getOrDefault(menu.getLocation(), 0);
        int maxPages = Math.max(1, (int) Math.ceil((double) entries.size() / AECellWorkbenchMenuSlots.ITEMS_PER_PAGE));
        if (page >= maxPages) {
            page = Math.max(0, maxPages - 1);
            workbench.getPageCache().put(menu.getLocation(), page);
        }

        int start = page * AECellWorkbenchMenuSlots.ITEMS_PER_PAGE;
        int end = Math.min(start + AECellWorkbenchMenuSlots.ITEMS_PER_PAGE, entries.size());

        for (int i = 0; i < AECellWorkbenchMenuSlots.DISPLAY_SLOTS.length; i++) {
            int slot = AECellWorkbenchMenuSlots.DISPLAY_SLOTS[i];
            if (i < end - start) {
                AEStorageCellCache.CellEntry entry = entries.get(start + i);
                menu.replaceExistingItem(slot, buildDisplayItem(entry.sample, entry.amount));
            } else {
                menu.replaceExistingItem(slot, buildDisplayPlaceholder());
            }
        }

        menu.replaceExistingItem(AECellWorkbenchMenuSlots.PREV, buildPageButton(page > 0, false));
        menu.replaceExistingItem(AECellWorkbenchMenuSlots.NEXT, buildPageButton(page < maxPages - 1, true));
        menu.replaceExistingItem(AECellWorkbenchMenuSlots.INFO, buildInfoItem(cellItem, entries.size()));
        menu.replaceExistingItem(AECellWorkbenchMenuSlots.CLOSE, buildCloseButton());
    }

    public static void clearDisplay(@NotNull BlockMenu menu) {
        for (int slot : AECellWorkbenchMenuSlots.DISPLAY_SLOTS) {
            menu.replaceExistingItem(slot, buildDisplayPlaceholder());
        }
    }

    public static int maxPages(@NotNull BlockMenu menu) {
        ItemStack cellItem = menu.getItemInSlot(AECellWorkbenchMenuSlots.CELL_SLOT);
        if (cellItem == null || !AEStorageCell.isStorageCell(cellItem)) {
            return 1;
        }
        long per = AEStorageCell.getPerTypeLimit(cellItem);
        AEStorageCellCache cache = AEStorageCell.loadCellCache(cellItem, per);
        int count = cache.getStoredItems().size();
        return Math.max(1, (int) Math.ceil((double) count / AECellWorkbenchMenuSlots.ITEMS_PER_PAGE));
    }

    @NotNull
    private static ItemStack buildDisplayItem(@NotNull ItemStack sample, long amount) {
        ItemStack display = new ItemStack(sample.getType());
        display.setAmount(1);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            String displayName = ItemStackHelper.getDisplayName(sample);
            if (displayName == null || displayName.isEmpty()) {
                displayName = MessageFormat.format("{0}", sample.getType().name());
            }
            meta.setDisplayName(displayName);
            List<String> lore = new ArrayList<>();
            lore.add(Lang.getString("messages.ae.workbench.item_count", AENumberFormat.formatNumber(amount)));
            lore.add("");
            lore.add(Lang.getString("messages.ae.workbench.delete_hint"));
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    @NotNull
    private static ItemStack buildInfoItem(@Nullable ItemStack cellItem, int types) {
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            if (cellItem != null && AEStorageCell.isStorageCell(cellItem)) {
                meta.setDisplayName(Lang.getString("messages.ae.workbench.info_title"));
                lore.add(Lang.getString("messages.ae.workbench.cell_name", ItemStackHelper.getDisplayName(cellItem)));
                lore.add(Lang.getString("messages.ae.workbench.item_types", types));
            } else {
                meta.setDisplayName(Lang.getString("messages.ae.workbench.info_title"));
                lore.add(Lang.getString("messages.ae.workbench.no_cell"));
            }
            meta.setLore(lore);
            info.setItemMeta(meta);
        }
        return info;
    }

    @NotNull
    private static ItemStack buildPageButton(boolean enabled, boolean next) {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            if (next) {
                meta.setDisplayName(enabled
                    ? Lang.getString("messages.ae.workbench.next_page")
                    : Lang.getString("messages.ae.workbench.last_page"));
            } else {
                meta.setDisplayName(enabled
                    ? Lang.getString("messages.ae.workbench.prev_page")
                    : Lang.getString("messages.ae.workbench.first_page"));
            }
            button.setItemMeta(meta);
        }
        return button;
    }

    @NotNull
    private static ItemStack buildCloseButton() {
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta meta = close.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Lang.getString("messages.ae.workbench.close"));
            close.setItemMeta(meta);
        }
        return close;
    }

    @NotNull
    private static ItemStack buildDisplayPlaceholder() {
        ItemStack placeholder = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            placeholder.setItemMeta(meta);
        }
        return placeholder;
    }
}