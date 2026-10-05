package com.ytdd9527.networksexpansion.implementation.machines.cellnet.ui;

import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.CellHandle;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.ledger.CellLedger;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.ledger.CellPersistence;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive.CellDrive;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ItemKey;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.Cell;

public final class CellMenuCommon {

    public static final int OFFHAND_SLOT = 40;

    private CellMenuCommon() {
    }

    public interface WriteBack {
        void write(@NotNull Player player, int slot, @NotNull ItemStack cell);
    }

    @Nullable
    public static ItemStack findCellInPlayer(@NotNull Player player, @NotNull UUID uuid) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (matchesCell(main, uuid)) {
            return main;
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (matchesCell(off, uuid)) {
            return off;
        }
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (matchesCell(item, uuid)) {
                return item;
            }
        }
        return null;
    }

    public static int indexOfCellInPlayer(@NotNull Player player, @NotNull UUID cellUuid) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (matchesCell(inventory.getItem(i), cellUuid)) {
                return i;
            }
        }
        if (matchesCell(inventory.getItemInOffHand(), cellUuid)) {
            return OFFHAND_SLOT;
        }
        return -1;
    }

    public static boolean matchesCell(@Nullable ItemStack item, @NotNull UUID cellUuid) {
        return cellUuid.equals(CellPersistence.getCellUUID(item));
    }

    public static void writeBackToHand(@NotNull Player player, int slot, @NotNull ItemStack cell) {
        if (slot == OFFHAND_SLOT) {
            player.getInventory().setItemInOffHand(cell);
        } else {
            player.getInventory().setItem(slot, cell);
        }
    }

    public static boolean writeBackLocated(@NotNull Player player, @NotNull ItemStack cell) {
        UUID cellUuid = CellPersistence.getCellUUID(cell);
        if (cellUuid == null) {
            return false;
        }
        int slot = indexOfCellInPlayer(player, cellUuid);
        if (slot < 0) {
            return false;
        }
        writeBackToHand(player, slot, cell);
        player.updateInventory();
        return true;
    }

    public static boolean refreshAndWriteBack(
            @NotNull Player player,
            @NotNull UUID cellUuid,
            @NotNull Consumer<ItemStack> refresh,
            @NotNull WriteBack writeBack) {
        int slot = indexOfCellInPlayer(player, cellUuid);
        if (slot < 0) {
            return false;
        }
        ItemStack live = slot == OFFHAND_SLOT
            ? player.getInventory().getItemInOffHand()
            : player.getInventory().getItem(slot);
        if (live == null) {
            return false;
        }
        refresh.accept(live);
        writeBack.write(player, slot, live);
        player.updateInventory();
        return true;
    }

    public static int indexOfCellInDrive(@NotNull BlockMenu driveMenu, @NotNull UUID cellUuid, int preferredSlot) {
        if (preferredSlot >= 0 && preferredSlot < driveMenu.getSize()
            && matchesCell(driveMenu.getItemInSlot(preferredSlot), cellUuid)) {
            return preferredSlot;
        }
        for (int slot : CellDrive.CELL_SLOTS) {
            if (slot != preferredSlot && matchesCell(driveMenu.getItemInSlot(slot), cellUuid)) {
                return slot;
            }
        }
        return -1;
    }

    public static boolean refreshDriveCell(@NotNull BlockMenu driveMenu, int slot, @NotNull Consumer<ItemStack> refresh) {
        ItemStack live = driveMenu.getItemInSlot(slot);
        if (live == null) {
            return false;
        }
        ItemStack updated = live.clone();
        refresh.accept(updated);
        driveMenu.replaceExistingItem(slot, updated);
        return true;
    }

    @Nullable
    public static CellHandle findCellHolding(@Nullable List<CellHandle> cells, @NotNull ItemKey key) {
        if (cells == null) {
            return null;
        }
        for (CellHandle cell : cells) {
            if (cell.getAmount(key) > 0) {
                return cell;
            }
        }
        return null;
    }

    public static boolean toggleEntry(
            @NotNull Player player,
            @Nullable BlockMenu driveMenu,
            @NotNull UUID cellUuid,
            @NotNull ItemKey key,
            boolean voidExcess) {
        CellLedger cache = CellLedger.getActiveCaches().get(cellUuid);
        if (cache == null) {
            return false;
        }
        if (voidExcess) {
            cache.setVoidExcessUnit(key, !cache.isVoidExcessUnit(key));
        } else {
            cache.setReserved(key, !cache.isReserved(key));
        }
        persistEntryFlags(player, driveMenu, cellUuid, voidExcess);
        return true;
    }

    @NotNull
    public static ItemStack browseEntry(@Nullable List<CellHandle> cells, @NotNull ItemStack sample, long amount) {
        CellHandle owner = findCellHolding(cells, new ItemKey(sample));
        CellLedger ledger = owner == null ? null : CellLedger.getActiveCaches().get(owner.getUuid());
        return ledger != null ? CellUi.displayItem(ledger, sample, amount) : CellUi.browseEntry(sample, amount);
    }

    private static void persistEntryFlags(
            @NotNull Player player,
            @Nullable BlockMenu driveMenu,
            @NotNull UUID cellUuid,
            boolean voidExcess) {
        Consumer<ItemStack> refresh = live -> {
            if (voidExcess) {
                CellPersistence.saveVoidExcessItems(live);
            } else {
                CellPersistence.saveReservedItems(live);
            }
        };
        if (refreshAndWriteBack(player, cellUuid, refresh, CellMenuCommon::writeBackToHand)) {
            return;
        }
        if (driveMenu == null) {
            return;
        }
        int slot = indexOfCellInDrive(driveMenu, cellUuid, -1);
        if (slot >= 0) {
            refreshDriveCell(driveMenu, slot, refresh);
        }
    }
}
