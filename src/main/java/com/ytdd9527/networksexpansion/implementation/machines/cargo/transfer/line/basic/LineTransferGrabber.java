package com.ytdd9527.networksexpansion.implementation.machines.cargo.transfer.line.basic;

import com.balugaq.netex.api.enums.TransferType;
import com.balugaq.netex.api.interfaces.GrabTickOnly;
import com.balugaq.netex.api.interfaces.SoftCellBannable;
import com.ytdd9527.networksexpansion.core.machines.AbstractTransfer;
import io.github.sefiraat.networks.network.NodeType;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class LineTransferGrabber extends AbstractTransfer implements SoftCellBannable, GrabTickOnly {
    public LineTransferGrabber(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.TRANSFER_GRABBER);
    }

    @Override
    public @NotNull TransferType getTransferType() {
        return TransferType.LINE_TRANSFER_GRABBER;
    }
}
