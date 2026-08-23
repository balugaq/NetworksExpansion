package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.slot;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AECellMenuSlots {

    public static final int[] MAIN_BACKGROUND = new int[]{1, 2, 3, 4, 5, 6, 7, 46, 47, 48, 49, 50, 51};
    public static final int[] LIST_SLOTS = new int[]{
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    public static final int PAGE_SIZE = LIST_SLOTS.length;
    public static final int PREV = 0;
    public static final int NEXT = 8;
    public static final int WHITELIST_BUTTON = 45;
    public static final int UPGRADE = 52;
    public static final int RENAME = 53;

    public static final int WHITELIST_TOGGLE = 4;
    public static final int WHITELIST_BACK = 49;
    public static final int[] WHITELIST_BACKGROUND = new int[]{1, 2, 3, 5, 6, 7, 45, 46, 47, 48, 50, 51, 52, 53};

    private static final Map<UUID, Integer> ITEM_PAGE = new HashMap<>();

    private AECellMenuSlots() {
    }

    public static int getItemPage(@NotNull UUID uuid) {
        return ITEM_PAGE.getOrDefault(uuid, 0);
    }

    public static void setItemPage(@NotNull UUID uuid, int page) {
        ITEM_PAGE.put(uuid, page);
    }

    public static void initPages(@NotNull UUID uuid) {
        ITEM_PAGE.put(uuid, 0);
    }

    public static void removePage(@NotNull UUID uuid) {
        ITEM_PAGE.remove(uuid);
    }

    public static int whitelistIndexForSlot(int slot) {
        for (int i = 0; i < LIST_SLOTS.length; i++) {
            if (LIST_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }
}