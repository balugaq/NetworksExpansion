package com.ytdd9527.networksexpansion.api.cell;

import com.ytdd9527.networksexpansion.utils.ItemKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class VoidCellHandle extends CellHandle {

    @NotNull
    private final List<ItemStack> filters;

    VoidCellHandle(
            @NotNull CellLedger cache,
            @NotNull List<ItemStack> filters) {
        super(cache);
        this.filters = List.copyOf(filters);
    }

    @Override
    public boolean canReceiveItem(@NotNull ItemKey key) {
        return false;
    }

    @Override
    public int pushItem(@NotNull ItemKey key, int amount) {
        return 0;
    }

    public boolean acceptsForDestroy(@Nullable ItemStack template) {
        return VoidCellSupport.matchesAnyFilter(filters, template);
    }
}
