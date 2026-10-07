/**
 * MIT License
 *
 * Copyright (c) 2024 Ddggdd135
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.ytdd9527.networksexpansion.implementation.machines.cellnet.support;

import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.rule.CellAcceptRules;
import com.ytdd9527.networksexpansion.utils.itemstacks.ItemStackUtil;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.DistinctiveItem;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

/**
 * An ItemStack-keyed map backed by an internal {@link ConcurrentHashMap}.
 * <p>
 * Derived from SlimeAE's ItemHashMap (MIT License, see licenses/MIT.md),
 *
 * @author Ddggdd135
 * @author ytdd9526
 */
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
