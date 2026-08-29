package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.cell;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.ae.constants.AECellUpgradeMaterialRegistry;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellPersistence;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AEStorageCellCache;
import com.ytdd9527.networksexpansion.implementation.machines.ae.item.AEStorageCell;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component.AECellLore;
import com.ytdd9527.networksexpansion.implementation.machines.ae.utils.AENumberFormat;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AECellMenu {

    // 菜单槽位
    public static final int[] MAIN_BACKGROUND = new int[]{1, 2, 3, 4, 5, 6, 7, 46, 47, 48, 49, 50, 51};
    public static final int[] LIST_SLOTS = new int[]{
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    public static final int PAGE_SIZE = LIST_SLOTS.length;
    public static final int PREV = 0;
    public static final int NEXT = 8;
    public static final int WHITELIST_BUTTON = 45;
    public static final int UPGRADE = 52;
    public static final int RENAME = 53;
    public static final int WHITELIST_TOGGLE = 4;
    public static final int WHITELIST_BACK = 49;
    public static final int[] WHITELIST_BACKGROUND = new int[]{1, 2, 3, 5, 6, 7, 45, 46, 47, 48, 50, 51, 52, 53};

    private static final Map<UUID, Integer> ITEM_PAGE = new HashMap<>();
    private static final Map<UUID, UUID> OPENING_CELL = new ConcurrentHashMap<>();
    private static final Map<UUID, ChestMenu> OPENING_MENUS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> RENAMING = new ConcurrentHashMap<>();

    private static final ItemStack ICON_BACK = Lang.getIcon("ae-back-button", Material.BARRIER);
    private static final ItemStack ICON_SETTING_SLOT = Lang.getIcon("ae-setting-slot", Material.GREEN_STAINED_GLASS_PANE);

    private AECellMenu() {
    }

    public static void markOpening(@NotNull UUID playerUuid, @NotNull UUID cellUuid) {
        OPENING_CELL.put(playerUuid, cellUuid);
    }

    public static void stopOpening(@NotNull UUID playerUuid) {
        UUID cellUuid = OPENING_CELL.remove(playerUuid);
        OPENING_MENUS.remove(playerUuid);
        if (cellUuid != null) {
            AEStorageCellCache cache = AEStorageCellCache.getActiveCaches().get(cellUuid);
            if (cache != null) {
                refreshHeldCell(playerUuid, cellUuid, cache);
            }
            removePage(cellUuid);
        }
    }

    @Nullable
    public static UUID getOpeningCell(@NotNull UUID playerUuid) {
        return OPENING_CELL.get(playerUuid);
    }

    private static boolean isOpenedByAnother(@NotNull UUID playerUuid, @NotNull UUID cellUuid) {
        for (Map.Entry<UUID, UUID> entry : OPENING_CELL.entrySet()) {
            if (entry.getValue().equals(cellUuid) && !entry.getKey().equals(playerUuid)) {
                return true;
            }
        }
        return false;
    }

    private static void refreshHeldCell(@NotNull UUID playerUuid, @NotNull UUID cellUuid, @NotNull AEStorageCellCache cache) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            return;
        }
        ItemStack cell = findCellInPlayer(player, cellUuid);
        if (cell == null) {
            return;
        }
        AEStorageCell.applyLore(cell, cache.getPerTypeLimit(), cache.getCurrentPerTypeLimit());
        updateHeldCell(player, cell);
    }

    @Nullable
    public static UUID getRenamingCell(@NotNull UUID playerUuid) {
        return RENAMING.get(playerUuid);
    }

    public static void stopRenaming(@NotNull UUID playerUuid) {
        RENAMING.remove(playerUuid);
    }

    public static void open(@NotNull Player player, @NotNull ItemStack cellItem) {
        UUID uuid = AEStorageCell.getOrCreateCellUUID(cellItem);
        initPages(uuid);
        openView(player, uuid);
    }

    private static void openView(@NotNull Player player, @NotNull UUID uuid) {
        if (isOpenedByAnother(player.getUniqueId(), uuid)) {
            player.sendMessage(Lang.getString("messages.ae.cell.menu.opened_by_another"));
            return;
        }

        AEStorageCellCache cache = getCache(uuid);
        if (cache == null) {
            return;
        }

        ChestMenu menu = new ChestMenu(Lang.getString("messages.ae.cell.menu.title_view"));
        menu.setPlayerInventoryClickable(true);

        for (int slot : MAIN_BACKGROUND) {
            menu.addItem(slot, ChestMenuUtils.getBackground(), (p, s, i, a) -> false);
        }

        menu.addItem(WHITELIST_BUTTON, whitelistButton(cache), (p, s, i, a) -> {
            openWhitelist(p, uuid);
            return false;
        });

        menu.addItem(UPGRADE, upgradeButton(cache), (p, s, i, a) -> {
            handleUpgradeClick(menu, p, uuid);
            menu.replaceExistingItem(WHITELIST_BUTTON, whitelistButton(cache));
            return false;
        });

        menu.addItem(RENAME, renameButton(cache), (p, s, i, a) -> {
            p.closeInventory();
            RENAMING.put(p.getUniqueId(), uuid);
            p.sendMessage(Lang.getString("messages.ae.cell.rename_prompt"));
            return false;
        });

        menu.setEmptySlotsClickable(false);

        renderItems(menu, uuid);
        markOpening(player.getUniqueId(), uuid);
        registerCloseHandler(menu, player.getUniqueId());
        menu.open(player);
    }

    private static void openWhitelist(@NotNull Player player, @NotNull UUID uuid) {
        AEStorageCellCache cache = getCache(uuid);
        if (cache == null) {
            return;
        }

        ChestMenu menu = new ChestMenu(Lang.getString("messages.ae.cell.menu.title_whitelist"));
        menu.setPlayerInventoryClickable(true);

        for (int slot : WHITELIST_BACKGROUND) {
            menu.addItem(slot, ChestMenuUtils.getBackground(), (p, s, i, a) -> false);
        }

        menu.addItem(WHITELIST_TOGGLE, toggleButton(cache), (p, s, i, a) -> {
            boolean enabled = !cache.isWhitelistEnabled();
            applyWhitelist(p, uuid, enabled, cache.getWhitelist());
            menu.replaceExistingItem(WHITELIST_TOGGLE, toggleButton(cache));
            renderWhitelist(menu, uuid);
            return false;
        });

        menu.addItem(WHITELIST_BACK, backButton(), (p, s, i, a) -> {
            openView(p, uuid);
            return false;
        });

        for (int slot : LIST_SLOTS) {
            menu.addMenuClickHandler(slot, (p, s, i, a) -> {
                handleWhitelistSlotClick(menu, p, slot, uuid);
                return false;
            });
        }

        menu.setEmptySlotsClickable(false);

        renderWhitelist(menu, uuid);
        registerCloseHandler(menu, player.getUniqueId());
        menu.open(player);
    }

    /**
     * 记录当前打开的菜单，关闭时只有“当前菜单”才触发 stopOpening，
     * 避免在视图菜单与白名单菜单之间切换时被中间的 close 事件误判。
     */
    private static void registerCloseHandler(@NotNull ChestMenu menu, @NotNull UUID playerUuid) {
        OPENING_MENUS.put(playerUuid, menu);
        menu.addMenuCloseHandler(p -> {
            if (OPENING_MENUS.get(playerUuid) == menu) {
                stopOpening(playerUuid);
            }
        });
    }

    private static void renderItems(@NotNull ChestMenu menu, @NotNull UUID uuid) {
        AEStorageCellCache cache = getCache(uuid);
        if (cache == null) {
            return;
        }

        List<Map.Entry<ItemStack, Long>> items = new ArrayList<>(cache.getAllItems().entrySet());
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / PAGE_SIZE));
        int page = Math.min(getItemPage(uuid), totalPages - 1);
        setItemPage(uuid, page);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, items.size());

        for (int i = 0; i < LIST_SLOTS.length; i++) {
            int slot = LIST_SLOTS[i];
            if (i < end - start) {
                Map.Entry<ItemStack, Long> entry = items.get(start + i);
                menu.replaceExistingItem(slot, displayItem(entry.getKey(), entry.getValue()));
            } else {
                menu.replaceExistingItem(slot, new ItemStack(Material.AIR));
            }
            menu.addMenuClickHandler(slot, (p, s, it, a) -> false);
        }

        menu.replaceExistingItem(PREV, pageButton(page > 0 ? Lang.getString("messages.ae.cell.menu.prev_page") : Lang.getString("messages.ae.cell.menu.first_page")));
        menu.addMenuClickHandler(PREV, (p, s, i, a) -> {
            int cur = getItemPage(uuid);
            if (cur > 0) {
                setItemPage(uuid, cur - 1);
                renderItems(menu, uuid);
            }
            return false;
        });

        menu.replaceExistingItem(NEXT, pageButton(page < totalPages - 1 ? Lang.getString("messages.ae.cell.menu.next_page") : Lang.getString("messages.ae.cell.menu.last_page")));
        menu.addMenuClickHandler(NEXT, (p, s, i, a) -> {
            if (page < totalPages - 1) {
                setItemPage(uuid, page + 1);
                renderItems(menu, uuid);
            }
            return false;
        });
    }

    private static void handleUpgradeClick(@NotNull ChestMenu menu, @NotNull Player player, @NotNull UUID uuid) {
        AEStorageCellCache cache = getCache(uuid);
        if (cache == null) {
            return;
        }
        if (cache.getMaxUnits() == Long.MAX_VALUE) {
            return;
        }
        long maxUnits = cache.getMaxUnits();
        if (cache.getCurrentPerTypeLimit() >= maxUnits) {
            player.sendMessage(Lang.getString("messages.ae.cell.upgrade_maxed", AENumberFormat.formatNumber(maxUnits)));
            return;
        }

        NetworkQuantumStorage required = AECellUpgradeMaterialRegistry.getUpgradeMaterial(cache.getPerTypeLimit());
        if (required == null) {
            player.sendMessage(Lang.getString("messages.ae.cell.upgrade_no_material"));
            return;
        }

        // 先确认元件还在玩家背包，再消耗升级材料
        ItemStack cell = findCellInPlayer(player, uuid);
        if (cell == null) {
            player.sendMessage(Lang.getString("messages.ae.cell.cell_not_in_inventory"));
            return;
        }

        if (!consumeUpgradeMaterial(player, required)) {
            player.sendMessage(Lang.getString("messages.ae.cell.upgrade_missing", ItemStackHelper.getDisplayName(required.getItem())));
            return;
        }

        long newCurrent = Math.min(maxUnits, cache.getCurrentPerTypeLimit() + 1);
        // 统一写入 item meta + 缓存
        AECellPersistence.setCurrentPerTypeLimit(cell, newCurrent);
        AECellLore.applySpecLore(cell, AECellPersistence.getPerTypeLimit(cell), newCurrent);
        updateHeldCell(player, cell);

        menu.replaceExistingItem(UPGRADE, upgradeButton(cache));
        player.sendMessage(Lang.getString("messages.ae.cell.upgrade_success", AENumberFormat.formatNumber(newCurrent), AENumberFormat.formatNumber(maxUnits)));
    }

    private static boolean consumeUpgradeMaterial(@NotNull Player player, @NotNull NetworkQuantumStorage required) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            SlimefunItem stackSf = SlimefunItem.getByItem(stack);
            if (stackSf == required && StackUtils.itemsMatch(required.getItem(), stack, true)) {
                if (!isEmptyQuantumStorage(stack)) {
                    continue;
                }
                stack.setAmount(stack.getAmount() - 1);
                player.getInventory().setItem(i, stack);
                return true;
            }
        }
        return false;
    }

    private static boolean isEmptyQuantumStorage(@NotNull ItemStack stack) {
        if (!stack.hasItemMeta()) {
            return true;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return true;
        }
        QuantumCache quantumCache = Keys.getQuantumCache(meta);
        return quantumCache == null
            || quantumCache.getItemStack() == null
            || quantumCache.getAmountLong() == 0;
    }

    private static void renderWhitelist(@NotNull ChestMenu menu, @NotNull UUID uuid) {
        AEStorageCellCache cache = getCache(uuid);
        if (cache == null) {
            return;
        }
        List<ItemStack> whitelist = cache.getWhitelist();
        for (int i = 0; i < LIST_SLOTS.length; i++) {
            int slot = LIST_SLOTS[i];
            if (i < whitelist.size()) {
                menu.replaceExistingItem(slot, whitelistSlotItem(whitelist.get(i)));
            } else {
                menu.replaceExistingItem(slot, settingSlotItem());
            }
        }
        menu.replaceExistingItem(WHITELIST_TOGGLE, toggleButton(cache));
        menu.replaceExistingItem(WHITELIST_BACK, backButton());
    }

    private static void handleWhitelistSlotClick(@NotNull ChestMenu menu, @NotNull Player player, int slot, @NotNull UUID uuid) {
        AEStorageCellCache cache = getCache(uuid);
        if (cache == null) {
            return;
        }
        int index = whitelistIndexForSlot(slot);
        if (index < 0) {
            return;
        }
        List<ItemStack> whitelist = cache.getWhitelist();
        ItemStack cursor = player.getItemOnCursor();
        boolean cursorAir = cursor == null || cursor.getType().isAir();

        if (index < whitelist.size()) {
            whitelist.remove(index);
            applyWhitelist(player, uuid, cache.isWhitelistEnabled(), whitelist);
        } else if (!cursorAir) {
            long limit = Math.min(cache.getCurrentPerTypeLimit(), LIST_SLOTS.length);
            if (whitelist.size() >= limit) {
                player.sendMessage(Lang.getString("messages.ae.cell.whitelist_limit", AENumberFormat.formatNumber(limit)));
            } else if (!inWhitelist(whitelist, cursor)) {
                if (AEStorageCell.isStorageCell(cursor) || StackUtils.isBlacklisted(cursor)) {
                    player.sendMessage(Lang.getString("messages.ae.cell.whitelist_not_allowed"));
                } else {
                    ItemStack template = cursor.clone();
                    template.setAmount(1);
                    whitelist.add(template);
                    applyWhitelist(player, uuid, cache.isWhitelistEnabled(), whitelist);
                }
            }
        }

        renderWhitelist(menu, uuid);
    }

    private static void applyWhitelist(@NotNull Player player, @NotNull UUID uuid, boolean enabled, @NotNull List<ItemStack> whitelist) {
        AEStorageCellCache cache = getCache(uuid);
        if (cache != null) {
            cache.updateWhitelist(enabled, whitelist);
        }

        ItemStack cell = findCellInPlayer(player, uuid);
        if (cell != null) {
            updateHeldCell(player, cell);
        }
    }

    private static boolean inWhitelist(@NotNull List<ItemStack> list, @NotNull ItemStack sample) {
        for (ItemStack item : list) {
            if (StackUtils.itemsMatch(item, sample)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static ItemStack findCellInPlayer(@NotNull Player player, @NotNull UUID uuid) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (matches(main, uuid)) {
            return main;
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (matches(off, uuid)) {
            return off;
        }
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (matches(item, uuid)) {
                return item;
            }
        }
        return null;
    }

    public static void updateHeldCell(@NotNull Player player, @NotNull ItemStack cell) {
        UUID cellUuid = AECellPersistence.getCellUUID(cell);
        if (cellUuid == null) {
            return;
        }
        if (matches(player.getInventory().getItemInMainHand(), cellUuid)) {
            player.getInventory().setItemInMainHand(cell);
        } else if (matches(player.getInventory().getItemInOffHand(), cellUuid)) {
            player.getInventory().setItemInOffHand(cell);
        } else {
            for (int i = 0; i < 36; i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (matches(item, cellUuid)) {
                    player.getInventory().setItem(i, cell);
                    break;
                }
            }
        }
        player.updateInventory();
    }

    private static boolean matches(@Nullable ItemStack item, @NotNull UUID uuid) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        UUID cellUuid = AECellPersistence.getCellUUID(item);
        return uuid.equals(cellUuid);
    }

    @Nullable
    private static AEStorageCellCache getCache(@NotNull UUID uuid) {
        return AEStorageCellCache.getActiveCaches().get(uuid);
    }

    private static int getItemPage(@NotNull UUID uuid) {
        return ITEM_PAGE.getOrDefault(uuid, 0);
    }

    private static void setItemPage(@NotNull UUID uuid, int page) {
        ITEM_PAGE.put(uuid, page);
    }

    private static void initPages(@NotNull UUID uuid) {
        ITEM_PAGE.put(uuid, 0);
    }

    private static void removePage(@NotNull UUID uuid) {
        ITEM_PAGE.remove(uuid);
    }

    private static int whitelistIndexForSlot(int slot) {
        for (int i = 0; i < LIST_SLOTS.length; i++) {
            if (LIST_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    // ---------- 图标构建 ----------

    @NotNull
    private static ItemStack displayItem(@NotNull ItemStack sample, long amount) {
        ItemStack item = sample.clone();
        item.setAmount(1);
        item.editMeta(meta -> {
            meta.setDisplayName(ItemStackHelper.getDisplayName(sample));
            meta.setLore(List.of(Lang.getString("messages.ae.cell.button.item_count", AENumberFormat.formatNumber(amount))));
        });
        return item;
    }

    @NotNull
    private static ItemStack pageButton(@NotNull String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        item.editMeta(meta -> meta.setDisplayName(name));
        return item;
    }

    @NotNull
    private static ItemStack backButton() {
        return ICON_BACK;
    }

    @NotNull
    private static ItemStack settingSlotItem() {
        return ICON_SETTING_SLOT;
    }

    @NotNull
    private static ItemStack whitelistButton(@NotNull AEStorageCellCache cache) {
        ItemStack item = new ItemStack(cache.isWhitelistEnabled() ? Material.LIME_DYE : Material.GRAY_DYE);
        item.editMeta(meta -> {
            meta.setDisplayName(Lang.getString("messages.ae.cell.button.whitelist"));
            List<String> lore = new ArrayList<>();
            lore.add(Lang.getString(cache.isWhitelistEnabled()
                ? "messages.ae.cell.button.whitelist_enabled"
                : "messages.ae.cell.button.whitelist_disabled"));
            boolean unlimited = cache.getMaxUnits() == Long.MAX_VALUE;
            lore.add(Lang.getString("messages.ae.cell.button.whitelist_count",
                cache.getWhitelist().size(), unlimited ? "∞" : AENumberFormat.formatNumber(cache.getCurrentPerTypeLimit())));
            lore.add(Lang.getString("messages.ae.cell.button.per_unit_capacity",
                unlimited ? "∞" : AENumberFormat.formatNumber(cache.getPerTypeLimit())));
            lore.add("");
            lore.add(Lang.getString("messages.ae.cell.button.open_whitelist"));
            meta.setLore(lore);
        });
        return item;
    }

    @NotNull
    private static ItemStack toggleButton(@NotNull AEStorageCellCache cache) {
        ItemStack item = new ItemStack(cache.isWhitelistEnabled() ? Material.GREEN_DYE : Material.RED_DYE);
        item.editMeta(meta -> {
            meta.setDisplayName(cache.isWhitelistEnabled()
                ? Lang.getString("messages.ae.cell.button.toggle_on")
                : Lang.getString("messages.ae.cell.button.toggle_off"));
            meta.setLore(List.of(Lang.getString("messages.ae.cell.button.toggle_hint")));
        });
        return item;
    }

    @NotNull
    private static ItemStack whitelistSlotItem(@NotNull ItemStack sample) {
        ItemStack item = sample.clone();
        item.setAmount(1);
        item.editMeta(meta -> {
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add(Lang.getString("messages.ae.cell.button.remove_item"));
            meta.setLore(lore);
        });
        return item;
    }

    @NotNull
    private static ItemStack upgradeButton(@NotNull AEStorageCellCache cache) {
        NetworkQuantumStorage required = AECellUpgradeMaterialRegistry.getUpgradeMaterial(cache.getPerTypeLimit());
        ItemStack item;
        if (required != null) {
            item = required.getItem().clone();
            item.setAmount(1);
        } else {
            item = new ItemStack(Material.BARRIER);
        }
        item.editMeta(meta -> {
            boolean unlimited = cache.getMaxUnits() == Long.MAX_VALUE;
            meta.setDisplayName(Lang.getString(unlimited
                ? "messages.ae.cell.button.no_upgrade_needed"
                : "messages.ae.cell.button.upgrade"));
            List<String> lore = new ArrayList<>();
            long maxUnits = cache.getMaxUnits();
            long currentUnits = Math.min(cache.getCurrentPerTypeLimit(), maxUnits);
            String unitText = unlimited ? "∞" : AENumberFormat.formatNumber(currentUnits);
            lore.add(Lang.getString("messages.ae.cell.button.per_unit_capacity", unlimited ? "∞" : AENumberFormat.formatNumber(cache.getPerTypeLimit())));
            lore.add(Lang.getString("messages.ae.cell.button.current_units", unitText, unlimited ? "∞" : AENumberFormat.formatNumber(maxUnits)));
            if (!unlimited) {
                lore.add(progressBar(currentUnits, maxUnits));
            }
            if (unlimited) {
                lore.add(Lang.getString("messages.ae.cell.button.unlimited_hint"));
            } else if (required != null) {
                lore.add(Lang.getString("messages.ae.cell.button.upgrade_material", ItemStackHelper.getDisplayName(required.getItem())));
                lore.add(Lang.getString("messages.ae.cell.button.upgrade_consume"));
            } else {
                lore.add(Lang.getString("messages.ae.cell.button.no_upgrade_material"));
                lore.add(Lang.getString("messages.ae.cell.button.raw_capacity", cache.getPerTypeLimit()));
            }
            meta.setLore(lore);
        });
        return item;
    }

    @NotNull
    private static String progressBar(long current, long max) {
        int total = 20;
        long filled = (max > 0) ? (current * total + max - 1) / max : 0L;
        filled = Math.min(total, Math.max(0L, filled));
        ChatColor fillColor = (filled >= total) ? ChatColor.GREEN : ChatColor.YELLOW;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < total; i++) {
            if (i < filled) {
                bar.append(fillColor).append("■");
            } else {
                bar.append(ChatColor.GRAY).append("·");
            }
        }
        return fillColor + bar.toString();
    }

    @NotNull
    private static ItemStack renameButton(@NotNull AEStorageCellCache cache) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        item.editMeta(meta -> {
            meta.setDisplayName(Lang.getString("messages.ae.cell.button.rename"));
            List<String> lore = new ArrayList<>();
            String customName = AECellPersistence.getCustomName(cache.getUuid());
            lore.add(Lang.getString("messages.ae.cell.button.rename_current", customName != null ? customName : Lang.getString("messages.ae.cell.button.rename_default")));
            lore.add("");
            lore.add(Lang.getString("messages.ae.cell.button.rename_hint"));
            meta.setLore(lore);
        });
        return item;
    }
}
