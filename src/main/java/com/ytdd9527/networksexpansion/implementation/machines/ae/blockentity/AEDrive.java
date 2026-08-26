package com.ytdd9527.networksexpansion.implementation.machines.ae.blockentity;

import com.balugaq.netex.utils.Lang;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellPersistence;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive.AECellUniquenessManager;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive.AEDriveStorage;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.drive.AEDriveMenuHandler;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.drive.AEDriveDisplay;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.slot.AEDriveMenuSlots;
import io.github.sefiraat.networks.Networks;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AEDrive extends SpecialSlimefunItem {

    private static final long DISPLAY_REFRESH_INTERVAL_MS = 1000L;
    private static boolean autoSaveStarted = false;

    private static final AEDriveStorage storage = new AEDriveStorage();

    private final Map<Location, Integer> itemAccessPageCache = new HashMap<>();
    private final Map<Location, Long> displayRefreshTimestamps = new HashMap<>();

    @NotNull
    public static AEDriveStorage getStorage() {
        return storage;
    }

    public AEDrive(
            @NotNull ItemGroup itemGroup,
            @NotNull SlimefunItemStack item,
            @NotNull RecipeType recipeType,
            ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe);
        if (!autoSaveStarted) {
            autoSaveStarted = true;
            startAutoSave();
        }
    }

    @NotNull
    public Map<Location, Integer> getItemAccessPageCache() {
        return itemAccessPageCache;
    }

    @Override
    public void preRegister() {
        addItemHandler(
            new BlockTicker() {

                @Override
                public boolean isSynchronized() {
                    return true;
                }

                @Override
                public void tick(@NotNull Block b, SlimefunItem item, SlimefunBlockData data) {
                    final BlockMenu blockMenu = StorageCacheUtils.getMenu(b.getLocation());
                    if (blockMenu == null || !blockMenu.hasViewer()) {
                        return;
                    }
                    final Location location = b.getLocation();
                    final long now = System.currentTimeMillis();
                    final Long last = displayRefreshTimestamps.get(location);
                    if (last != null && now - last < DISPLAY_REFRESH_INTERVAL_MS) {
                        return;
                    }
                    displayRefreshTimestamps.put(location, now);
                    AEDriveDisplay.updateMainDisplay(blockMenu, storage);
                }
            },
            new BlockBreakHandler(false, false) {

                @Override
                public void onPlayerBreak(@NotNull BlockBreakEvent event, @NotNull ItemStack item, @NotNull List<ItemStack> drops) {
                    onBreak(event);
                }
            });
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                setSize(45);
                drawBackground(AEDriveMenuSlots.MAIN_BACKGROUND_SLOTS);
            }

            @Override
            public boolean canOpen(@NotNull Block block, @NotNull Player player) {
                return player.hasPermission("slimefun.inventory.bypass")
                        || (AEDrive.this.canUse(player, false)
                        && Slimefun.getProtectionManager()
                        .hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK));
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }

            @Override
            public void newInstance(@NotNull BlockMenu menu, @NotNull Block block) {
                AEDriveMenuHandler.setupMainMenuHandlers(menu, block, AEDrive.this, storage);
                AEDriveDisplay.updateMainDisplay(menu, storage);
            }
        };
    }

    private void onBreak(@NotNull BlockBreakEvent event) {
        final Location location = event.getBlock().getLocation();
        itemAccessPageCache.remove(location);
        displayRefreshTimestamps.remove(location);
        AECellUniquenessManager.unregisterDrive(location);
        storage.invalidateCellCache(location);
        final BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
        if (blockMenu != null) {
            for (int slot : AEDriveMenuSlots.CELL_SLOTS) {
                blockMenu.dropItems(location, slot);
            }
        }
        Slimefun.getDatabaseManager().getBlockDataController().removeBlock(location);
    }

    public static void saveAllDriveCells() {
        Networks.getInstance().getLogger().info(Lang.getString("messages.ae.drive.saving"));
        AECellPersistence.flush();
        Networks.getInstance().getLogger().info(Lang.getString("messages.ae.drive.saved"));
    }

    private static void startAutoSave() {
        Networks networks = Networks.getInstance();
        int seconds = Networks.getConfigManager().getInt("drawer-auto-save-period", 300);
        seconds = seconds <= 0 ? 300 : seconds;
        long period = 20L * seconds;
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAllDriveCells();
            }
        }.runTaskTimerAsynchronously(networks, 2 * period, period);
    }

    @NotNull
    public Map<ItemStack, Long> getAllCellItems(@NotNull BlockMenu menu) {
        return storage.getAllCellItems(storage.getCells(menu));
    }
}