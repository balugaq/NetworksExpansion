package com.ytdd9527.networksexpansion.implementation.machines.ae.utils;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.ae.constants.AECellUpgradeMaterialRegistry;
import com.ytdd9527.networksexpansion.implementation.machines.ae.constants.AEStorageCellType;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellPersistence;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AEStorageCellCache;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.networks.utils.datatypes.PersistentQuantumStorageType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 将手持 AE 存储元件中的物品批量转换为该元件对应等级的量子存储.
 *
 * <p>目标量子存储由元件类型经 {@link AECellUpgradeMaterialRegistry#getUpgradeMaterial}
 * 反查，容量与元件单格上限一致，故物品数量不会溢出；
 * 物品以净版形式写入量子存储以规避粘液物品序列化丢失；
 * 先经 {@link AEStorageCellCache#takeItem} 扣除、成功后才发放，防止复制；
 * 每成功转换一种物品，元件单元数（可存物品种类上限）相应递减，经
 * {@link AECellPersistence#setCurrentPerTypeLimit} 写入并同步缓存，下限为 1；
 * 背包满时掉落至玩家位置。结束统一异步落盘数据库并刷新元件 lore。
 */
public final class AECellQuantumConverter {

    private AECellQuantumConverter() {
    }

    // 指令和转换机共用：把物品+数量包成对应等级的量子储存成品
    @NotNull
    public static ItemStack buildQuantumStorage(@NotNull ItemStack sample, long amount, @NotNull NetworkQuantumStorage storage) {
        ItemStack qsItem = storage.getItem().clone();
        ItemMeta meta = qsItem.getItemMeta();
        QuantumCache quantumCache = new QuantumCache(
            sample.clone(), amount, storage.getMaxAmount(), false, storage.supportsCustomMaxAmount());
        DataTypeMethods.setCustom(meta, Keys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE, quantumCache);
        quantumCache.addMetaLore(meta);
        qsItem.setItemMeta(meta);
        return qsItem;
    }

    public static void convert(@NotNull Player player) {
        ItemStack cell = player.getInventory().getItemInMainHand();
        if (cell.getType() == Material.AIR || !AEStorageCell.isStorageCell(cell)) {
            player.sendMessage(Lang.getString("messages.commands.aetoquantum.need-cell"));
            return;
        }

        long perTypeLimit = AEStorageCell.getPerTypeLimit(cell);
        AEStorageCellType cellType = AEStorageCellType.fromAmount(perTypeLimit);
        NetworkQuantumStorage storage = cellType == null ? null : AECellUpgradeMaterialRegistry.getStorage(cellType);
        if (storage == null) {
            player.sendMessage(Lang.getString("messages.commands.aetoquantum.none-converted"));
            return;
        }

        AEStorageCellCache cache = AEStorageCell.loadCellCache(cell, perTypeLimit);
        List<AEStorageCellCache.CellEntry> entries = new ArrayList<>(cache.getStoredItems());
        if (entries.isEmpty()) {
            player.sendMessage(Lang.getString("messages.commands.aetoquantum.empty"));
            return;
        }

        int converted = 0;
        for (AEStorageCellCache.CellEntry entry : entries) {
            if (cache.takeItem(entry.sample, entry.amount) == null) {
                continue;
            }
            converted++;

            ItemStack qsItem = buildQuantumStorage(entry.sample, entry.amount, storage);

            if (!player.getInventory().addItem(qsItem).isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), qsItem);
            }
        }

        if (converted > 0) {
            long newUnits = AEStorageCell.getCurrentPerTypeLimit(cell) - converted;
            AECellPersistence.setCurrentPerTypeLimit(cell, newUnits);
        }

        if (Networks.getAeStorageDatabase() != null) {
            Networks.getAeStorageDatabase().saveAllAsync();
        }
        AEStorageCell.applyLore(cell, perTypeLimit, AEStorageCell.getCurrentPerTypeLimit(cell));
        player.getInventory().setItemInMainHand(cell);

        if (converted == 0) {
            player.sendMessage(Lang.getString("messages.commands.aetoquantum.none-converted"));
        } else {
            player.sendMessage(Lang.getString("messages.commands.aetoquantum.converted"));
        }
    }
}