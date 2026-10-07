package com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell;

import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ItemHashMap;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ItemKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.ledger.CellLedger;

public class CellHandle implements Cell {

    private final CellLedger cache;

    protected CellHandle(@NotNull CellLedger cache) {
        this.cache = cache;
    }

    @NotNull
    public static CellHandle create(@NotNull ItemStack cellItem) {
        if (VoidCellSupport.isVoidCell(cellItem)) {
            VoidCellSupport.ensureInitialized(cellItem);
            long perTypeLimit = StorageCell.getPerTypeLimit(cellItem);
            CellLedger cache = StorageCell.loadCellCache(cellItem, perTypeLimit);
            cache.setVoidCell(true);
            return new VoidCellHandle(
                cache,
                VoidCellSupport.filtersView(cellItem));
        }
        long perTypeLimit = StorageCell.getPerTypeLimit(cellItem);
        CellLedger cache = StorageCell.loadCellCache(cellItem, perTypeLimit);
        return new CellHandle(cache);
    }

    @NotNull
    public static CellHandle wrap(@NotNull CellLedger existingCache) {
        return new CellHandle(existingCache);
    }

    @NotNull
    public UUID getUuid() {
        return cache.getUuid();
    }

    public long getAmount(@NotNull ItemKey key) {
        return cache.getAmount(key);
    }

    public boolean canReceiveItem(@NotNull ItemKey key) {
        return cache.canReceiveItem(key);
    }

    public long receiveCapacity(@NotNull ItemKey key) {
        return cache.receiveCapacity(key);
    }

    public boolean contains(@NotNull ItemKey key) {
        return cache.contains(key, 1);
    }

    public int pushItem(@NotNull ItemKey key, int amount) {
        return cache.pushItem(key, amount);
    }

    @Nullable
    public ItemStack takeItem(@NotNull ItemKey key, long amount) {
        return cache.takeItem(key, amount);
    }

    public long takeItemAmount(@NotNull ItemKey key, long amount) {
        return cache.takeItemAmount(key, amount);
    }

    @NotNull
    public Map<ItemStack, Long> getAllItems() {
        return cache.getAllItems();
    }

    public void accumulateInto(@NotNull ItemHashMap<Long> target, @Nullable Map<ItemKey, List<UUID>> index) {
        cache.accumulateInto(target, index);
    }

    public long getStoredCount() {
        return cache.getStored();
    }
}
