package com.ytdd9527.networksexpansion.implementation.machines.cellnet.ui;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.GhostItems;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntConsumer;

public final class BrowseUI {

    public interface EntryClick {
        void onClick(@NotNull Player player, int slot, @NotNull ItemStack item, @NotNull ClickAction action);
    }

    public static final int[] LIST_SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    public static final int PREV = 0;
    public static final int NEXT = 8;
    public static final int SEARCH = 51;
    public static final int PAGE_SIZE = LIST_SLOTS.length;

    private BrowseUI() {
    }

    public static int totalPages(int entryCount) {
        return Math.max(1, (int) Math.ceil((double) entryCount / PAGE_SIZE));
    }

    public static void renderEntries(@NotNull ChestMenu menu,
                                     @NotNull List<Map.Entry<ItemStack, Long>> entries,
                                     int start,
                                     int end,
                                     @NotNull Function<Map.Entry<ItemStack, Long>, ItemStack> renderer) {
        renderEntries(menu, entries, start, end, renderer, null);
    }

    public static void renderEntries(@NotNull ChestMenu menu,
                                     @NotNull List<Map.Entry<ItemStack, Long>> entries,
                                     int start,
                                     int end,
                                     @NotNull Function<Map.Entry<ItemStack, Long>, ItemStack> renderer,
                                     @Nullable EntryClick onClick) {
        for (int i = 0; i < LIST_SLOTS.length; i++) {
            int slot = LIST_SLOTS[i];
            if (i < end - start) {
                menu.replaceExistingItem(slot, renderer.apply(entries.get(start + i)));
            } else if (entries.isEmpty() && i == 0) {
                menu.replaceExistingItem(slot, Icons.SEARCH_EMPTY);
            } else {
                menu.replaceExistingItem(slot, Icons.PREVIEW_FILL);
            }
            menu.addMenuClickHandler(slot, (p, s, it, a) -> {
                if (onClick != null && it != null && !it.getType().isAir()) {
                    onClick.onClick(p, s, it, a);
                }
                return false;
            });
        }
    }

    public static void wirePager(@NotNull ChestMenu menu, int page, int totalPages, @NotNull IntConsumer goTo) {
        menu.replaceExistingItem(PREV, pageButton(page > 0
            ? Lang.getString(CellnetText.CELL_MENU_PREV_PAGE)
            : Lang.getString(CellnetText.CELL_MENU_FIRST_PAGE)));
        menu.addMenuClickHandler(PREV, (p, s, i, a) -> {
            if (page > 0) {
                goTo.accept(page - 1);
            }
            return false;
        });

        menu.replaceExistingItem(NEXT, pageButton(page < totalPages - 1
            ? Lang.getString(CellnetText.CELL_MENU_NEXT_PAGE)
            : Lang.getString(CellnetText.CELL_MENU_LAST_PAGE)));
        menu.addMenuClickHandler(NEXT, (p, s, i, a) -> {
            if (page < totalPages - 1) {
                goTo.accept(page + 1);
            }
            return false;
        });
    }

    @NotNull
    public static ItemStack pageButton(@NotNull String name) {
        ItemStack item = Icons.PAGE_ARROW.clone();
        item.editMeta(meta -> meta.setDisplayName(name));
        GhostItems.mark(item);
        return item;
    }

    @NotNull
    public static ItemStack backButton() {
        return Icons.BACK;
    }
}
