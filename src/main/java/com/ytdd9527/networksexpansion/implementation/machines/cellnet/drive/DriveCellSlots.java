package com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive;

import com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive.CellDrive;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.CellHandle;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.VoidCellSupport;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.StorageCell;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.VoidCell;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
public final class DriveCellSlots {

    private DriveCellSlots() {
    }

    public static boolean isStorageCell(@NotNull ItemStack itemStack) {
        return StorageCell.isStorageCell(itemStack) || VoidCellSupport.isVoidCell(itemStack);
    }

    @NotNull
    public static List<CellHandle> collectCells(@NotNull BlockMenu menu) {
        List<CellHandle> cells = new ArrayList<>();
        for (int i = 0; i < CellDrive.CELL_SLOTS.length; i++) {
            ItemStack cellItem = menu.getItemInSlot(CellDrive.CELL_SLOTS[i]);
            if (cellItem == null || !isStorageCell(cellItem)) {
                continue;
            }
            cells.add(CellHandle.create(cellItem));
        }
        return cells;
    }

    public static void refreshCellLore(@NotNull BlockMenu menu, int slot) {
        ItemStack cellItem = menu.getItemInSlot(slot);
        if (cellItem == null || !isStorageCell(cellItem)) {
            return;
        }
        if (VoidCellSupport.isVoidCell(cellItem)) {
            refreshVoidCellLore(menu, slot, cellItem);
            return;
        }
        long per = StorageCell.getPerTypeLimit(cellItem);
        long current = StorageCell.getCurrentPerTypeLimit(cellItem);
        StorageCell.applyLore(cellItem, per, current);
        menu.replaceExistingItem(slot, cellItem);
    }

    private static void refreshVoidCellLore(@NotNull BlockMenu menu, int slot, @NotNull ItemStack cellItem) {
        SlimefunItem sf = SlimefunItem.getByItem(cellItem);
        if (sf instanceof VoidCell voidCell) {
            voidCell.renderLore(cellItem);
            menu.replaceExistingItem(slot, cellItem);
        }
    }
}
