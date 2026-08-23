package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.slot;

public final class AECellWorkbenchMenuSlots {

    public static final int CELL_SLOT = 4;
    public static final int PREV = 0;
    public static final int NEXT = 8;
    public static final int INFO = 45;
    public static final int CLOSE = 53;

    public static final int[] DISPLAY_SLOTS = new int[]{
        9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    public static final int ITEMS_PER_PAGE = DISPLAY_SLOTS.length;

    public static final int[] BACKGROUND_SLOTS = new int[]{
        1, 2, 3, 5, 6, 7,
        46, 47, 48, 49, 50, 51, 52
    };

    private AECellWorkbenchMenuSlots() {
    }
}