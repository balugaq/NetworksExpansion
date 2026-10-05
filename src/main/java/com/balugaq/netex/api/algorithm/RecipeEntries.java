package com.balugaq.netex.api.algorithm;

import com.balugaq.netex.api.helpers.SupportedAncientAltarRecipes;
import com.balugaq.netex.api.helpers.SupportedArmorForgeRecipes;
import com.balugaq.netex.api.helpers.SupportedCompressorRecipes;
import com.balugaq.netex.api.helpers.SupportedCraftingTableRecipes;
import com.balugaq.netex.api.helpers.SupportedExpansionWorkbenchRecipes;
import com.balugaq.netex.api.helpers.SupportedGrindStoneRecipes;
import com.balugaq.netex.api.helpers.SupportedJuicerRecipes;
import com.balugaq.netex.api.helpers.SupportedMagicWorkbenchRecipes;
import com.balugaq.netex.api.helpers.SupportedOreCrusherRecipes;
import com.balugaq.netex.api.helpers.SupportedPressureChamberRecipes;
import com.balugaq.netex.api.helpers.SupportedQuantumWorkbenchRecipes;
import com.balugaq.netex.api.helpers.SupportedSmelteryRecipes;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RecipeEntries {
    private static final Map<ItemStack[], Set<ItemStack>> slimefunRecipes3x3 = new HashMap<>();

    static {
        loadRecipe(SupportedAncientAltarRecipes.getRecipes());
        loadRecipe(SupportedArmorForgeRecipes.getRecipes());
        loadRecipe(SupportedCompressorRecipes.getRecipes());
        loadRecipe(SupportedCraftingTableRecipes.getRecipes());
        loadRecipe(SupportedExpansionWorkbenchRecipes.getRecipes());
        loadRecipe(SupportedGrindStoneRecipes.getRecipes());
        loadRecipe(SupportedJuicerRecipes.getRecipes());
        loadRecipe(SupportedMagicWorkbenchRecipes.getRecipes());
        loadRecipe(SupportedOreCrusherRecipes.getRecipes());
        loadRecipe(SupportedPressureChamberRecipes.getRecipes());
        loadRecipe(SupportedQuantumWorkbenchRecipes.getRecipes());
        loadRecipe(SupportedSmelteryRecipes.getRecipes());
        // copy more ae
    }

    private static void loadRecipe(Map<ItemStack[], ItemStack> recipes) {
        recipes.forEach((k, v) -> {
            if (!slimefunRecipes3x3.containsKey(k))
                slimefunRecipes3x3.put(k, new HashSet<>());

            slimefunRecipes3x3.get(k).add(v);
        });
    }

    public static Set<Map.Entry<ItemStack[], Set<ItemStack>>> get3x3() {
        return slimefunRecipes3x3.entrySet();
    }
}
