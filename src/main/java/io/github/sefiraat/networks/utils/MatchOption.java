package io.github.sefiraat.networks.utils;

import io.github.sefiraat.networks.network.stackcaches.ItemStackCache;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Data;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * 物品匹配选项（MatchOption）。
 * <p>
 * 所有 {@code itemsMatch} 重载最终都会路由到
 * {@link StackUtils#itemsMatch(ItemStackCache, ItemStack, MatchOption)}，
 * 并由这里描述的「忽略项」决定是否比对某些物品属性。
 * <p>
 * 「忽略项」使用 1 个 {@code long} 的位标记表示，未来新增忽略项时，只需追加一个位。
 * <p>
 * 默认值为 {@link #DEFAULT}：不忽略任何项，比较 lore / 数量以外的既有默认项。
 *
 * @see StackUtils#itemsMatch(ItemStackCache, ItemStack, MatchOption)
 * @author balugaq
 */
@Data
public class MatchOption {
    // <editor-fold desc="忽略项位标记，新增忽略项时在此追加">
    /** 忽略物品耐久（Damage） */
    public static final long IGNORE_DURABILITY = 1L;
    /** 忽略物品附魔（Enchantment） */
    public static final long IGNORE_ENCHANTMENT = 1L << 1;
    /** 忽略物品属性（Attribute） */
    public static final long IGNORE_ATTRIBUTE = 1L << 2;
    /** 忽略物品描述（Lore） */
    public static final long IGNORE_LORE = 1L << 3;
    /** 忽略物品自定义模型（Custom Model Data） */
    public static final long IGNORE_CUSTOM_MODEL_DATA = 1L << 4;
    /** 忽略物品数量 */
    @ApiStatus.Internal
    public static final long IGNORE_AMOUNT = 1L << 5; // Does NOT open to players
    // </editor-fold>

    private static final long IGNORE_MASK =
        IGNORE_DURABILITY | IGNORE_ENCHANTMENT | IGNORE_ATTRIBUTE | IGNORE_LORE | IGNORE_CUSTOM_MODEL_DATA | IGNORE_AMOUNT;

    /** 不忽略任何项 */
    public static final long NO_IGNORE = 0L;

    /** 默认选项：与改动前 {@code itemsMatch(cache, item)} 的行为保持一致 */
    public static final MatchOption DEFAULT = new MatchOption(IGNORE_AMOUNT | IGNORE_CUSTOM_MODEL_DATA);

    private final long ignoreFlags;

    private MatchOption(long ignoreFlags) {
        this.ignoreFlags = ignoreFlags & IGNORE_MASK;
    }

    public static @NotNull MatchOption of(long ignoreFlags) {
        return new MatchOption(ignoreFlags);
    }

    public @NotNull MatchOption withIgnore(long flag, boolean ignored) {
        final long flags = ignored ? (ignoreFlags | flag) : (ignoreFlags & ~flag);
        return new MatchOption(flags);
    }

    public boolean isIgnoreAmount() {
        return (ignoreFlags & IGNORE_AMOUNT) != 0;
    }

    public boolean isIgnoreDurability() {
        return (ignoreFlags & IGNORE_DURABILITY) != 0;
    }

    public boolean isIgnoreEnchantment() {
        return (ignoreFlags & IGNORE_ENCHANTMENT) != 0;
    }

    public boolean isIgnoreAttribute() {
        return (ignoreFlags & IGNORE_ATTRIBUTE) != 0;
    }

    public boolean isIgnoreLore() {
        return (ignoreFlags & IGNORE_LORE) != 0;
    }

    public boolean isIgnoreCustomModelData() {
        return (ignoreFlags & IGNORE_CUSTOM_MODEL_DATA) != 0;
    }

    /** 没有任何忽略项生效 */
    public boolean isEmpty() {
        return ignoreFlags == NO_IGNORE;
    }

    /** 只要有一个「忽略」标记生效 */
    public boolean isAnyIgnore() {
        return ignoreFlags != NO_IGNORE;
    }

    public static final Long2ObjectOpenHashMap<Set<DataComponentType>> cacheSets = new Long2ObjectOpenHashMap<>();
    static {
        cacheSets.defaultReturnValue(null);
    }

    public Set<DataComponentType> toDataComponentSet() {
        var set = cacheSets.get(ignoreFlags);
        if (set != null) return set;
        set = new HashSet<>();
        set.add(DataComponentTypes.LORE); // always ignore lore since it is served for StackUtils
        if (isIgnoreAttribute()) set.add(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (isIgnoreDurability()) set.add(DataComponentTypes.DAMAGE);
        if (isIgnoreEnchantment()) set.add(DataComponentTypes.ENCHANTMENTS);
        if (isIgnoreCustomModelData()) set.add(DataComponentTypes.CUSTOM_MODEL_DATA);
        cacheSets.put(ignoreFlags, set);
        return set;
    }
}
