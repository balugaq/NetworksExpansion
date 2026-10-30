package com.ytdd9527.networksexpansion.api.assembly;

import com.ytdd9527.networksexpansion.utils.ItemKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record Recipe(@NotNull ItemStack output, @NotNull List<Ingredient> ingredients, @NotNull ItemKey outputKey,
                     @NotNull ItemKey outputExactKey) {
}
