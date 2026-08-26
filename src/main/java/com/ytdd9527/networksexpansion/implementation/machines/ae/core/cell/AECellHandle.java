package com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell;

import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemHashMap;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AECellHandle {

    private final int slotIndex;
    private final AEStorageCellCache cache;

    private AECellHandle(int slotIndex, @NotNull AEStorageCellCache cache) {
        this.slotIndex = slotIndex;
        this.cache = cache;
    }

    @NotNull
    public static AECellHandle create(int slotIndex, @NotNull ItemStack cellItem) {
        long perTypeLimit = AEStorageCell.getPerTypeLimit(cellItem);
        AEStorageCellCache cache = AEStorageCell.loadCellCache(cellItem, perTypeLimit);
        return new AECellHandle(slotIndex, cache);
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    @NotNull
    public UUID getUuid() {
        return cache.getUuid();
    }

    public long getAmount(@NotNull ItemStack sample) {
        return cache.getAmount(sample);
    }

    public long getAmount(@NotNull ItemKey key) {
        return cache.getAmount(key);
    }

    public boolean canAccept(@NotNull ItemStack sample) {
        return cache.canAccept(sample);
    }

    public boolean canAccept(@NotNull ItemKey key) {
        return cache.canAccept(key);
    }

    public boolean contains(@NotNull ItemStack sample) {
        return cache.contains(sample, 1);
    }

    public boolean contains(@NotNull ItemKey key) {
        return cache.contains(key, 1);
    }

    public int pushItem(@NotNull ItemStack incoming) {
        return cache.pushItem(incoming);
    }

    public int pushItem(@NotNull ItemKey key, int amount) {
        return cache.pushItem(key, amount);
    }

    @Nullable
    public ItemStack takeItem(@NotNull ItemStack sample, int amount) {
        return cache.takeItem(sample, amount);
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