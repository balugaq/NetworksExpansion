package com.ytdd9527.networksexpansion.api.assembly;

import com.ytdd9527.networksexpansion.utils.ItemKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

record RoundFetch(long @NotNull [] feasible, @NotNull Map<ItemKey, Long> outputs,
                  @NotNull Map<ItemKey, ItemStack> outputTemplates) {
}
