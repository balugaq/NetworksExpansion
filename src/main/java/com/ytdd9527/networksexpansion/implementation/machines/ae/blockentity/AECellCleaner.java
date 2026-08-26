package com.ytdd9527.networksexpansion.implementation.machines.ae.blockentity;

import com.balugaq.netex.utils.Lang;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellPersistence;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AEStorageCellCache;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import com.ytdd9527.networksexpansion.implementation.machines.ae.utils.AENumberFormat;
import io.github.sefiraat.networks.Networks;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


    /**
     * 元件清理台：清理元件内不需要的物品。仅在有查看者时刷新，1 秒一次。
     */
    public class AECellCleaner extends SpecialSlimefunItem {

        public static final int CELL_SLOT = 4;
        public static final int PREV = 0;
        public static final int NEXT = 8;
        public static final int INFO = 45;
    public static final int CLOSE = 53;
    public static final int[] DISPLAY_SLOTS = new int[]{
        9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    public static final int ITEMS_PER_PAGE = DISPLAY_SLOTS.length;
    public static final int[] BACKGROUND_SLOTS = new int[]{
        1, 2, 3, 5, 6, 7,
        46, 47, 48, 49, 50, 51, 52
    };

    private static final NamespacedKey CELL_SLOT_MARKER_KEY =
        new NamespacedKey("networks", "ae_cleaner_cell_slot_marker");
    private static final ItemStack DISPLAY_PLACEHOLDER = buildDisplayPlaceholder();

    private final Map<Location, Integer> pageCache = new HashMap<>();

    public AECellCleaner(
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
                setSize(54);
                drawBackground(BACKGROUND_SLOTS);
            }

            @Override
            public boolean canOpen(@NotNull Block block, @NotNull Player player) {
                return player.hasPermission("slimefun.inventory.bypass")
                    || (AECellCleaner.this.canUse(player, false)
                    && Slimefun.getProtectionManager()
                    .hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK));
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }

            @Override
            public void newInstance(@NotNull BlockMenu menu, @NotNull Block block) {
                ItemStack existing = menu.getItemInSlot(CELL_SLOT);
                if (existing == null || existing.getType().isAir()) {
                    menu.replaceExistingItem(CELL_SLOT, createCellSlotMarker());
                }
                for (int slot : DISPLAY_SLOTS) {
                    menu.replaceExistingItem(slot, DISPLAY_PLACEHOLDER.clone());
                }
                setupHandlers(menu);
                refresh(menu);
            }
        };
    }

    private void onBreak(@NotNull BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        pageCache.remove(location);
        BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
        if (blockMenu != null) {
            ItemStack cellItem = blockMenu.getItemInSlot(CELL_SLOT);
            if (cellItem != null && AEStorageCell.isStorageCell(cellItem)) {
                blockMenu.dropItems(location, CELL_SLOT);
            }
        }
    }

    private static boolean isCellSlotMarker(@Nullable ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(CELL_SLOT_MARKER_KEY, PersistentDataType.BOOLEAN);
    }

    @NotNull
    private static ItemStack createCellSlotMarker() {
        ItemStack marker = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = marker.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Lang.getString("messages.ae.cleaner.cell_slot"));
            List<String> lore = new ArrayList<>();
            lore.add(Lang.getString("messages.ae.cleaner.cell_slot_lore"));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(CELL_SLOT_MARKER_KEY, PersistentDataType.BOOLEAN, true);
            marker.setItemMeta(meta);
        }
        return marker;
    }

    private void setupHandlers(@NotNull BlockMenu menu) {
        menu.addMenuClickHandler(CELL_SLOT, (player, slot, item, action) -> {
            if (action.isShiftClicked()) {
                return true;
            }
            ItemStack cursor = player.getItemOnCursor();
            boolean slotIsMarker = isCellSlotMarker(item);
            boolean cursorIsCell = cursor != null && AEStorageCell.isStorageCell(cursor);
            boolean cursorIsEmpty = cursor == null || cursor.getType().isAir();

            if (slotIsMarker) {
                if (cursorIsCell) {
                    ItemStack placed = cursor.asQuantity(1);
                    menu.replaceExistingItem(slot, placed);
                    player.setItemOnCursor(cursor.getAmount() <= 1 ? null : cursor.asQuantity(cursor.getAmount() - 1));
                    insertCell(menu, placed, player);
                }
            } else if (cursorIsEmpty) {
                ItemStack ejected = ejectCell(menu, slot);
                if (ejected != null) {
                    player.setItemOnCursor(ejected);
                }
            } else if (cursorIsCell) {
                ItemStack ejected = ejectCell(menu, slot);
                ItemStack placed = cursor.asQuantity(1);
                menu.replaceExistingItem(slot, placed);
                player.setItemOnCursor(cursor.getAmount() <= 1 ? null : cursor.asQuantity(cursor.getAmount() - 1));
                insertCell(menu, placed, player);
                if (ejected != null) {
                    giveOrDrop(player, ejected);
                }
            }
            return false;
        });

        for (int displaySlot : DISPLAY_SLOTS) {
            menu.addMenuClickHandler(displaySlot, (player, slot, item, action) -> {
                ItemStack cursor = player.getItemOnCursor();
                boolean cursorHasItem = cursor != null && !cursor.getType().isAir();
                boolean slotHasCellItem = item != null && !item.getType().isAir()
                    && item.getType() != Material.GREEN_STAINED_GLASS_PANE;
                if (slotHasCellItem && !cursorHasItem) {
                    deleteItem(menu, slot);
                }
                return false;
            });
        }

        menu.addMenuClickHandler(PREV, (player, slot, item, action) -> {
            int page = pageCache.getOrDefault(menu.getLocation(), 0);
            if (page > 0) {
                pageCache.put(menu.getLocation(), page - 1);
                refresh(menu);
            }
            return false;
        });

        menu.addMenuClickHandler(NEXT, (player, slot, item, action) -> {
            int page = pageCache.getOrDefault(menu.getLocation(), 0);
            int max = maxPages(menu);
            if (page < max - 1) {
                pageCache.put(menu.getLocation(), page + 1);
                refresh(menu);
            }
            return false;
        });

        menu.addMenuClickHandler(INFO, (player, slot, item, action) -> false);

        menu.addMenuClickHandler(CLOSE, (player, slot, item, action) -> {
            player.closeInventory();
            return false;
        });

        for (int bg : BACKGROUND_SLOTS) {
            menu.addMenuClickHandler(bg, (p, s, i, a) -> false);
        }
    }

    private void insertCell(@NotNull BlockMenu menu, @NotNull ItemStack cellItem, @NotNull Player player) {
        if (AECellPersistence.isWrongServer(player, cellItem)) {
            player.sendMessage(Lang.getString("messages.ae.cell.wrong_server"));
            return;
        }
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
        pageCache.put(menu.getLocation(), 0);
        menu.replaceExistingItem(CELL_SLOT, cellItem);
        refresh(menu);
    }

    @Nullable
    private ItemStack ejectCell(@NotNull BlockMenu menu, int slot) {
        ItemStack cellItem = menu.getItemInSlot(slot);
        if (cellItem == null || !AEStorageCell.isStorageCell(cellItem)) {
            return null;
        }
        long per = AEStorageCell.getPerTypeLimit(cellItem);
        AEStorageCell.loadCellCache(cellItem, per);
        AEStorageCell.applyLore(cellItem, per, AEStorageCell.getCurrentPerTypeLimit(cellItem));

        menu.replaceExistingItem(slot, createCellSlotMarker());

        clearDisplay(menu);
        pageCache.put(menu.getLocation(), 0);
        refresh(menu);
        return cellItem;
    }

    private static void giveOrDrop(@NotNull Player player, @NotNull ItemStack item) {
        ItemStack onCursor = player.getItemOnCursor();
        if (onCursor == null || onCursor.getType().isAir()) {
            player.setItemOnCursor(item);
            return;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), drop);
            }
        }
    }

    private void deleteItem(@NotNull BlockMenu menu, int displaySlot) {
        ItemStack cellItem = menu.getItemInSlot(CELL_SLOT);
        if (cellItem == null || !AEStorageCell.isStorageCell(cellItem)) {
            return;
        }
        long per = AEStorageCell.getPerTypeLimit(cellItem);
        AEStorageCellCache cache = AEStorageCell.loadCellCache(cellItem, per);

        int page = pageCache.getOrDefault(menu.getLocation(), 0);
        int idx = -1;
        for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
            if (DISPLAY_SLOTS[i] == displaySlot) {
                idx = page * ITEMS_PER_PAGE + i;
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
        menu.replaceExistingItem(CELL_SLOT, cellItem);
        refresh(menu);

        if (Networks.getAeStorageDatabase() != null) {
            Networks.getAeStorageDatabase().saveAllAsync();
        }
    }

    private void refresh(@NotNull BlockMenu menu) {
        ItemStack cellItem = menu.getItemInSlot(CELL_SLOT);

        if (cellItem == null || !AEStorageCell.isStorageCell(cellItem)) {
            menu.replaceExistingItem(CELL_SLOT, createCellSlotMarker());
            clearDisplay(menu);
            menu.replaceExistingItem(INFO, buildInfoItem(null, 0));
            menu.replaceExistingItem(PREV, buildPageButton(false, false));
            menu.replaceExistingItem(NEXT, buildPageButton(false, true));
            menu.replaceExistingItem(CLOSE, buildCloseButton());
            return;
        }

        long per = AEStorageCell.getPerTypeLimit(cellItem);
        AEStorageCellCache cache = AEStorageCell.loadCellCache(cellItem, per);

        AEStorageCell.applyLore(cellItem, per, AEStorageCell.getCurrentPerTypeLimit(cellItem));
        menu.replaceExistingItem(CELL_SLOT, cellItem);

        List<AEStorageCellCache.CellEntry> entries = cache.getStoredItems();
        int page = pageCache.getOrDefault(menu.getLocation(), 0);
        int maxPages = Math.max(1, (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE));
        if (page >= maxPages) {
            page = Math.max(0, maxPages - 1);
            pageCache.put(menu.getLocation(), page);
        }

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, entries.size());

        for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
            int slot = DISPLAY_SLOTS[i];
            if (i < end - start) {
                AEStorageCellCache.CellEntry entry = entries.get(start + i);
                menu.replaceExistingItem(slot, buildDisplayItem(entry.sample, entry.amount));
            } else {
                menu.replaceExistingItem(slot, DISPLAY_PLACEHOLDER.clone());
            }
        }

        menu.replaceExistingItem(PREV, buildPageButton(page > 0, false));
        menu.replaceExistingItem(NEXT, buildPageButton(page < maxPages - 1, true));
        menu.replaceExistingItem(INFO, buildInfoItem(cellItem, entries.size()));
        menu.replaceExistingItem(CLOSE, buildCloseButton());
    }

    private void clearDisplay(@NotNull BlockMenu menu) {
        for (int slot : DISPLAY_SLOTS) {
            menu.replaceExistingItem(slot, DISPLAY_PLACEHOLDER.clone());
        }
    }

    private int maxPages(@NotNull BlockMenu menu) {
        ItemStack cellItem = menu.getItemInSlot(CELL_SLOT);
        if (cellItem == null || !AEStorageCell.isStorageCell(cellItem)) {
            return 1;
        }
        long per = AEStorageCell.getPerTypeLimit(cellItem);
        AEStorageCellCache cache = AEStorageCell.loadCellCache(cellItem, per);
        int count = cache.getStoredItems().size();
        return Math.max(1, (int) Math.ceil((double) count / ITEMS_PER_PAGE));
    }

    @NotNull
    private static ItemStack buildDisplayItem(@NotNull ItemStack sample, long amount) {
        ItemStack display = sample.clone();
        display.setAmount(1);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            String displayName = ItemStackHelper.getDisplayName(sample);
            if (displayName == null || displayName.isEmpty()) {
                displayName = sample.getType().name();
            }
            meta.setDisplayName(displayName);
            List<String> lore = new ArrayList<>();
            lore.add(Lang.getString("messages.ae.cleaner.item_count", AENumberFormat.formatNumber(amount)));
            lore.add("");
            lore.add(Lang.getString("messages.ae.cleaner.delete_hint"));
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    @NotNull
    private static ItemStack buildInfoItem(@Nullable ItemStack cellItem, int types) {
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            meta.setDisplayName(Lang.getString("messages.ae.cleaner.info_title"));
            if (cellItem != null && AEStorageCell.isStorageCell(cellItem)) {
                lore.add(Lang.getString("messages.ae.cleaner.cell_name", ItemStackHelper.getDisplayName(cellItem)));
                lore.add(Lang.getString("messages.ae.cleaner.item_types", types));
            } else {
                lore.add(Lang.getString("messages.ae.cleaner.no_cell"));
            }
            meta.setLore(lore);
            info.setItemMeta(meta);
        }
        return info;
    }

    @NotNull
    private static ItemStack buildPageButton(boolean enabled, boolean next) {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            if (next) {
                meta.setDisplayName(enabled
                    ? Lang.getString("messages.ae.cleaner.next_page")
                    : Lang.getString("messages.ae.cleaner.last_page"));
            } else {
                meta.setDisplayName(enabled
                    ? Lang.getString("messages.ae.cleaner.prev_page")
                    : Lang.getString("messages.ae.cleaner.first_page"));
            }
            button.setItemMeta(meta);
        }
        return button;
    }

    @NotNull
    private static ItemStack buildCloseButton() {
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta meta = close.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Lang.getString("messages.ae.cleaner.close"));
            close.setItemMeta(meta);
        }
        return close;
    }

    @NotNull
    private static ItemStack buildDisplayPlaceholder() {
        ItemStack placeholder = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            placeholder.setItemMeta(meta);
        }
        return placeholder;
    }
}
