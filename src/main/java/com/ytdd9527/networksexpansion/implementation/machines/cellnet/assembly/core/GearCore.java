package com.ytdd9527.networksexpansion.implementation.machines.cellnet.assembly.core;

import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
public abstract class GearCore extends SpecialSlimefunItem implements NotPlaceable {

    protected GearCore(
            @NotNull ItemGroup itemGroup,
            @NotNull SlimefunItemStack item,
            @NotNull RecipeType recipeType,
            ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    public abstract int gearBonus();

    public static boolean isUpgradeCore(@Nullable ItemStack itemStack) {
        return itemStack != null && !itemStack.getType().isAir()
            && SlimefunItem.getByItem(itemStack) instanceof GearCore;
    }
}
