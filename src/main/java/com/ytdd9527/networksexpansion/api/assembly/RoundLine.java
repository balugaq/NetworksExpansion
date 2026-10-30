package com.ytdd9527.networksexpansion.api.assembly;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

record RoundLine(int index, @NotNull ItemStack blueprint, @NotNull Recipe recipe, @NotNull CraftLimit limit, long units,
                 long batch) {
}
