package com.ytdd9527.networksexpansion.implementation.machines.cellnet.support;

import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.rule.CellAcceptRules;
import com.ytdd9527.networksexpansion.utils.itemstacks.ItemStackUtil;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.DistinctiveItem;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
public class ItemKey {

    private enum Mode {
        CANONICAL,
        EXACT,
        LIVE
    }

    private final ItemStack template;
    private final int hash;
    private volatile Boolean oversized;

    public ItemKey(@NotNull ItemStack itemStack) {
        this(itemStack, Mode.CANONICAL);
    }

    private ItemKey(@NotNull ItemStack itemStack, @NotNull Mode mode) {
        if (mode == Mode.LIVE) {
            this.template = itemStack;
            this.hash = ItemStackUtil.hashItemComponents(itemStack);
            return;
        }
        ItemStack unit = itemStack.asOne();
        if (mode == Mode.CANONICAL) {
            final SlimefunItem sfItem = SlimefunItem.getByItem(unit);
            if (sfItem != null && !(sfItem instanceof DistinctiveItem)) {
                unit = toPlainCopy(sfItem.getItem());
            }
        }
        this.template = unit;
        this.hash = unit.hashCode();
    }

    @NotNull
    public static ItemKey exact(@NotNull ItemStack itemStack) {
        return new ItemKey(itemStack, Mode.EXACT);
    }

    @NotNull
    public static ItemKey exactLive(@NotNull ItemStack itemStack) {
        if (!ItemStackUtil.componentsBridgeAvailable()) {
            return exact(itemStack);
        }
        return new ItemKey(itemStack, Mode.LIVE);
    }

    @NotNull
    private static ItemStack toPlainCopy(@NotNull ItemStack slimefunStack) {
        ItemStack plain = new ItemStack(slimefunStack.getType());
        ItemMeta meta = slimefunStack.getItemMeta();
        if (meta != null) {
            plain.setItemMeta(meta);
        }
        return plain;
    }

    @NotNull
    public ItemStack getItemStack() {
        return template.clone();
    }

    public boolean hasMeta() {
        return template.hasItemMeta();
    }

    public boolean isOversized() {
        Boolean cached = oversized;
        if (cached == null) {
            cached = CellAcceptRules.isNbtOversized(template);
            oversized = cached;
        }
        return cached;
    }

    public boolean matches(@Nullable ItemStack other) {
        return other != null && template.isSimilar(other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ItemKey that
            && hash == that.hash
            && template.isSimilar(that.template);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
