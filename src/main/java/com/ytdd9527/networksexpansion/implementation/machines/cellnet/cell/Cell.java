package com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell;

import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ItemHashMap;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ItemKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface Cell {

    @NotNull
    UUID getUuid();

    long getAmount(@NotNull ItemKey key);

    boolean canReceiveItem(@NotNull ItemKey key);

    long receiveCapacity(@NotNull ItemKey key);

    boolean contains(@NotNull ItemKey key);

    int pushItem(@NotNull ItemKey key, int amount);

    @Nullable
    ItemStack takeItem(@NotNull ItemKey key, long amount);

    long takeItemAmount(@NotNull ItemKey key, long amount);

    @NotNull
    Map<ItemStack, Long> getAllItems();

    void accumulateInto(@NotNull ItemHashMap<Long> target, @Nullable Map<ItemKey, List<UUID>> index);

    long getStoredCount();
}
