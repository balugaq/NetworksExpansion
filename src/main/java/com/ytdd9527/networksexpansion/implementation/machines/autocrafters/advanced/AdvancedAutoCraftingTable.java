package com.ytdd9527.networksexpansion.implementation.machines.autocrafters.advanced;

import com.balugaq.netex.api.helpers.SupportedCraftingTableRecipes;
import com.ytdd9527.networksexpansion.core.items.machines.AbstractAdvancedAutoCrafter;
import com.ytdd9527.networksexpansion.implementation.blueprints.CraftingBlueprint;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;

public class AdvancedAutoCraftingTable extends AbstractAdvancedAutoCrafter {
    public AdvancedAutoCraftingTable(
            ItemGroup itemGroup,
            SlimefunItemStack item,
            RecipeType recipeType,
            ItemStack[] recipe,
            int chargePerCraft,
            boolean withholding
    ) {
        super(itemGroup, item, recipeType, recipe, chargePerCraft, withholding);
    }

    public Set<Map.Entry<ItemStack[], ItemStack>> getRecipeEntries() {
        return SupportedCraftingTableRecipes.getRecipes().entrySet();
    }

    public boolean getRecipeTester(ItemStack[] inputs, ItemStack[] recipe) {
        return SupportedCraftingTableRecipes.testRecipe(inputs, recipe);
    }

    public boolean isValidBlueprint(SlimefunItem item) {
        return item instanceof CraftingBlueprint;
    }

    public boolean canTestVanillaRecipe() {
        return true;
    }
}