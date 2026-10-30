package com.ytdd9527.networksexpansion.implementation.machines.cargo.transfer.line.advanced;

import com.balugaq.netex.api.enums.TransferType;
import com.balugaq.netex.api.interfaces.GrabTickOnly;
import com.balugaq.netex.api.interfaces.SoftCellBannable;
import com.balugaq.netex.api.interfaces.VanillaTransfer;
import com.ytdd9527.networksexpansion.core.machines.AbstractTransfer;
import io.github.sefiraat.networks.network.NodeType;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AdvancedLineTransferVanillaGrabber extends AbstractTransfer implements SoftCellBannable, GrabTickOnly, VanillaTransfer {
    public AdvancedLineTransferVanillaGrabber(@NotNull final ItemGroup itemGroup, @NotNull final SlimefunItemStack item, @NotNull final RecipeType recipeType, final ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.LINE_TRANSFER_VANILLA_GRABBER);
    }

    @Override
    public TransferType getTransferType() {
        return TransferType.ADVANCED_LINE_TRANSFER_VANILLA_GRABBER;
    }

    @Override
    public boolean runSync() {
        return true;
    }
}
