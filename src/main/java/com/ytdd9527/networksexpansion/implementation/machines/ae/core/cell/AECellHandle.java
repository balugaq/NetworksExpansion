package com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell;

import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemHashMap;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemKey;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AECellHandle {

    private final AEStorageCellCache cache;

    private AECellHandle(@NotNull AEStorageCellCache cache) {
        this.cache = cache;
    }

    @NotNull
    public static AECellHandle create(@NotNull ItemStack cellItem) {
        long perTypeLimit = AEStorageCell.getPerTypeLimit(cellItem);
        AEStorageCellCache cache = AEStorageCell.loadCellCache(cellItem, perTypeLimit);
        return new AECellHandle(cache);
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

    @NotNull
    public Map<ItemStack, Long> getAllItems() {
        return cache.getAllItems();
    }

    public void accumulateInto(@NotNull ItemHashMap<Long> target, @NotNull Map<ItemKey, List<UUID>> index) {
        cache.accumulateInto(target, index);
    }

    public long getStoredCount() {
        return cache.getStored();
    }
}
