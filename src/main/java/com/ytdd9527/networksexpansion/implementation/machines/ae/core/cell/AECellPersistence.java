package com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell;

import com.google.common.base.Preconditions;
import io.github.sefiraat.networks.utils.Keys;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.AEStorageDatabase;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.dao.AEStorageCellController;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import io.github.sefiraat.networks.Networks;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import me.ddggdd135.guguslimefunlib.GuguSlimefunLib;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.jeff_media.morepersistentdatatypes.DataType;
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
        String uuidStr = PersistentDataAPI.getString(meta, Keys.AE_CELL_UUID);
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
        String uuidStr = PersistentDataAPI.getString(meta, Keys.AE_CELL_UUID);
        return (uuidStr == null || uuidStr.isEmpty()) ? null : uuidStr;
    }

    @NotNull
    public static UUID getOrCreateCellUUID(@NotNull ItemStack itemStack) {
        Preconditions.checkArgument(!itemStack.getType().isAir(), "Cannot assign cell UUID to air");
        UUID existing = getCellUUID(itemStack);
        if (existing != null) {
            return existing;
        }
        ItemMeta meta = itemStack.getItemMeta();
        Preconditions.checkNotNull(meta, "ItemMeta cannot be null");
        UUID uuid = UUID.randomUUID();
        PersistentDataAPI.setString(meta, Keys.AE_CELL_UUID, uuid.toString());
        setServerUUID(meta);
        itemStack.setItemMeta(meta);
        return uuid;
    }

    public static long getPerTypeLimit(@NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            Long cap = PersistentDataAPI.getLong(meta, Keys.AE_CELL_CAPACITY);
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
            Long current = PersistentDataAPI.getLong(meta, Keys.AE_CELL_CURRENT_CAPACITY);
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
        // 无限元件单元数恒为无限，不接受增减(避免 MAX+1 溢出归零)
        if (per >= Long.MAX_VALUE / 2) {
            return;
        }
        long maxUnits = getMaxUnitsFor(per);
        long safeCurrent = Math.max(1L, Math.min(current, maxUnits));
        PersistentDataAPI.setLong(meta, Keys.AE_CELL_CURRENT_CAPACITY, safeCurrent);
        itemStack.setItemMeta(meta);

        UUID uuid = getCellUUID(itemStack);
        if (uuid != null) {
            AEStorageCellCache cache = AEStorageCellCache.getActiveCaches().get(uuid);
            if (cache != null) {
                cache.setCurrentPerTypeLimit(safeCurrent);
            }
        }
    }

    public static long getMaxUnitsFor(long perTypeLimit) {
        if (perTypeLimit >= Long.MAX_VALUE / 2) {
            return Long.MAX_VALUE;
        }
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
            PersistentDataAPI.setString(meta, Keys.AE_CELL_CUSTOM_NAME, name);
        } else {
            PersistentDataAPI.remove(meta, Keys.AE_CELL_CUSTOM_NAME);
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

        String uuidStr = PersistentDataAPI.getString(meta, Keys.AE_CELL_UUID);
        if (uuidStr == null || uuidStr.isEmpty()) {
            PersistentDataAPI.setString(meta, Keys.AE_CELL_UUID, UUID.randomUUID().toString());
        }
        setServerUUID(meta);

        PersistentDataAPI.setLong(meta, Keys.AE_CELL_CAPACITY, perTypeLimit);
        Long cur = PersistentDataAPI.getLong(meta, Keys.AE_CELL_CURRENT_CAPACITY);
        if (cur == null || cur <= 0) {
            long initial = perTypeLimit >= Long.MAX_VALUE / 2 ? Long.MAX_VALUE : 1L;
            PersistentDataAPI.setLong(meta, Keys.AE_CELL_CURRENT_CAPACITY, initial);
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

        boolean unlimited = perTypeLimit >= Long.MAX_VALUE / 2;
        long currentPerTypeLimit = unlimited
            ? Long.MAX_VALUE
            : Math.min(getCurrentPerTypeLimit(itemStack),
                Math.min(perTypeLimit, AEStorageCellCache.getMaxUnitCount()));
        cache = AEStorageCellCache.getOrCreate(uuid, perTypeLimit, currentPerTypeLimit);
        cache.setUnlimited(unlimited);

        loadMetaFromItem(cache, itemStack);
        restoreStoredItems(cache, uuid);
        return cache;
    }

    private static void loadMetaFromItem(@NotNull AEStorageCellCache cache, @NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        String customName = PersistentDataAPI.getString(meta, Keys.AE_CELL_CUSTOM_NAME);
        cache.setCustomName(customName);
    }

    private static void restoreStoredItems(@NotNull AEStorageCellCache cache, @NotNull UUID uuid) {
        AEStorageDatabase db = Networks.getAeStorageDatabase();
        if (db == null) {
            return;
        }
        AEStorageCellController.CellData data = db.getStorageController().loadData(uuid);
        for (Map.Entry<ItemStack, Long> entry : data.storage.entrySet()) {
            cache.loadItemSilently(entry.getKey(), entry.getValue());
        }
        cache.loadMetaSilently(data.whitelistEnabled, data.whitelist);
    }

    private static void setServerUUID(@NotNull ItemMeta meta) {
        if (Networks.getSupportedPluginManager().isGuguSlimefunLib()) {
            meta.getPersistentDataContainer().set(Keys.AE_CELL_SERVER, DataType.UUID, GuguSlimefunLib.getServerUUID());
        }
    }

    @Nullable
    public static UUID getServerUUID(@Nullable ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }
        return itemStack.getItemMeta().getPersistentDataContainer().get(Keys.AE_CELL_SERVER, DataType.UUID);
    }

    public static boolean isWrongServer(@NotNull Player player, @NotNull ItemStack itemStack) {
        if (!Networks.getSupportedPluginManager().isGuguSlimefunLib()) {
            return false;
        }
        UUID suuid = getServerUUID(itemStack);
        return suuid != null && !player.isOp() && !suuid.equals(GuguSlimefunLib.getServerUUID());
    }
}