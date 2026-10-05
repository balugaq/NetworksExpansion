package com.ytdd9527.networksexpansion.implementation.machines.cellnet.assembly;

import io.github.sefiraat.networks.utils.StackUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.Bukkit;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
public final class RecipeLegitimacy {

    private RecipeLegitimacy() {
    }

    public static boolean isValid(@NotNull List<ItemStack> inputs, @NotNull ItemStack output) {
        if (inputs.isEmpty() || output.getType().isAir() || output.getAmount() <= 0) {
            return false;
        }
        List<ItemStack> merged = AssemblyCard.mergeInputs(inputs);
        ItemStack outOne = output.clone();
        outOne.setAmount(1);

        return matchesVanillaRecipe(merged, output, outOne) || matchesSlimefunRecipe(merged, output, outOne);
    }

    private static boolean matchesVanillaRecipe(
            @NotNull List<ItemStack> merged, @NotNull ItemStack output, @NotNull ItemStack outOne) {
        for (Recipe recipe : Bukkit.getRecipesFor(output)) {
            if (recipe instanceof ShapedRecipe shaped) {
                List<ItemStack> need = new ArrayList<>();
                for (ItemStack grid : shaped.getIngredientMap().values()) {
                    if (grid != null && !grid.getType().isAir()) {
                        need.add(grid);
                    }
                }
                if (matchesCraftingOutput(shaped.getResult(), output) && sameConsumption(merged, need)) {
                    return true;
                }
            } else if (recipe instanceof ShapelessRecipe shapeless) {
                List<ItemStack> need = new ArrayList<>(shapeless.getIngredientList());
                if (matchesCraftingOutput(shapeless.getResult(), output) && sameConsumption(merged, need)) {
                    return true;
                }
            } else if (recipe instanceof FurnaceRecipe furnace) {
                if (matchesSmelting(furnace.getInputChoice(), furnace.getResult(), merged, output)) {
                    return true;
                }
            } else if (recipe instanceof CampfireRecipe campfire) {
                if (matchesSmelting(campfire.getInputChoice(), campfire.getResult(), merged, output)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesSlimefunRecipe(
            @NotNull List<ItemStack> merged, @NotNull ItemStack output, @NotNull ItemStack outOne) {
        for (SlimefunItem item : Slimefun.getRegistry().getAllSlimefunItems()) {
            ItemStack[] grid = item.getRecipe();
            if (grid == null || item.getRecipeType() == RecipeType.NULL) {
                continue;
            }
            ItemStack result = item.getRecipeOutput();
            if (result == null || result.getType().isAir()) {
                continue;
            }
            List<ItemStack> need = new ArrayList<>();
            for (ItemStack cell : grid) {
                if (cell != null && !cell.getType().isAir()) {
                    need.add(cell);
                }
            }
            if (need.isEmpty()) {
                continue;
            }
            ItemStack resultOne = result.clone();
            resultOne.setAmount(1);
            if (!StackUtils.itemsMatch(outOne, resultOne) || result.getAmount() != output.getAmount()) {
                continue;
            }
            if (sameConsumption(merged, need)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesCraftingOutput(@Nullable ItemStack result, @NotNull ItemStack output) {
        if (result == null || result.getType().isAir() || result.getAmount() != output.getAmount()) {
            return false;
        }
        ItemStack resultOne = result.clone();
        resultOne.setAmount(1);
        ItemStack outOne = output.clone();
        outOne.setAmount(1);
        return StackUtils.itemsMatch(outOne, resultOne);
    }

    private static boolean matchesSmelting(
            @NotNull org.bukkit.inventory.RecipeChoice choice,
            @Nullable ItemStack result,
            @NotNull List<ItemStack> merged,
            @NotNull ItemStack output) {
        if (merged.size() != 1 || result == null || result.getType().isAir()) {
            return false;
        }
        ItemStack inputOne = merged.get(0).clone();
        inputOne.setAmount(1);
        if (!choice.test(inputOne)) {
            return false;
        }
        ItemStack resultOne = result.clone();
        resultOne.setAmount(1);
        ItemStack outOne = output.clone();
        outOne.setAmount(1);
        return StackUtils.itemsMatch(outOne, resultOne) && result.getAmount() == output.getAmount();
    }

    private static boolean sameConsumption(@NotNull List<ItemStack> card, @NotNull List<ItemStack> recipe) {
        List<ItemStack> need = AssemblyCard.mergeInputs(recipe);
        if (card.size() != need.size()) {
            return false;
        }
        boolean[] used = new boolean[need.size()];
        for (ItemStack cardItem : card) {
            ItemStack cardOne = cardItem.clone();
            cardOne.setAmount(1);
            boolean hit = false;
            for (int i = 0; i < need.size(); i++) {
                if (used[i]) {
                    continue;
                }
                ItemStack recipeItem = need.get(i);
                ItemStack recipeOne = recipeItem.clone();
                recipeOne.setAmount(1);
                if (StackUtils.itemsMatch(cardOne, recipeOne) && recipeItem.getAmount() == cardItem.getAmount()) {
                    used[i] = true;
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                return false;
            }
        }
        return true;
    }
}
