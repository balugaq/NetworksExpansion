package com.ytdd9527.networksexpansion.implementation.machines.ae.blockentity;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AEStorageCellCache;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.workbench.AECellWorkbenchDisplay;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.workbench.AECellWorkbenchMenuHandler;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.slot.AECellWorkbenchMenuSlots;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AECellWorkbench extends SpecialSlimefunItem {

    private final Map<Location, Integer> pageCache = new HashMap<>();

    public AECellWorkbench(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @NotNull
    public Map<Location, Integer> getPageCache() {
        return pageCache;
    }

    @Override
    public void preRegister() {
        addItemHandler(
            new BlockTicker() {
                @Override
                public boolean isSynchronized() {
                    return false;
                }

                @Override
                public void tick(@NotNull Block b, SlimefunItem item, SlimefunBlockData data) {
                    BlockMenu blockMenu = StorageCacheUtils.getMenu(b.getLocation());
                    if (blockMenu != null && blockMenu.hasViewer()) {
                        AECellWorkbenchDisplay.refresh(blockMenu, AECellWorkbench.this);
                    }
                }
            },
            new BlockBreakHandler(false, false) {
                @Override
                public void onPlayerBreak(@NotNull BlockBreakEvent event, @NotNull ItemStack item, @NotNull List<ItemStack> drops) {
                    onBreak(event);
                }
            },
            new BlockPlaceHandler(false) {
                @Override
                public void onPlayerPlace(@NotNull BlockPlaceEvent event) {
                }
            });
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                setSize(54);
                drawBackground(AECellWorkbenchMenuSlots.BACKGROUND_SLOTS);
            }

            @Override
            public boolean canOpen(@NotNull Block block, @NotNull Player player) {
                return player.hasPermission("slimefun.inventory.bypass")
                    || (AECellWorkbench.this.canUse(player, false)
                    && Slimefun.getProtectionManager()
                    .hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK));
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }

            @Override
            public void newInstance(@NotNull BlockMenu menu, @NotNull Block block) {
                menu.toInventory().setItem(AECellWorkbenchMenuSlots.CELL_SLOT, AECellWorkbenchMenuHandler.createCellSlotMarker());
                ItemStack displayPlaceholder = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
                ItemMeta dm = displayPlaceholder.getItemMeta();
                if (dm != null) {
                    dm.setDisplayName(" ");
                    displayPlaceholder.setItemMeta(dm);
                }
                for (int slot : AECellWorkbenchMenuSlots.DISPLAY_SLOTS) {
                    menu.replaceExistingItem(slot, displayPlaceholder.clone());
                }
                AECellWorkbenchMenuHandler.setupHandlers(menu, block, AECellWorkbench.this);
                AECellWorkbenchDisplay.refresh(menu, AECellWorkbench.this);
            }
        };
    }

    private void onBreak(@NotNull BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        pageCache.remove(location);
        BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
        if (blockMenu != null) {
            ItemStack cellItem = blockMenu.toInventory().getItem(AECellWorkbenchMenuSlots.CELL_SLOT);
            if (cellItem != null && AEStorageCell.isStorageCell(cellItem)) {
                blockMenu.dropItems(location, AECellWorkbenchMenuSlots.CELL_SLOT);
                blockMenu.toInventory().setItem(AECellWorkbenchMenuSlots.CELL_SLOT, null);
            }
        }
        Slimefun.getDatabaseManager().getBlockDataController().removeBlock(location);
    }

    public static void insertCell(@NotNull BlockMenu menu, @NotNull ItemStack cellItem, @NotNull Player player, @NotNull AECellWorkbench workbench) {
        if (!AEStorageCell.isStorageCell(cellItem)) {
            return;
        }

        long perTypeLimit = AEStorageCell.getPerTypeLimit(cellItem);
        if (perTypeLimit <= 0) {
            SlimefunItem sfItem = SlimefunItem.getByItem(cellItem);
            if (sfItem instanceof AEStorageCell aeCell) {
                perTypeLimit = aeCell.getPerTypeLimit();
                AEStorageCell.initializeCell(cellItem, perTypeLimit);
            }
        }

        AEStorageCell.loadCellCache(cellItem, perTypeLimit);

        workbench.getPageCache().put(menu.getLocation(), 0);
        AECellWorkbenchDisplay.refresh(menu, workbench);
    }

    public static void ejectCell(@NotNull BlockMenu menu, int slot, @NotNull Player player, @NotNull AECellWorkbench workbench) {
        ItemStack cellItem = menu.toInventory().getItem(slot);
        if (cellItem == null || !AEStorageCell.isStorageCell(cellItem)) {
            return;
        }
        long per = AEStorageCell.getPerTypeLimit(cellItem);
        AEStorageCellCache cache = AEStorageCell.loadCellCache(cellItem, per);
        AEStorageCell.applyLore(cellItem, per, AEStorageCell.getCurrentPerTypeLimit(cellItem));

        menu.toInventory().setItem(slot, AECellWorkbenchMenuHandler.createCellSlotMarker());

        player.setItemOnCursor(cellItem);

        AECellWorkbenchDisplay.clearDisplay(menu);
        workbench.getPageCache().put(menu.getLocation(), 0);
        AECellWorkbenchDisplay.refresh(menu, workbench);
    }

    public static void deleteItem(@NotNull BlockMenu menu, int displaySlot, @NotNull AECellWorkbench workbench) {
        ItemStack cellItem = menu.getItemInSlot(AECellWorkbenchMenuSlots.CELL_SLOT);
        if (cellItem == null || !AEStorageCell.isStorageCell(cellItem)) {
            return;
        }
        long per = AEStorageCell.getPerTypeLimit(cellItem);
        AEStorageCellCache cache = AEStorageCell.loadCellCache(cellItem, per);

        int page = workbench.getPageCache().getOrDefault(menu.getLocation(), 0);
        int idx = -1;
        for (int i = 0; i < AECellWorkbenchMenuSlots.DISPLAY_SLOTS.length; i++) {
            if (AECellWorkbenchMenuSlots.DISPLAY_SLOTS[i] == displaySlot) {
                idx = page * AECellWorkbenchMenuSlots.ITEMS_PER_PAGE + i;
                break;
            }
        }
        if (idx < 0) {
            return;
        }

        List<AEStorageCellCache.CellEntry> entries = cache.getStoredItems();
        if (idx >= entries.size()) {
            return;
        }

        AEStorageCellCache.CellEntry target = entries.get(idx);
        cache.takeItem(target.sample, target.amount);

        menu.replaceExistingItem(AECellWorkbenchMenuSlots.CELL_SLOT, cellItem);
        AECellWorkbenchDisplay.refresh(menu, workbench);
    }
}
