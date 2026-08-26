package com.ytdd9527.networksexpansion.implementation.machines.ae.core.item;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.DistinctiveItem;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * 归一化物品身份键，忽略数量（{@code asOne()}），身份由物品种类与完整 NBT 字节决定.
 *
 * <p>已注册粘液物品未实现 {@link DistinctiveItem} 时以原型物品统一身份，
 * 附魔、等级等实例差异不产生不同键。{@link #getItemStack()} 始终返回克隆副本。
 * @author TimetownDev（SlimeAE，MIT）
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
        return itemStack.clone();
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
