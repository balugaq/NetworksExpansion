package com.ytdd9527.networksexpansion.implementation.machines.ae.item;

import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AEStorageCellCache;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.cell.AECellMenu;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellPersistence;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component.AECellLore;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AEStorageCell extends SpecialSlimefunItem implements NotPlaceable {

    private final long perTypeLimit;

    public AEStorageCell(
            @NotNull ItemGroup itemGroup,
            @NotNull SlimefunItemStack item,
            @NotNull RecipeType recipeType,
            ItemStack @NotNull [] recipe,
            long perTypeLimit) {
        super(itemGroup, item, recipeType, recipe);
        this.perTypeLimit = perTypeLimit;
    }

    public long getPerTypeLimit() {
        return perTypeLimit;
    }

    @Override
    public void preRegister() {
        addItemHandler((ItemUseHandler) e -> {
            if (e.getPlayer().isSneaking()) {
                return;
            }
            ItemStack cell = e.getItem();
            long per = getPerTypeLimit(cell);
            loadCellCache(cell, per);
            applyLore(cell, per, getCurrentPerTypeLimit(cell));
            e.getPlayer().getInventory().setItemInMainHand(cell);
            AECellMenu.open(e.getPlayer(), cell);
            e.cancel();
        });
    }

    public static boolean isStorageCell(@Nullable ItemStack itemStack) {
        return AECellPersistence.isStorageCell(itemStack);
    }

    @Nullable
    public static UUID getCellUUID(@Nullable ItemStack itemStack) {
        return AECellPersistence.getCellUUID(itemStack);
    }

    @NotNull
    public static UUID getOrCreateCellUUID(@NotNull ItemStack itemStack) {
        return AECellPersistence.getOrCreateCellUUID(itemStack);
    }

    public static long getPerTypeLimit(@NotNull ItemStack itemStack) {
        return AECellPersistence.getPerTypeLimit(itemStack);
    }

    public static long getCurrentPerTypeLimit(@NotNull ItemStack itemStack) {
        return AECellPersistence.getCurrentPerTypeLimit(itemStack);
    }

    public static void applyLore(@NotNull ItemStack itemStack, long perTypeLimit, long currentPerTypeLimit) {
        AECellLore.applySpecLore(itemStack, perTypeLimit, currentPerTypeLimit);
    }

    public static void initializeCell(@NotNull ItemStack itemStack, long perTypeLimit) {
        AECellPersistence.initializeCell(itemStack, perTypeLimit);
    }

    @NotNull
    public static AEStorageCellCache loadCellCache(@NotNull ItemStack itemStack, long perTypeLimit) {
        return AECellPersistence.loadCellCache(itemStack, perTypeLimit);
    }
}
