package com.ytdd9527.networksexpansion.implementation.machines.ae.core.item;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.DistinctiveItem;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * A canonical, immutable identity key for an {@link ItemStack}.
 *
 * <p>The full item identity is captured via {@link ItemStack#serializeAsBytes()}
 * after the item has been reduced to a count of one and, when applicable,
 * canonicalized to its Slimefun template. An item matches another if and only if
 * they share the same material and full NBT data, ignoring stack size.</p>
 */
public class ItemKey {

    private final ItemStack itemStack;
    private final byte[] data;
    private final int hash;

    public ItemKey(@NotNull ItemStack itemStack) {
        itemStack = itemStack.asOne();
        final SlimefunItem sfItem = SlimefunItem.getByItem(itemStack);
        if (sfItem != null && !(sfItem instanceof DistinctiveItem)) {
            itemStack = sfItem.getItem().asOne();
        }
        this.itemStack = itemStack;
        this.data = itemStack.serializeAsBytes();
        this.hash = Arrays.hashCode(this.data);
    }

    @NotNull
    public ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemKey that)) {
            return false;
        }
        return Arrays.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "ItemKey{" + "itemStack=" + itemStack + '}';
    }
}