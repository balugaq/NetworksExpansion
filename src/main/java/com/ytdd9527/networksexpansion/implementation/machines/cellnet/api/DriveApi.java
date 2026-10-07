package com.ytdd9527.networksexpansion.implementation.machines.cellnet.api;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.CellHandle;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive.CellDrive;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive.DriveOwnership;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive.WhitelistStore;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DriveApi {

    private DriveApi() {
    }

    public static boolean isDrive(@Nullable SlimefunItem item) {
        return DriveType.of(item) != null;
    }

    @Nullable
    public static DriveType typeOf(@Nullable SlimefunItem item) {
        return DriveType.of(item);
    }

    @Nullable
    public static UUID owner(@NotNull Location location) {
        return DriveOwnership.getOwnerUuid(location);
    }

    @NotNull
    public static List<UUID> whitelist(@NotNull Location location) {
        UUID ownerUuid = DriveOwnership.getOwnerUuid(location);
        if (ownerUuid == null) {
            return List.of();
        }
        return List.copyOf(WhitelistStore.getWhitelist(ownerUuid));
    }

    @Nullable
    public static Map<ItemStack, Long> storageSnapshot(@NotNull Location location) {
        SlimefunItem item = StorageCacheUtils.getSfItem(location);
        if (DriveType.of(item) == null) {
            return null;
        }
        BlockMenu menu = StorageCacheUtils.getMenu(location);
        if (menu == null) {
            return null;
        }
        List<CellHandle> cells = CellDrive.getStorage().getCells(menu);
        return Collections.unmodifiableMap(CellDrive.getStorage().getAllCellItems(cells));
    }
}
