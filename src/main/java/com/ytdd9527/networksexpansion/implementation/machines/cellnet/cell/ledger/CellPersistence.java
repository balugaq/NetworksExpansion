package com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.ledger;

import com.google.common.base.Preconditions;
import io.github.sefiraat.networks.utils.Keys;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ItemKey;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.storage.CellStorageDatabase;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.storage.dao.CellDao;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.StorageCell;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.utils.datatypes.DataTypeMethods;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import me.ddggdd135.guguslimefunlib.GuguSlimefunLib;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.jeff_media.morepersistentdatatypes.DataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.Cell;
public final class CellPersistence {

    private CellPersistence() {
    }

    public static void saveAsync() {
        CellStorageDatabase db = Networks.getCellStorageDatabase();
        if (db != null) {
            db.saveAllAsync();
        }
    }

    public static boolean isStorageCell(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }
        return SlimefunItem.getByItem(itemStack) instanceof StorageCell;
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
        String uuidStr = PersistentDataAPI.getString(meta, Keys.CELL_UUID);
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
        String uuidStr = PersistentDataAPI.getString(meta, Keys.CELL_UUID);
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
        PersistentDataAPI.setString(meta, Keys.CELL_UUID, uuid.toString());
        setServerUUID(meta);
        itemStack.setItemMeta(meta);
        return uuid;
    }

    public static long getPerTypeLimit(@NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            Long cap = PersistentDataAPI.getLong(meta, Keys.CELL_CAPACITY);
            if (cap != null && cap > 0) {
                return cap;
            }
        }
        SlimefunItem sfItem = SlimefunItem.getByItem(itemStack);
        if (sfItem instanceof StorageCell cell) {
            return cell.getPerTypeLimit();
        }
        return 0;
    }

    public static long getCurrentPerTypeLimit(@NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            Long current = PersistentDataAPI.getLong(meta, Keys.CELL_CURRENT_CAPACITY);
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
        if (isUnlimited(per)) {
            return;
        }
        long maxUnits = getMaxUnitsFor(per);
        long safeCurrent = Math.max(1L, Math.min(current, maxUnits));
        PersistentDataAPI.setLong(meta, Keys.CELL_CURRENT_CAPACITY, safeCurrent);
        itemStack.setItemMeta(meta);

        UUID uuid = getCellUUID(itemStack);
        if (uuid != null) {
            CellLedger cache = CellLedger.getActiveCaches().get(uuid);
            if (cache != null) {
                cache.setCurrentPerTypeLimit(safeCurrent);
            }
        }
    }

    public static boolean isUnlimited(long perTypeLimit) {
        return CellLedger.isUnlimitedPerType(perTypeLimit);
    }

    public static long getMaxUnitsFor(long perTypeLimit) {
        if (isUnlimited(perTypeLimit)) {
            return Long.MAX_VALUE;
        }
        return Math.min(perTypeLimit, CellLedger.getMaxUnitCount());
    }

    @Nullable
    public static String getCustomName(@NotNull UUID uuid) {
        CellLedger cache = CellLedger.getActiveCaches().get(uuid);
        return cache != null ? cache.getCustomName() : null;
    }

    public static void setCustomName(@NotNull ItemStack itemStack, @Nullable String name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        if (name != null && !name.isEmpty()) {
            PersistentDataAPI.setString(meta, Keys.CELL_CUSTOM_NAME, name);
        } else {
            PersistentDataAPI.remove(meta, Keys.CELL_CUSTOM_NAME);
        }
        itemStack.setItemMeta(meta);
        UUID uuid = getCellUUID(itemStack);
        if (uuid != null) {
            CellLedger cache = CellLedger.getActiveCaches().get(uuid);
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

        String uuidStr = PersistentDataAPI.getString(meta, Keys.CELL_UUID);
        if (uuidStr == null || uuidStr.isEmpty()) {
            PersistentDataAPI.setString(meta, Keys.CELL_UUID, UUID.randomUUID().toString());
        }
        setServerUUID(meta);

        PersistentDataAPI.setLong(meta, Keys.CELL_CAPACITY, perTypeLimit);
        Long cur = PersistentDataAPI.getLong(meta, Keys.CELL_CURRENT_CAPACITY);
        if (cur == null || cur <= 0) {
            long initial = isUnlimited(perTypeLimit) ? Long.MAX_VALUE : 1L;
            PersistentDataAPI.setLong(meta, Keys.CELL_CURRENT_CAPACITY, initial);
        }
        itemStack.setItemMeta(meta);
    }

    @NotNull
    public static CellLedger loadCellCache(@NotNull ItemStack itemStack, long perTypeLimit) {
        UUID uuid = getOrCreateCellUUID(itemStack);
        CellLedger cache = CellLedger.getActiveCaches().get(uuid);
        if (cache != null) {
            loadReservedFromItem(cache, itemStack);
            loadVoidExcessFromItem(cache, itemStack);
            return cache;
        }

        boolean unlimited = isUnlimited(perTypeLimit);
        long currentPerTypeLimit = unlimited
            ? Long.MAX_VALUE
            : Math.min(getCurrentPerTypeLimit(itemStack),
                Math.min(perTypeLimit, CellLedger.getMaxUnitCount()));
        // First load must be atomic: a plain get→create sequence lets two threads run
        // restoreStoredItems against the same ledger and double every stored amount.
        return CellLedger.getActiveCaches().computeIfAbsent(uuid, k -> {
            CellLedger created = new CellLedger(uuid, perTypeLimit, currentPerTypeLimit);
            created.setUnlimited(unlimited);

            loadMetaFromItem(created, itemStack);
            restoreStoredItems(created, uuid);
            loadReservedFromItem(created, itemStack);
            loadVoidExcessFromItem(created, itemStack);
            if (hasLegacyVoidExcess(itemStack)) {
                created.migrateLegacyVoidExcess();
            }
            return created;
        });
    }

    public static void saveReservedItems(@NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        UUID uuid = getCellUUID(itemStack);
        CellLedger cache = uuid == null ? null : CellLedger.getActiveCaches().get(uuid);
        List<ItemStack> templates = cache == null ? List.of() : cache.getReservedTemplates();
        if (templates.isEmpty()) {
            PersistentDataAPI.remove(meta, Keys.CELL_RESERVED_ITEMS);
        } else {
            DataTypeMethods.setCustom(meta, Keys.CELL_RESERVED_ITEMS, DataType.ITEM_STACK_ARRAY,
                templates.toArray(new ItemStack[0]));
        }
        itemStack.setItemMeta(meta);
    }

    private static void loadReservedFromItem(@NotNull CellLedger cache, @NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        ItemStack[] templates = DataTypeMethods.getCustom(meta, Keys.CELL_RESERVED_ITEMS, DataType.ITEM_STACK_ARRAY);
        if (templates == null) {
            return;
        }
        for (ItemStack template : templates) {
            if (template == null || template.getType().isAir()) {
                continue;
            }
            cache.restoreReserved(new ItemKey(template));
        }
    }

    public static void saveVoidExcessItems(@NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        UUID uuid = getCellUUID(itemStack);
        CellLedger cache = uuid == null ? null : CellLedger.getActiveCaches().get(uuid);
        List<ItemStack> templates = cache == null ? List.of() : cache.getVoidExcessTemplates();
        if (templates.isEmpty()) {
            PersistentDataAPI.remove(meta, Keys.CELL_VOID_EXCESS_ITEMS);
        } else {
            DataTypeMethods.setCustom(meta, Keys.CELL_VOID_EXCESS_ITEMS, DataType.ITEM_STACK_ARRAY,
                templates.toArray(new ItemStack[0]));
        }
        itemStack.setItemMeta(meta);
    }

    private static void loadVoidExcessFromItem(@NotNull CellLedger cache, @NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        ItemStack[] templates = DataTypeMethods.getCustom(meta, Keys.CELL_VOID_EXCESS_ITEMS, DataType.ITEM_STACK_ARRAY);
        if (templates == null) {
            return;
        }
        for (ItemStack template : templates) {
            if (template == null || template.getType().isAir()) {
                continue;
            }
            cache.restoreVoidExcessUnit(new ItemKey(template));
        }
    }

    private static boolean hasLegacyVoidExcess(@NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        return meta != null && PersistentDataAPI.getByte(meta, Keys.CELL_VOID_EXCESS) == (byte) 1;
    }

    private static void loadMetaFromItem(@NotNull CellLedger cache, @NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        String customName = PersistentDataAPI.getString(meta, Keys.CELL_CUSTOM_NAME);
        cache.setCustomName(customName);
    }

    private static void restoreStoredItems(@NotNull CellLedger cache, @NotNull UUID uuid) {
        CellStorageDatabase db = Networks.getCellStorageDatabase();
        if (db == null) {
            return;
        }
        CellDao.CellData data = db.getStorageController().loadData(uuid);
        for (Map.Entry<ItemStack, Long> entry : data.storage.entrySet()) {
            cache.loadItemSilently(entry.getKey(), entry.getValue());
        }
        cache.loadMetaSilently(data.whitelistEnabled, data.whitelist);
    }

    private static void setServerUUID(@NotNull ItemMeta meta) {
        if (Networks.getSupportedPluginManager().isGuguSlimefunLib()) {
            meta.getPersistentDataContainer().set(Keys.CELL_SERVER, DataType.UUID, GuguSlimefunLib.getServerUUID());
        }
    }

    @Nullable
    public static UUID getServerUUID(@Nullable ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }
        return itemStack.getItemMeta().getPersistentDataContainer().get(Keys.CELL_SERVER, DataType.UUID);
    }

    public static boolean isWrongServer(@NotNull Player player, @NotNull ItemStack itemStack) {
        if (!Networks.getSupportedPluginManager().isGuguSlimefunLib()) {
            return false;
        }
        UUID suuid = getServerUUID(itemStack);
        return suuid != null && !player.isOp() && !suuid.equals(GuguSlimefunLib.getServerUUID());
    }
}