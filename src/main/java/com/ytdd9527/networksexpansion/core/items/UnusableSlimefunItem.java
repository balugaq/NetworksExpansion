package com.ytdd9527.networksexpansion.core.items;

import com.ytdd9527.networksexpansion.implementation.ExpansionRecipes;
import com.ytdd9527.networksexpansion.utils.itemstacks.MachineUtil;
import io.github.sefiraat.networks.slimefun.NetworksItemGroups;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.EntityInteractHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ToolUseHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.WeaponUseHandler;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Final_ROOT
 * @since 2.0
 */
public class UnusableSlimefunItem extends SpecialSlimefunItem {
    public UnusableSlimefunItem(SlimefunItemStack item) {
        this(NetworksItemGroups.DISABLED_ITEMS, item, RecipeType.NULL, ExpansionRecipes.NULL);
    }
    public UnusableSlimefunItem(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        this(itemGroup, item, recipeType, recipe, item);
    }

    public UnusableSlimefunItem(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe,
        @Nullable ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);
        this.addItemHandler(MachineUtil.BLOCK_PLACE_HANDLER_DENY);
        this.addItemHandler((ItemUseHandler) PlayerRightClickEvent::cancel);
        this.addItemHandler((WeaponUseHandler) (e, player, item2) -> e.setCancelled(true));
        this.addItemHandler((EntityInteractHandler) (e, item1, offHand) -> e.setCancelled(true));
        this.addItemHandler((ToolUseHandler) (e, tool, fortune, drops) -> e.setCancelled(true));
    }
}
