package com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive;

import com.ytdd9527.networksexpansion.implementation.machines.ae.blockentity.AEDrive;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellHandle;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class AEDriveCellManager {

    private AEDriveCellManager() {
    }

    public static boolean isStorageCell(@NotNull ItemStack itemStack) {
        return AEStorageCell.isStorageCell(itemStack);
    }

    @NotNull
    public static List<AECellHandle> collectCells(@NotNull BlockMenu menu) {
        List<AECellHandle> cells = new ArrayList<>();
        for (int i = 0; i < AEDrive.CELL_SLOTS.length; i++) {
            ItemStack cellItem = menu.getItemInSlot(AEDrive.CELL_SLOTS[i]);
            if (cellItem == null || !isStorageCell(cellItem)) {
                continue;
            }
            cells.add(AECellHandle.create(cellItem));
        }
        return cells;
    }

    public static void refreshCellLore(@NotNull BlockMenu menu, int slot) {
        ItemStack cellItem = menu.getItemInSlot(slot);
        if (cellItem == null || !isStorageCell(cellItem)) {
            return;
        }
        long per = AEStorageCell.getPerTypeLimit(cellItem);
        long current = AEStorageCell.getCurrentPerTypeLimit(cellItem);
        AEStorageCell.applyLore(cellItem, per, current);
        menu.replaceExistingItem(slot, cellItem);
    }
}
