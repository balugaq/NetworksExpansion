package com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive;

import com.balugaq.netex.utils.Lang;
import com.balugaq.netex.utils.NetworksVersionedParticle;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellPersistence;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.slot.AEDriveMenuSlots;
import io.github.sefiraat.networks.Networks;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AECellUniquenessManager {

    private static final int[] FACE_OFFSETS_X = new int[]{1, -1, 0, 0};
    private static final int[] FACE_OFFSETS_Z = new int[]{0, 0, 1, -1};
    private static final Map<String, Set<Location>> UUID_TO_DRIVES = new ConcurrentHashMap<>();

    private AECellUniquenessManager() {
    }

    @Nullable
    public static String getUuidString(@Nullable ItemStack itemStack) {
        return AECellPersistence.getCellUUIDString(itemStack);
    }

    @NotNull
    public static Set<String> collectUuidStrings(@NotNull BlockMenu menu) {
        Set<String> uuids = new HashSet<>();
        for (int slot : AEDriveMenuSlots.CELL_SLOTS) {
            ItemStack item = menu.getItemInSlot(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            String uuid = getUuidString(item);
            if (uuid != null) {
                uuids.add(uuid);
            }
        }
        return uuids;
    }

    public static boolean isDuplicateUuid(@NotNull BlockMenu menu, @NotNull ItemStack candidate) {
        String uuid = getUuidString(candidate);
        if (uuid == null) {
            return false;
        }
        return collectUuidStrings(menu).contains(uuid);
    }

    public static void registerCell(@NotNull Location driveLocation, @NotNull ItemStack cell) {
        String uuid = getUuidString(cell);
        if (uuid == null) {
            return;
        }
        UUID_TO_DRIVES.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(driveLocation);
    }

    public static void registerDrive(@NotNull BlockMenu menu) {
        Location location = menu.getLocation();
        unregisterDrive(location);
        for (int slot : AEDriveMenuSlots.CELL_SLOTS) {
            ItemStack item = menu.getItemInSlot(slot);
            String uuid = getUuidString(item);
            if (uuid != null) {
                UUID_TO_DRIVES.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(location);
            }
        }
    }

    public static void unregisterDrive(@NotNull Location driveLocation) {
        if (UUID_TO_DRIVES.isEmpty()) {
            return;
        }
        List<String> empty = new ArrayList<>();
        for (Map.Entry<String, Set<Location>> entry : UUID_TO_DRIVES.entrySet()) {
            Set<Location> drives = entry.getValue();
            drives.remove(driveLocation);
            if (drives.isEmpty()) {
                empty.add(entry.getKey());
            }
        }
        for (String uuid : empty) {
            UUID_TO_DRIVES.remove(uuid);
        }
    }

    public static boolean isDuplicateAcrossDrives(@NotNull Location currentDrive, @NotNull ItemStack candidate) {
        String uuid = getUuidString(candidate);
        return uuid != null && isDuplicateAcrossDrives(currentDrive, uuid);
    }

    private static boolean isDuplicateAcrossDrives(@NotNull Location currentDrive, @NotNull String uuid) {
        Set<Location> drives = UUID_TO_DRIVES.get(uuid);
        if (drives == null || drives.isEmpty()) {
            return false;
        }
        for (Location drive : drives) {
            if (!drive.equals(currentDrive)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDuplicate(@NotNull BlockMenu menu, @NotNull ItemStack candidate) {
        return isDuplicateUuid(menu, candidate) || isDuplicateAcrossDrives(menu.getLocation(), candidate);
    }

    public static void scanAndEjectDuplicates(@NotNull BlockMenu menu, @NotNull Player player) {
        registerDrive(menu);
        Location location = menu.getLocation();
        Set<String> seen = new HashSet<>();
        for (int slot : AEDriveMenuSlots.CELL_SLOTS) {
            ItemStack item = menu.getItemInSlot(slot);
            String uuid = getUuidString(item);
            if (uuid == null) {
                continue;
            }
            if (!seen.add(uuid) || isDuplicateAcrossDrives(location, uuid)) {
                ejectDuplicate(menu, player, slot);
            }
        }
    }

    public static void ejectDuplicate(@NotNull BlockMenu menu, @NotNull Player player, int slot) {
        ItemStack item = menu.getItemInSlot(slot);
        if (item == null || item.getType().isAir()) {
            return;
        }
        menu.toInventory().setItem(slot, null);
        returnItem(player, menu.getLocation(), item);
    }

    public static void notifyDuplicateRejected(@NotNull Player player) {
        String message = Lang.getString("messages.ae.drive.cell_uuid_duplicate");
        player.sendTitle("", message, 5, 55, 20);
    }

    private static void returnItem(@NotNull Player player, @NotNull Location driveLocation, @NotNull ItemStack item) {
        Location target = findEjectLocation(driveLocation);
        if (target != null) {
            dropItem(target, item);
            return;
        }
        if (!player.getInventory().addItem(item).isEmpty()) {
            dropItem(player.getLocation(), item);
        }
    }

    @Nullable
    private static Location findEjectLocation(@NotNull Location driveLocation) {
        World world = driveLocation.getWorld();
        if (world == null) {
            return null;
        }
        Location above = driveLocation.clone().add(0, 1, 0);
        if (isPassable(above)) {
            return above;
        }
        for (int i = 0; i < FACE_OFFSETS_X.length; i++) {
            Location side = driveLocation.clone().add(FACE_OFFSETS_X[i], 0, FACE_OFFSETS_Z[i]);
            if (isPassable(side)) {
                return side;
            }
        }
        return null;
    }

    private static boolean isPassable(@NotNull Location location) {
        return location.getBlock().isPassable();
    }

    private static void dropItem(@NotNull Location location, @NotNull ItemStack item) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Location center = location.clone().add(0.5, 0.15, 0.5);
        Item entity = world.dropItem(center, item);
        if (entity == null) {
            return;
        }
        entity.setVelocity(new Vector(0, 0, 0));
        flashEntity(entity);
    }

    private static void flashEntity(@NotNull Item entity) {
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!entity.isValid()) {
                    cancel();
                    return;
                }
                entity.setGlowing(ticks % 2 == 0);
                World world = entity.getWorld();
                if (world != null) {
                    Location loc = entity.getLocation();
                    world.spawnParticle(NetworksVersionedParticle.DUST, loc, 8, 0.3, 0.3, 0.3, new Particle.DustOptions(0xFF5A5A, 1.2f));
                }
                if (++ticks >= 4) {
                    entity.setGlowing(false);
                    cancel();
                }
            }
        }.runTaskTimer(Networks.getInstance(), 0L, 4L);
    }
}