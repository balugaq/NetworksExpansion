package com.ytdd9527.networksexpansion.api.assembly;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class RowDisplay {
    public @Nullable Recipe recipe;
    public @Nullable Object snapshotRef;
    public long mode;
    public long target;
    public long remaining;
    public long keep;
    public long crafted;
    public int gear;
    public boolean blocked;
    public boolean empty;
    public @Nullable ItemStack icon;
}
