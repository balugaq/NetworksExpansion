package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.workbench;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.ae.blockentity.AECellWorkbench;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.slot.AECellWorkbenchMenuSlots;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class AECellWorkbenchMenuHandler {

    private static final NamespacedKey CELL_SLOT_MARKER_KEY = new NamespacedKey("networks", "ae_workbench_cell_slot_marker");

    private AECellWorkbenchMenuHandler() {
    }

    public static boolean isCellSlotMarker(@org.jetbrains.annotations.Nullable ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(CELL_SLOT_MARKER_KEY, PersistentDataType.BOOLEAN);
    }

    @NotNull
    public static ItemStack createCellSlotMarker() {
        ItemStack marker = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = marker.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Lang.getString("messages.ae.workbench.cell_slot"));
            List<String> lore = new ArrayList<>();
            lore.add(Lang.getString("messages.ae.workbench.cell_slot_lore"));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(CELL_SLOT_MARKER_KEY, PersistentDataType.BOOLEAN, true);
            marker.setItemMeta(meta);
        }
        return marker;
    }

    public static void setupHandlers(@NotNull BlockMenu menu, @NotNull Block block, @NotNull AECellWorkbench workbench) {
        menu.addMenuClickHandler(AECellWorkbenchMenuSlots.CELL_SLOT, new ChestMenu.AdvancedMenuClickHandler() {
            @Override
            public boolean onClick(InventoryClickEvent e, Player player, int slot, ItemStack cursor, ClickAction action) {
                Inventory inventory = e.getClickedInventory();
                if (inventory == null) {
                    return false;
                }
                ItemStack itemInSlot = inventory.getItem(slot);
                boolean slotIsMarker = isCellSlotMarker(itemInSlot);
                boolean cursorIsCell = cursor != null && AEStorageCell.isStorageCell(cursor);
                boolean cursorIsEmpty = cursor == null || cursor.getType().isAir();

                if (slotIsMarker) {
                    if (cursorIsCell) {
                        ItemStack placed = cursor.asQuantity(1);
                        inventory.setItem(slot, placed);
                        e.getWhoClicked().setItemOnCursor(cursor.getAmount() <= 1 ? null : cursor.asQuantity(cursor.getAmount() - 1));
                        AECellWorkbench.insertCell(menu, placed, player, workbench);
                    }
                } else {
                    if (cursorIsEmpty) {
                        AECellWorkbench.ejectCell(menu, slot, player, workbench);
                    } else if (cursorIsCell) {
                        AECellWorkbench.ejectCell(menu, slot, player, workbench);
                        ItemStack placed = cursor.asQuantity(1);
                        inventory.setItem(slot, placed);
                        e.getWhoClicked().setItemOnCursor(cursor.getAmount() <= 1 ? null : cursor.asQuantity(cursor.getAmount() - 1));
                        AECellWorkbench.insertCell(menu, placed, player, workbench);
                    }
                }
                return false;
            }

            @Override
            public boolean onClick(Player player, int slot, ItemStack item, ClickAction action) {
                return false;
            }
        });

        for (int displaySlot : AECellWorkbenchMenuSlots.DISPLAY_SLOTS) {
            menu.addMenuClickHandler(displaySlot, new ChestMenu.AdvancedMenuClickHandler() {
                @Override
                public boolean onClick(InventoryClickEvent e, Player player, int slot, ItemStack cursor, ClickAction action) {
                    ItemStack item = menu.getItemInSlot(slot);
                    boolean cursorHasItem = cursor != null && !cursor.getType().isAir();
                    boolean slotHasCellItem = item != null && !item.getType().isAir() && item.getType() != Material.GREEN_STAINED_GLASS_PANE;

                    if (slotHasCellItem && !cursorHasItem) {
                        AECellWorkbench.deleteItem(menu, slot, workbench);
                    }
                    return false;
                }

                @Override
                public boolean onClick(Player player, int slot, ItemStack item, ClickAction action) {
                    return false;
                }
            });
        }

        menu.addMenuClickHandler(AECellWorkbenchMenuSlots.PREV, (player, slot, item, action) -> {
            int page = workbench.getPageCache().getOrDefault(menu.getLocation(), 0);
            if (page > 0) {
                workbench.getPageCache().put(menu.getLocation(), page - 1);
                AECellWorkbenchDisplay.refresh(menu, workbench);
            }
            return false;
        });

        menu.addMenuClickHandler(AECellWorkbenchMenuSlots.NEXT, (player, slot, item, action) -> {
            int page = workbench.getPageCache().getOrDefault(menu.getLocation(), 0);
            int max = AECellWorkbenchDisplay.maxPages(menu);
            if (page < max - 1) {
                workbench.getPageCache().put(menu.getLocation(), page + 1);
                AECellWorkbenchDisplay.refresh(menu, workbench);
            }
            return false;
        });

        menu.addMenuClickHandler(AECellWorkbenchMenuSlots.INFO, (player, slot, item, action) -> false);

        menu.addMenuClickHandler(AECellWorkbenchMenuSlots.CLOSE, (player, slot, item, action) -> {
            player.closeInventory();
            return false;
        });

        for (int bg : AECellWorkbenchMenuSlots.BACKGROUND_SLOTS) {
            menu.addMenuClickHandler(bg, (p, s, i, a) -> false);
        }
    }
}