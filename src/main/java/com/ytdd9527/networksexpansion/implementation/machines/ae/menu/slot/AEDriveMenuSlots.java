package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.slot;

public final class AEDriveMenuSlots {

    public static final int[] CELL_SLOTS = new int[]{10, 11, 12, 19, 20, 21, 28, 29, 30};
    public static final int CELL_SLOT_COUNT = CELL_SLOTS.length;
    public static final int DISPLAY_SLOT = 4;
    public static final int ACCESS_SLOT = 13;

    public static final int[] MAIN_BACKGROUND_SLOTS = new int[]{
            0, 1, 2, 3, 5, 6, 7, 8, 9, 14, 15, 16, 17, 18, 22, 23, 24, 25, 26, 27, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    public static final int[] ITEM_DISPLAY_SLOTS = new int[]{
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44,
            45, 46, 47, 48, 49, 50, 51, 52, 53
    };
    public static final int ITEM_PAGE_PREVIOUS = 0;
    public static final int ITEM_PAGE_NEXT = 8;
    public static final int ITEM_RETURN = 4;
    public static final int ITEMS_PER_PAGE = ITEM_DISPLAY_SLOTS.length;
    public static final int[] ITEM_ACCESS_BACKGROUND_SLOTS = new int[]{
            1, 2, 3, 5, 6, 7
    };

    private AEDriveMenuSlots() {
    }
}