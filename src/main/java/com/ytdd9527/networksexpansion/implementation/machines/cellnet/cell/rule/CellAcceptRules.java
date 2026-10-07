package com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.rule;

import com.balugaq.netex.utils.Debug;
import io.github.sefiraat.networks.utils.Keys;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class CellAcceptRules {

    private static final int MAX_ITEM_BYTES = 128 * 1024;

    private CellAcceptRules() {
    }

    public static boolean isNbtOversized(@NotNull ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) {
            return false;
        }
        try {
            ItemStack unit = itemStack.clone();
            unit.setAmount(1);
            return unit.serializeAsBytes().length > MAX_ITEM_BYTES;
        } catch (Exception e) {
            Debug.debug("物品 NBT 体量检测序列化失败，按超大处理拒收: " + e.getMessage());
            return true;
        }
    }

    public static boolean isUsedCell(@NotNull ItemStack itemStack) {
        try {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta == null) {
                return false;
            }
            return meta.getPersistentDataContainer().has(Keys.CELL_UUID, PersistentDataType.STRING)
                || meta.getPersistentDataContainer().has(Keys.VOID_FILTERS, PersistentDataType.BYTE_ARRAY);
        } catch (Exception e) {
            Debug.debug("元件使用痕迹检测失败，按已使用拒收: " + e.getMessage());
            return true;
        }
    }

    public static boolean isFilledContainer(@NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta instanceof BlockStateMeta blockStateMeta
            && blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
            return !shulkerBox.getInventory().isEmpty();
        }
        if (meta instanceof BundleMeta bundleMeta) {
            return bundleMeta.hasItems();
        }
        return false;
    }
}
