package com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell;

import com.ytdd9527.networksexpansion.implementation.machines.ae.constants.AECellKeys;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.AEStorageDatabase;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.dao.AEStorageCellController;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import io.github.sefiraat.networks.Networks;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public final class AECellPersistence {

    private AECellPersistence() {
    }

    public static boolean isStorageCell(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }
        return SlimefunItem.getByItem(itemStack) instanceof AEStorageCell;
    }

    @Nullable
    public static UUID getCellUUID(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return null;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }
        String uuidStr = PersistentDataAPI.getString(meta, AECellKeys.CELL_UUID_KEY);
        if (uuidStr == null || uuidStr.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public static String getCellUUIDString(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return null;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }
        String uuidStr = PersistentDataAPI.getString(meta, AECellKeys.CELL_UUID_KEY);
        return (uuidStr == null || uuidStr.isEmpty()) ? null : uuidStr;
    }

    @NotNull
    public static UUID getOrCreateCellUUID(@NotNull ItemStack itemStack) {
        UUID existing = getCellUUID(itemStack);
        if (existing != null) {
            return existing;
        }
        if (itemStack.getType().isAir()) {
            throw new IllegalArgumentException("无法为空气物品分配元件 UUID");
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            throw new IllegalArgumentException("ItemMeta cannot be null");
        }
        UUID uuid = UUID.randomUUID();
        PersistentDataAPI.setString(meta, AECellKeys.CELL_UUID_KEY, uuid.toString());
        itemStack.setItemMeta(meta);
        return uuid;
    }

    public static long getPerTypeLimit(@NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            Long cap = PersistentDataAPI.getLong(meta, AECellKeys.CELL_CAPACITY_KEY);
            if (cap != null && cap > 0) {
                return cap;
            }
        }
        SlimefunItem sfItem = SlimefunItem.getByItem(itemStack);
        if (sfItem instanceof AEStorageCell cell) {
            return cell.getPerTypeLimit();
        }
        return 0;
    }

    public static long getCurrentPerTypeLimit(@NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            Long current = PersistentDataAPI.getLong(meta, AECellKeys.CELL_CURRENT_CAPACITY_KEY);
            if (current != null && current > 0) {
                return current;
            }
        }
        return 1L;
    }

    public static void setCurrentPerTypeLimit(@NotNull ItemStack itemStack, long current) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        long per = getPerTypeLimit(itemStack);
        long maxUnits = getMaxUnitsFor(per);
        long safeCurrent = Math.max(1L, Math.min(current, maxUnits));
        PersistentDataAPI.setLong(meta, AECellKeys.CELL_CURRENT_CAPACITY_KEY, safeCurrent);
        itemStack.setItemMeta(meta);
    }

    public static long getMaxUnitsFor(long perTypeLimit) {
        return Math.min(perTypeLimit, AEStorageCellCache.getMaxUnitCount());
    }

    @Nullable
    public static String getCustomName(@NotNull UUID uuid) {
        AEStorageCellCache cache = AEStorageCellCache.getActiveCaches().get(uuid);
        return cache != null ? cache.getCustomName() : null;
    }

    public static void setCustomName(@NotNull ItemStack itemStack, @Nullable String name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        if (name != null && !name.isEmpty()) {
            PersistentDataAPI.setString(meta, AECellKeys.CELL_CUSTOM_NAME_KEY, name);
        } else {
            PersistentDataAPI.remove(meta, AECellKeys.CELL_CUSTOM_NAME_KEY);
        }
        itemStack.setItemMeta(meta);
        UUID uuid = getCellUUID(itemStack);
        if (uuid != null) {
            AEStorageCellCache cache = AEStorageCellCache.getActiveCaches().get(uuid);
            if (cache != null) {
                cache.setCustomName(name);
            }
        }
    }

    public static void initializeCell(@NotNull ItemStack itemStack, long perTypeLimit) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        String uuidStr = PersistentDataAPI.getString(meta, AECellKeys.CELL_UUID_KEY);
        if (uuidStr == null || uuidStr.isEmpty()) {
            PersistentDataAPI.setString(meta, AECellKeys.CELL_UUID_KEY, UUID.randomUUID().toString());
        }

        PersistentDataAPI.setLong(meta, AECellKeys.CELL_CAPACITY_KEY, perTypeLimit);
        long cur = PersistentDataAPI.getLong(meta, AECellKeys.CELL_CURRENT_CAPACITY_KEY);
        if (cur <= 0) {
            PersistentDataAPI.setLong(meta, AECellKeys.CELL_CURRENT_CAPACITY_KEY, 1L);
        }
        itemStack.setItemMeta(meta);
    }

    @NotNull
    public static AEStorageCellCache loadCellCache(@NotNull ItemStack itemStack, long perTypeLimit) {
        UUID uuid = getOrCreateCellUUID(itemStack);
        AEStorageCellCache cache = AEStorageCellCache.getActiveCaches().get(uuid);
        if (cache != null) {
            return cache;
        }

        long currentPerTypeLimit = Math.min(getCurrentPerTypeLimit(itemStack), Math.min(perTypeLimit, AEStorageCellCache.getMaxUnitCount()));
        cache = AEStorageCellCache.getOrCreate(uuid, perTypeLimit, currentPerTypeLimit);

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return cache;
        }

        String customName = PersistentDataAPI.getString(meta, AECellKeys.CELL_CUSTOM_NAME_KEY);
        cache.setCustomName(customName);

        AEStorageDatabase db = Networks.getAeStorageDatabase();
        if (db != null) {
            AEStorageCellController.CellData data = db.getStorageController().loadData(uuid);
            for (Map.Entry<ItemStack, Long> entry : data.storage.entrySet()) {
                cache.loadItemSilently(entry.getKey(), entry.getValue());
            }
            cache.loadMetaSilently(data.whitelistEnabled, data.whitelist);
        }
        return cache;
    }

    public static void flush() {
        AEStorageDatabase db = Networks.getAeStorageDatabase();
        if (db != null) {
            db.saveAllAsync();
        }
    }
}