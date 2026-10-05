package com.ytdd9527.networksexpansion.implementation.machines.cellnet.ui;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.StorageCell;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.ledger.CellPersistence;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.GhostItems;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.ui.Icons;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.NumberFormat;
import com.ytdd9527.networksexpansion.utils.itemstacks.ItemStackUtil;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import net.guizhanss.minecraft.guizhanlib.gugu.minecraft.helpers.inventory.ItemStackHelper;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.Cell;

public final class CellSlotUi {

    private CellSlotUi() {
    }

    public static boolean handleCellSlotClick(
            @NotNull BlockMenu menu,
            int slot,
            @Nullable ItemStack slotItem,
            @NotNull Player player,
            @NotNull Predicate<ItemStack> markerTest,
            @NotNull BiConsumer<BlockMenu, ItemStack> insertCell,
            @NotNull BiFunction<BlockMenu, Integer, ItemStack> ejectCell) {
        ItemStack cursor = player.getItemOnCursor();
        boolean slotIsMarker = markerTest.test(slotItem);
        boolean cursorIsCell = cursor != null && StorageCell.isStorageCell(cursor);
        boolean cursorIsEmpty = cursor == null || cursor.getType().isAir();

        if (slotIsMarker) {
            if (cursorIsCell) {
                ItemStack placed = placeOneFromCursor(player, menu, slot, cursor);
                insertCell.accept(menu, placed);
            }
            return false;
        }
        if (cursorIsEmpty) {
            ItemStack ejected = ejectCell.apply(menu, slot);
            if (ejected != null) {
                player.setItemOnCursor(ejected);
            }
            return false;
        }
        if (cursorIsCell) {
            ItemStack ejected = ejectCell.apply(menu, slot);
            ItemStack placed = placeOneFromCursor(player, menu, slot, cursor);
            insertCell.accept(menu, placed);
            if (ejected != null) {
                ItemStackUtil.giveOrDropOnCursor(player, ejected);
            }
        }
        return false;
    }

    @NotNull
    public static ItemStack placeOneFromCursor(
            @NotNull Player player, @NotNull BlockMenu menu, int slot, @NotNull ItemStack cursor) {
        ItemStack placed = cursor.asQuantity(1);
        menu.replaceExistingItem(slot, placed);
        player.setItemOnCursor(cursor.getAmount() <= 1 ? null : cursor.asQuantity(cursor.getAmount() - 1));
        return placed;
    }

    @Nullable
    public static ItemStack ejectCell(
            @NotNull BlockMenu menu,
            int slot,
            @NotNull Supplier<ItemStack> markerSupplier,
            int @NotNull [] displaySlots,
            @NotNull Map<Location, Integer> pageCache,
            @NotNull Runnable refresher) {
        ItemStack cellItem = menu.getItemInSlot(slot);
        if (cellItem == null || !StorageCell.isStorageCell(cellItem)) {
            return null;
        }
        long per = StorageCell.getPerTypeLimit(cellItem);
        StorageCell.loadCellCache(cellItem, per);
        StorageCell.applyLore(cellItem, per, StorageCell.getCurrentPerTypeLimit(cellItem));
        menu.replaceExistingItem(slot, markerSupplier.get());
        clearDisplay(menu, displaySlots);
        pageCache.put(menu.getLocation(), 0);
        refresher.run();
        return cellItem;
    }

    public static boolean prepareInsertedCell(@NotNull Player player, @NotNull ItemStack cellItem) {
        if (CellPersistence.isWrongServer(player, cellItem)) {
            player.sendMessage(Lang.getString(CellnetText.CELL_WRONG_SERVER));
            return false;
        }
        if (!StorageCell.isStorageCell(cellItem)) {
            return false;
        }
        long perTypeLimit = StorageCell.getPerTypeLimit(cellItem);
        if (perTypeLimit <= 0) {
            SlimefunItem sfItem = SlimefunItem.getByItem(cellItem);
            if (sfItem instanceof StorageCell aeCell) {
                perTypeLimit = aeCell.getPerTypeLimit();
                StorageCell.initializeCell(cellItem, perTypeLimit);
            }
        }
        StorageCell.loadCellCache(cellItem, perTypeLimit);
        return true;
    }

    public static void clearDisplay(@NotNull BlockMenu menu, int @NotNull [] displaySlots) {
        for (int slot : displaySlots) {
            menu.replaceExistingItem(slot, Icons.CLEANER_DISPLAY);
        }
    }

    @NotNull
    public static ItemStack pageButton(
            boolean enabled,
            boolean next,
            @NotNull String prevKey,
            @NotNull String firstKey,
            @NotNull String nextKey,
            @NotNull String lastKey) {
        ItemStack button = Icons.PAGE_ARROW.clone();
        String key = next
                ? (enabled ? nextKey : lastKey)
                : (enabled ? prevKey : firstKey);
        button.editMeta(meta -> meta.setDisplayName(Lang.getString(key)));
        return button;
    }

    @NotNull
    public static ItemStack displayItem(
            @NotNull ItemStack sample, long amount, @NotNull String countKey, @NotNull String hintKey) {
        ItemStack display = sample.clone();
        display.setAmount(1);
        display.editMeta(meta -> {
            String displayName = ItemStackHelper.getDisplayName(sample);
            meta.setDisplayName(displayName == null || displayName.isEmpty() ? sample.getType().name() : displayName);
            meta.setLore(List.of(
                    Lang.getString(countKey, NumberFormat.formatNumber(amount)),
                    "",
                    Lang.getString(hintKey)));
        });
        GhostItems.mark(display);
        return display;
    }

    public static int normalizedPage(
            @NotNull Map<Location, Integer> pageCache, @NotNull Location location, int total, int perPage) {
        int page = pageCache.getOrDefault(location, 0);
        int max = Math.max(1, (int) Math.ceil((double) total / perPage));
        if (page >= max) {
            page = Math.max(0, max - 1);
            pageCache.put(location, page);
        }
        return page;
    }

    public static int pageIndex(int @NotNull [] displaySlots, int displaySlot, int page, int perPage) {
        for (int i = 0; i < displaySlots.length; i++) {
            if (displaySlots[i] == displaySlot) {
                return page * perPage + i;
            }
        }
        return -1;
    }

    public static int indexOf(int @NotNull [] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
    }

    public static int countOccupied(@NotNull BlockMenu menu, int @NotNull [] slots) {
        int used = 0;
        for (int slot : slots) {
            ItemStack onSlot = menu.getItemInSlot(slot);
            if (onSlot != null && !onSlot.getType().isAir()) {
                used++;
            }
        }
        return used;
    }

    public static void wirePageButtons(
            @NotNull BlockMenu menu,
            int prevSlot,
            int nextSlot,
            @NotNull Map<Location, Integer> pageCache,
            @NotNull ToIntFunction<BlockMenu> maxPages,
            @NotNull Consumer<BlockMenu> refresher) {
        menu.addMenuClickHandler(prevSlot, (player, slot, item, action) -> {
            int page = pageCache.getOrDefault(menu.getLocation(), 0);
            if (page > 0) {
                pageCache.put(menu.getLocation(), page - 1);
                refresher.accept(menu);
            }
            return false;
        });
        menu.addMenuClickHandler(nextSlot, (player, slot, item, action) -> {
            int page = pageCache.getOrDefault(menu.getLocation(), 0);
            if (page < maxPages.applyAsInt(menu) - 1) {
                pageCache.put(menu.getLocation(), page + 1);
                refresher.accept(menu);
            }
            return false;
        });
    }
}
