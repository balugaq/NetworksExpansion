package com.ytdd9527.networks.expansion.core.items.machines.autocrafters.advanced;

import com.ytdd9527.networks.expansion.core.helpers.SupportedSmelteryRecipes;
import com.ytdd9527.networks.expansion.core.items.machines.autocrafters.system.blueprints.SmelteryBlueprint;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;

public class AdvancedAutoSmelteryCrafter extends AbstractAdvancedAutoCrafter {
    public AdvancedAutoSmelteryCrafter(
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
        return SupportedSmelteryRecipes.getRecipes().entrySet();
    }

    public boolean getRecipeTester(ItemStack[] inputs, ItemStack[] recipe) {
        return SupportedSmelteryRecipes.testRecipe(inputs, recipe);
    }

    public boolean isVaildBlueprint(SlimefunItem item) {
        return item instanceof SmelteryBlueprint;
    }
}
