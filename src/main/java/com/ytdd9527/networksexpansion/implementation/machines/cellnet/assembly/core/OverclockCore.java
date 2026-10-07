package com.ytdd9527.networksexpansion.implementation.machines.cellnet.assembly.core;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
public class OverclockCore extends GearCore {

    public OverclockCore(
            @NotNull ItemGroup itemGroup,
            @NotNull SlimefunItemStack item,
            @NotNull RecipeType recipeType,
            ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public int gearBonus() {
        return 1;
    }

    public static boolean isOverclockCore(@Nullable ItemStack itemStack) {
        return itemStack != null && !itemStack.getType().isAir()
            && SlimefunItem.getByItem(itemStack) instanceof OverclockCore;
    }
}
