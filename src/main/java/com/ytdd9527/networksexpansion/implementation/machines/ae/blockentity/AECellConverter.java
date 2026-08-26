package com.ytdd9527.networksexpansion.implementation.machines.ae.blockentity;

import com.balugaq.netex.utils.Lang;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import com.ytdd9527.networksexpansion.implementation.machines.ae.constants.AECellUpgradeMaterialRegistry;
import com.ytdd9527.networksexpansion.implementation.machines.ae.constants.AEStorageCellType;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellPersistence;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AEStorageCellCache;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.item.ItemKey;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import com.ytdd9527.networksexpansion.implementation.machines.ae.utils.AECellQuantumConverter;
import com.ytdd9527.networksexpansion.implementation.machines.ae.utils.AENumberFormat;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.StackUtils;
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
 * 元件量子转换机。
 *
 * <p>支持两种模式：转出(元件→同级量子储存，产物进输出槽，转一种扣一个单元数)与
 * 灌入(同级量子储存→元件，物品入元件、量子储存消耗、新类型单元+1)。
 *
 * <p>仅在槽位有元件且菜单被查看时刷新，并以 1 秒间隔节流，避免高频重绘。
 */
public class AECellConverter extends SpecialSlimefunItem {

    public static final int CELL_SLOT = 4;
    public static final int PREV = 0;
    public static final int NEXT = 8;
    public static final int MODE_SLOT = 48;
    public static final int CLOSE = 53;
    public static final int[] DISPLAY_SLOTS = new int[]{
        9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26
    };
    public static final int[] SEPARATOR_SLOTS = new int[]{
        27, 28, 29, 30, 31, 32, 33, 34, 35
    };
    public static final int[] OUTPUT_SLOTS = new int[]{
        36, 37, 38, 39, 40, 41, 42, 43, 44,
        45, 46, 47
    };
    public static final int ITEMS_PER_PAGE = DISPLAY_SLOTS.length;
    public static final int[] BACKGROUND_SLOTS = new int[]{1, 2, 3, 5, 6, 7, 49, 50, 51, 52};

    private static final NamespacedKey CELL_SLOT_MARKER_KEY =
        new NamespacedKey("networks", "ae_converter_cell_slot_marker");
    private static final ItemStack DISPLAY_PLACEHOLDER = buildDisplayPlaceholder();
    private static final ItemStack BORDER_ICON = buildBorderIcon();
    private static final ItemStack SEPARATOR_ICON = buildSeparatorIcon();

    private final Map<Location, Integer> pageCache = new HashMap<>();
    private final Map<Location, Boolean> exportMode = new HashMap<>();

    public AECellConverter(
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
                for (int slot : BACKGROUND_SLOTS) {
                    addItem(slot, BORDER_ICON.clone());
                }
                for (int slot : SEPARATOR_SLOTS) {
                    addItem(slot, SEPARATOR_ICON.clone());
                }
            }

            @Override
            public boolean canOpen(@NotNull Block block, @NotNull Player player) {
                return player.hasPermission("slimefun.inventory.bypass")
                    || (AECellConverter.this.canUse(player, false)
                    && Slimefun.getProtectionManager()
                    .hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK));
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return flow == ItemTransportFlow.WITHDRAW ? OUTPUT_SLOTS : new int[0];
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
        exportMode.remove(location);
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
            meta.setDisplayName(Lang.getString("messages.ae.converter.cell_slot"));
            List<String> lore = new ArrayList<>();
            lore.add(Lang.getString("messages.ae.converter.cell_slot_lore"));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(CELL_SLOT_MARKER_KEY, PersistentDataType.BOOLEAN, true);
            marker.setItemMeta(meta);
        }
        return marker;
    }

    private static boolean isPlaceholder(@Nullable ItemStack item) {
        return item == null || item.getType().isAir() || item.getType() == Material.GREEN_STAINED_GLASS_PANE;
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
                if (!isPlaceholder(item)) {
                    if (isExportMode(menu)) {
                        transferItem(menu, player, slot);
                    } else {
                        importItem(menu, player, slot);
                    }
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

        menu.addMenuClickHandler(MODE_SLOT, (player, slot, item, action) -> {
            boolean exporting = isExportMode(menu);
            exportMode.put(menu.getLocation(), !exporting);
            pageCache.put(menu.getLocation(), 0);
            refresh(menu);
            return false;
        });

        menu.addMenuClickHandler(CLOSE, (player, slot, item, action) -> {
            player.closeInventory();
            return false;
        });

        for (int slot : BACKGROUND_SLOTS) {
            menu.addMenuClickHandler(slot, (p, s, i, a) -> false);
        }
        for (int slot : SEPARATOR_SLOTS) {
            menu.addMenuClickHandler(slot, (p, s, i, a) -> false);
        }
    }

    private boolean isExportMode(@NotNull BlockMenu menu) {
        return exportMode.getOrDefault(menu.getLocation(), true);
    }

    private void transferItem(@NotNull BlockMenu menu, @NotNull Player player, int displaySlot) {
        ItemStack cellItem = menu.getItemInSlot(CELL_SLOT);
        if (cellItem == null || !AEStorageCell.isStorageCell(cellItem)) {
            return;
        }
        long per = AEStorageCell.getPerTypeLimit(cellItem);
        AEStorageCellCache cache = AEStorageCell.loadCellCache(cellItem, per);

        int idx = displayIndex(menu, displaySlot);
        if (idx < 0) {
            return;
        }
        List<AEStorageCellCache.CellEntry> entries = cache.getStoredItems();
        if (idx >= entries.size()) {
            return;
        }
        AEStorageCellCache.CellEntry target = entries.get(idx);

        int outputSlot = findEmptyOutputSlot(menu);
        if (outputSlot < 0) {
            player.sendMessage(Lang.getString("messages.ae.converter.output_full"));
            return;
        }

        AEStorageCellType cellType = AEStorageCellType.fromAmount(per);
        NetworkQuantumStorage qs = cellType == null ? null : AECellUpgradeMaterialRegistry.getStorage(cellType);
        if (qs == null) {
            player.sendMessage(Lang.getString("messages.ae.converter.no_matching_storage"));
            return;
        }
        ItemStack qsItem = AECellQuantumConverter.buildQuantumStorage(target.sample, target.amount, qs);

        menu.replaceExistingItem(outputSlot, qsItem);
        cache.takeItem(target.sample, target.amount);
        AECellPersistence.setCurrentPerTypeLimit(cellItem, AEStorageCell.getCurrentPerTypeLimit(cellItem) - 1);
        menu.replaceExistingItem(CELL_SLOT, cellItem);
        refresh(menu);

        if (Networks.getAeStorageDatabase() != null) {
            Networks.getAeStorageDatabase().saveAllAsync();
        }
    }

    private void importItem(@NotNull BlockMenu menu, @NotNull Player player, int displaySlot) {
        ItemStack cellItem = menu.getItemInSlot(CELL_SLOT);
        if (cellItem == null || !AEStorageCell.isStorageCell(cellItem)) {
            return;
        }
        long per = AEStorageCell.getPerTypeLimit(cellItem);
        AEStorageCellCache cache = AEStorageCell.loadCellCache(cellItem, per);

        int idx = displayIndex(menu, displaySlot);
        if (idx < 0) {
            return;
        }
        List<Integer> qsSlots = listQuantumSlots(menu);
        if (idx >= qsSlots.size()) {
            return;
        }
        int sourceSlot = qsSlots.get(idx);
        ItemStack qsItem = menu.getItemInSlot(sourceSlot);
        QuantumCache qc = Keys.getQuantumCache(qsItem.getItemMeta());
        if (qc == null || qc.getItemStack() == null || qc.getAmountLong() <= 0) {
            player.sendMessage(Lang.getString("messages.ae.converter.no_quantum_storage"));
            return;
        }
        NetworkQuantumStorage storage = getQuantumStorageOf(qsItem);
        if (storage == null) {
            player.sendMessage(Lang.getString("messages.ae.converter.tier_mismatch"));
            return;
        }

        ItemKey targetKey = new ItemKey(qc.getItemStack());
        long existing = cache.getAmount(targetKey);
        long amount = qc.getAmountLong();
        boolean brandNew = existing <= 0;
        long remaining = existing > 0 ? per - existing : per;
        long transferable = Math.min(amount, remaining);
        if (transferable <= 0) {
            player.sendMessage(Lang.getString("messages.ae.converter.item_capacity_full"));
            return;
        }
        if (brandNew && cache.getCurrentPerTypeLimit() >= cache.getMaxUnits()) {
            player.sendMessage(Lang.getString("messages.ae.converter.units_full"));
            return;
        }
        if (cache.isWhitelistEnabled() && !isInCellWhitelist(cache, qc.getItemStack())) {
            player.sendMessage(Lang.getString("messages.ae.converter.whitelist_rejected"));
            return;
        }

        boolean sameTier = storage == AECellUpgradeMaterialRegistry.getStorage(AEStorageCellType.fromAmount(per));
        boolean upgrade = brandNew && sameTier
            && amount <= remaining && cache.getCurrentPerTypeLimit() < cache.getMaxUnits();

        cache.pushItemLong(targetKey, transferable);
        if (upgrade) {
            AECellPersistence.setCurrentPerTypeLimit(cellItem, AEStorageCell.getCurrentPerTypeLimit(cellItem) + 1);
        }

        if (upgrade) {
            menu.replaceExistingItem(sourceSlot, null);
        } else if (amount - transferable <= 0) {
            menu.replaceExistingItem(sourceSlot, storage.getItem().clone());
        } else {
            long left = amount - transferable;
            menu.replaceExistingItem(sourceSlot,
                AECellQuantumConverter.buildQuantumStorage(qc.getItemStack(), left, storage));
        }

        menu.replaceExistingItem(CELL_SLOT, cellItem);
        refresh(menu);

        if (Networks.getAeStorageDatabase() != null) {
            Networks.getAeStorageDatabase().saveAllAsync();
        }
    }

    private int displayIndex(@NotNull BlockMenu menu, int displaySlot) {
        int page = pageCache.getOrDefault(menu.getLocation(), 0);
        for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
            if (DISPLAY_SLOTS[i] == displaySlot) {
                return page * ITEMS_PER_PAGE + i;
            }
        }
        return -1;
    }

    private static int findEmptyOutputSlot(@NotNull BlockMenu menu) {
        for (int slot : OUTPUT_SLOTS) {
            ItemStack onSlot = menu.getItemInSlot(slot);
            if (onSlot == null || onSlot.getType().isAir()) {
                return slot;
            }
        }
        return -1;
    }

    private static List<Integer> listQuantumSlots(@NotNull BlockMenu menu) {
        List<Integer> slots = new ArrayList<>();
        for (int slot : OUTPUT_SLOTS) {
            ItemStack onSlot = menu.getItemInSlot(slot);
            if (onSlot == null || onSlot.getType().isAir()) {
                continue;
            }
            QuantumCache qc = Keys.getQuantumCache(onSlot.getItemMeta());
            if (qc != null && qc.getAmountLong() > 0) {
                slots.add(slot);
            }
        }
        return slots;
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

    private static boolean isInCellWhitelist(@NotNull AEStorageCellCache cache, @NotNull ItemStack sample) {
        for (ItemStack template : cache.getWhitelist()) {
            if (StackUtils.itemsMatch(template, sample)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static NetworkQuantumStorage getQuantumStorageOf(@NotNull ItemStack item) {
        SlimefunItem sf = SlimefunItem.getByItem(item);
        return sf instanceof NetworkQuantumStorage storage ? storage : null;
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

    private void refresh(@NotNull BlockMenu menu) {
        boolean exporting = isExportMode(menu);
        ItemStack cellItem = menu.getItemInSlot(CELL_SLOT);

        menu.replaceExistingItem(MODE_SLOT, buildModeButton(exporting));

        if (cellItem == null || !AEStorageCell.isStorageCell(cellItem)) {
            menu.replaceExistingItem(CELL_SLOT, createCellSlotMarker());
            clearDisplay(menu);
            menu.replaceExistingItem(PREV, buildPageButton(false, false));
            menu.replaceExistingItem(NEXT, buildPageButton(false, true));
            menu.replaceExistingItem(CLOSE, buildCloseButton());
            return;
        }

        long per = AEStorageCell.getPerTypeLimit(cellItem);
        AEStorageCellCache cache = AEStorageCell.loadCellCache(cellItem, per);
        AEStorageCell.applyLore(cellItem, per, AEStorageCell.getCurrentPerTypeLimit(cellItem));
        menu.replaceExistingItem(CELL_SLOT, cellItem);

        if (exporting) {
            renderCellItems(menu, cache);
        } else {
            renderQuantumItems(menu);
        }

        menu.replaceExistingItem(PREV, buildPageButton(pageCache.getOrDefault(menu.getLocation(), 0) > 0, false));
        menu.replaceExistingItem(NEXT, buildPageButton(pageCache.getOrDefault(menu.getLocation(), 0) < maxPages(menu) - 1, true));
        menu.replaceExistingItem(CLOSE, buildCloseButton());
    }

    private void renderCellItems(@NotNull BlockMenu menu, @NotNull AEStorageCellCache cache) {
        List<AEStorageCellCache.CellEntry> entries = cache.getStoredItems();
        int page = normalizePage(menu, entries.size());
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
    }

    private void renderQuantumItems(@NotNull BlockMenu menu) {
        List<Integer> qsSlots = listQuantumSlots(menu);
        int page = normalizePage(menu, qsSlots.size());
        for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
            int index = page * ITEMS_PER_PAGE + i;
            if (index < qsSlots.size()) {
                ItemStack qsItem = menu.getItemInSlot(qsSlots.get(index));
                QuantumCache qc = qsItem == null ? null : Keys.getQuantumCache(qsItem.getItemMeta());
                if (qc != null && qc.getItemStack() != null && qc.getAmountLong() > 0) {
                    menu.replaceExistingItem(DISPLAY_SLOTS[i], buildDisplayItem(qc.getItemStack(), qc.getAmountLong()));
                    continue;
                }
            }
            menu.replaceExistingItem(DISPLAY_SLOTS[i], DISPLAY_PLACEHOLDER.clone());
        }
    }

    private int normalizePage(@NotNull BlockMenu menu, int total) {
        int page = pageCache.getOrDefault(menu.getLocation(), 0);
        int max = Math.max(1, (int) Math.ceil((double) total / ITEMS_PER_PAGE));
        if (page >= max) {
            page = Math.max(0, max - 1);
            pageCache.put(menu.getLocation(), page);
        }
        return page;
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
        if (!isExportMode(menu)) {
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
            lore.add(Lang.getString("messages.ae.converter.item_count", AENumberFormat.formatNumber(amount)));
            lore.add("");
            lore.add(Lang.getString("messages.ae.converter.transfer_hint"));
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    @NotNull
    private static ItemStack buildModeButton(boolean exporting) {
        ItemStack mode = new ItemStack(Material.REPEATER);
        mode.editMeta(meta -> {
            meta.setDisplayName(Lang.getString(exporting
                ? "messages.ae.converter.mode_transfer"
                : "messages.ae.converter.mode_import"));
            meta.setLore(List.of(Lang.getString("messages.ae.converter.mode_hint")));
        });
        return mode;
    }

    @NotNull
    private static ItemStack buildPageButton(boolean enabled, boolean next) {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            if (next) {
                meta.setDisplayName(enabled
                    ? Lang.getString("messages.ae.converter.next_page")
                    : Lang.getString("messages.ae.converter.last_page"));
            } else {
                meta.setDisplayName(enabled
                    ? Lang.getString("messages.ae.converter.prev_page")
                    : Lang.getString("messages.ae.converter.first_page"));
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
            meta.setDisplayName(Lang.getString("messages.ae.converter.close"));
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

    @NotNull
    private static ItemStack buildBorderIcon() {
        ItemStack border = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        border.editMeta(meta -> meta.setDisplayName(" "));
        return border;
    }

    @NotNull
    private static ItemStack buildSeparatorIcon() {
        ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        separator.editMeta(meta -> meta.setDisplayName(" "));
        return separator;
    }
}
