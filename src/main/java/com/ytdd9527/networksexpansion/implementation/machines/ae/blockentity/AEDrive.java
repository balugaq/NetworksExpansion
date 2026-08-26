package com.ytdd9527.networksexpansion.implementation.machines.ae.blockentity;

import com.balugaq.netex.utils.Lang;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellHandle;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive.AECellUniquenessManager;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive.AEDriveCellManager;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive.AEDriveStorage;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive.AEDriveWhitelistManager;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.drive.AEDriveWhitelist;
import com.ytdd9527.networksexpansion.implementation.machines.ae.utils.AENumberFormat;
import io.github.sefiraat.networks.Networks;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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

/**
 * AE 驱动器方块，用于存放存储元件。
 * <p>
 * 放置时记录放置者为 owner，并对 GUI 打开与方块拆除做权限控制：
 * 存在 owner 时，仅 owner 本人、owner 白名单成员或持有 bypass 权限者可通过；
 * 无 owner（旧方块或数据异常）时按通用规则放行，避免锁死。
 */
public class AEDrive extends SpecialSlimefunItem {

    public static final int[] CELL_SLOTS = new int[]{10, 11, 12, 19, 20, 21, 28, 29, 30};
    public static final int CELL_SLOT_COUNT = CELL_SLOTS.length;
    public static final int DISPLAY_SLOT = 4;
    public static final int ACCESS_SLOT = 13;
    public static final int[] MAIN_BACKGROUND_SLOTS = new int[]{
        0, 1, 2, 3, 5, 6, 7, 8, 9, 14, 15, 16, 17, 18, 22, 23, 24, 25, 26, 27, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    public static final int[] ITEM_DISPLAY_SLOTS = new int[]{
        9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42, 43, 44,
        45, 46, 47, 48, 49, 50, 51, 52, 53
    };
    public static final int ITEM_PAGE_PREVIOUS = 0;
    public static final int ITEM_PAGE_NEXT = 8;
    public static final int ITEM_RETURN = 4;
    public static final int ITEMS_PER_PAGE = ITEM_DISPLAY_SLOTS.length;
    public static final int[] ITEM_ACCESS_BACKGROUND_SLOTS = new int[]{1, 2, 3, 5, 6, 7};

    public static final String OWNER_KEY = "owner";
    public static final int WHITELIST_BUTTON_SLOT = 39;

    private static final Map<Location, UUID> OWNER_CACHE = new ConcurrentHashMap<>();

    private static final long DISPLAY_REFRESH_INTERVAL_MS = 1000L;

    private static final AEDriveStorage storage = new AEDriveStorage();

    private final Map<Location, Integer> itemAccessPageCache = new ConcurrentHashMap<>();
    private final Map<Location, Long> displayRefreshTimestamps = new ConcurrentHashMap<>();

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
    }

    @Override
    public void preRegister() {
        addItemHandler(new BlockPlaceHandler(false) {

            @Override
            public void onPlayerPlace(@NotNull BlockPlaceEvent event) {
                Location location = event.getBlock().getLocation();
                UUID ownerUuid = event.getPlayer().getUniqueId();
                OWNER_CACHE.put(location.clone(), ownerUuid);
                SlimefunBlockData blockData = StorageCacheUtils.getBlock(location);
                if (blockData != null) {
                    blockData.setData(OWNER_KEY, ownerUuid.toString());
                }
            }
        });
        addItemHandler(
            new BlockTicker() {

                @Override
                public boolean isSynchronized() {
                    return false;
                }

                @Override
                public void tick(@NotNull Block b, SlimefunItem item, SlimefunBlockData data) {
                    final Location location = b.getLocation();
                    final long now = System.currentTimeMillis();
                    // 原子地"检查间隔 + 保留时间戳"：并发异步 tick 也只会有一个拿到调度权
                    boolean shouldRefresh = displayRefreshTimestamps.compute(location, (k, last) -> {
                        if (last != null && now - last < DISPLAY_REFRESH_INTERVAL_MS) {
                            return last;
                        }
                        return now;
                    }) == now;
                    if (!shouldRefresh) {
                        return;
                    }
                    // isSynchronized=false：tick 在异步线程执行，菜单访问须回到主线程
                    Bukkit.getScheduler().runTask(Networks.getInstance(), () -> {
                        final BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
                        if (blockMenu == null || !blockMenu.hasViewer()) {
                            return;
                        }
                        updateMainDisplay(blockMenu);
                    });
                }
            },
            new BlockBreakHandler(false, false) {

                @Override
                public void onPlayerBreak(@NotNull BlockBreakEvent event, @NotNull ItemStack item, @NotNull List<ItemStack> drops) {
                    if (!canBreak(event.getPlayer(), event.getBlock())) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage(Lang.getString("messages.ae.drive.break_not_allowed"));
                        return;
                    }
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
                drawBackground(MAIN_BACKGROUND_SLOTS);
            }

            @Override
            public boolean canOpen(@NotNull Block block, @NotNull Player player) {
                if (player.hasPermission("slimefun.inventory.bypass") || player.isOp()) {
                    return true;
                }
                return canAccess(player, block.getLocation())
                        && AEDrive.this.canUse(player, false);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }

            @Override
            public void newInstance(@NotNull BlockMenu menu, @NotNull Block block) {
                getOwnerUuid(block.getLocation());
                setupMainMenuHandlers(menu, block);
                updateMainDisplay(menu);
            }
        };
    }

    private void onBreak(@NotNull BlockBreakEvent event) {
        final Location location = event.getBlock().getLocation();
        itemAccessPageCache.remove(location);
        displayRefreshTimestamps.remove(location);
        OWNER_CACHE.remove(location);
        AECellUniquenessManager.unregisterDrive(location);
        storage.invalidateCellCache(location);
        final BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
        if (blockMenu != null) {
            blockMenu.dropItems(location, CELL_SLOTS);
        }
    }

    public static void saveAllDriveCells() {
        Networks.getInstance().getLogger().info(Lang.getString("messages.ae.drive.saving"));
        if (Networks.getAeStorageDatabase() != null) {
            Networks.getAeStorageDatabase().saveAllAsync();
        }
        Networks.getInstance().getLogger().info(Lang.getString("messages.ae.drive.saved"));
    }

    @NotNull
    public Map<ItemStack, Long> getAllCellItems(@NotNull BlockMenu menu) {
        return storage.getAllCellItems(storage.getCells(menu));
    }

    private void setupMainMenuHandlers(@NotNull BlockMenu menu, @NotNull Block block) {
        menu.addMenuClickHandler(DISPLAY_SLOT, (p, s, i, a) -> false);

        menu.addMenuClickHandler(ACCESS_SLOT, (player, clickedSlot, item, action) -> {
            openItemAccessMenu(menu, block, player);
            return false;
        });

        menu.addMenuClickHandler(WHITELIST_BUTTON_SLOT, (player, clickedSlot, item, action) -> {
            AEDriveWhitelist.open(menu, block.getLocation(), player);
            return false;
        });

        menu.addMenuCloseHandler(p -> {
            storage.invalidateCellCache(menu.getLocation());
            AECellUniquenessManager.registerDrive(menu);
        });
        menu.addMenuOpeningHandler(player -> {
            for (int slot : CELL_SLOTS) {
                AEDriveCellManager.refreshCellLore(menu, slot);
            }
            AECellUniquenessManager.scanAndEjectDuplicates(menu, player);
        });
    }

    private void openItemAccessMenu(@NotNull BlockMenu driveMenu, @NotNull Block block, @NotNull Player player) {
        Location location = block.getLocation();
        itemAccessPageCache.put(location, 0);

        ChestMenu menu = new ChestMenu(Lang.getString("messages.ae.drive.preview_title"));
        menu.setPlayerInventoryClickable(true);

        for (int slot : ITEM_ACCESS_BACKGROUND_SLOTS) {
            menu.addItem(slot, ChestMenuUtils.getBackground(), (p, s, i, a) -> false);
        }

        renderBrowser(menu, driveMenu, location);
        menu.setEmptySlotsClickable(false);
        menu.open(player);
    }

    private void renderBrowser(@NotNull ChestMenu menu, @NotNull BlockMenu driveMenu, @NotNull Location location) {
        Map<ItemStack, Long> allItems = getAllCellItems(driveMenu);
        List<Map.Entry<ItemStack, Long>> itemList = new ArrayList<>(allItems.entrySet());

        int page = itemAccessPageCache.getOrDefault(location, 0);
        int totalPages = Math.max(1, (int) Math.ceil((double) itemList.size() / ITEMS_PER_PAGE));
        if (page >= totalPages) {
            page = totalPages - 1;
            itemAccessPageCache.put(location, page);
        }

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, itemList.size());

        for (int i = 0; i < ITEM_DISPLAY_SLOTS.length; i++) {
            int slot = ITEM_DISPLAY_SLOTS[i];
            if (i < end - start) {
                Map.Entry<ItemStack, Long> entry = itemList.get(start + i);
                menu.addItem(slot, displayItem(entry.getKey(), entry.getValue()));
            } else {
                menu.addItem(slot, new ItemStack(Material.AIR));
            }
            menu.addMenuClickHandler(slot, (p, s, it, a) -> false);
        }

        menu.addItem(ITEM_PAGE_PREVIOUS,
            pageButton(page > 0 ? Lang.getString("messages.ae.drive.prev_page") : Lang.getString("messages.ae.drive.first_page")));
        menu.addMenuClickHandler(ITEM_PAGE_PREVIOUS, (p, s, i, a) -> {
            int current = itemAccessPageCache.getOrDefault(location, 0);
            if (current > 0) {
                itemAccessPageCache.put(location, current - 1);
                renderBrowser(menu, driveMenu, location);
            }
            return false;
        });

        menu.addItem(ITEM_PAGE_NEXT,
            pageButton(page < totalPages - 1 ? Lang.getString("messages.ae.drive.next_page") : Lang.getString("messages.ae.drive.last_page")));
        menu.addMenuClickHandler(ITEM_PAGE_NEXT, (p, s, i, a) -> {
            int current = itemAccessPageCache.getOrDefault(location, 0);
            if (current < totalPages - 1) {
                itemAccessPageCache.put(location, current + 1);
                renderBrowser(menu, driveMenu, location);
            }
            return false;
        });

        menu.addItem(ITEM_RETURN, backButton());
        menu.addMenuClickHandler(ITEM_RETURN, (p, s, i, a) -> {
            BlockMenu dm = StorageCacheUtils.getMenu(location);
            if (dm != null) {
                dm.open(p);
            }
            return false;
        });
    }

    private void updateMainDisplay(@NotNull BlockMenu menu) {
        long totalStored = 0;
        int cellCount = 0;

        List<AECellHandle> handles = storage.getCells(menu);
        for (AECellHandle cell : handles) {
            totalStored += cell.getStoredCount();
            cellCount++;
        }

        Map<ItemStack, Long> aggregatedItems = storage.getAllCellItems(handles);

        menu.replaceExistingItem(DISPLAY_SLOT, buildDisplayItem(cellCount, totalStored, aggregatedItems));
        menu.replaceExistingItem(ACCESS_SLOT, buildAccessButton());
        menu.replaceExistingItem(WHITELIST_BUTTON_SLOT, AEDriveWhitelist.buildWhitelistButton(menu.getLocation()));
    }

    @NotNull
    private static ItemStack buildDisplayItem(int cellCount, long totalStored, Map<ItemStack, Long> aggregatedItems) {
        ItemStack display = new ItemStack(Material.PAPER);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(Lang.getString("messages.ae.drive.cell_count", cellCount, CELL_SLOT_COUNT));
            lore.add(Lang.getString("messages.ae.drive.total_items", AENumberFormat.formatNumber(totalStored)));
            lore.add(Lang.getString("messages.ae.drive.item_types", aggregatedItems.size()));
            lore.add("");

            int count = 0;
            for (Map.Entry<ItemStack, Long> entry : aggregatedItems.entrySet()) {
                if (count >= 8) {
                    lore.add(Lang.getString("messages.ae.drive.more_items"));
                    break;
                }
                lore.add(Lang.getString("messages.ae.drive.item_entry", ItemStackHelper.getDisplayName(entry.getKey()), AENumberFormat.formatNumber(entry.getValue())));
                count++;
            }

            meta.setLore(lore);
            meta.setDisplayName(Lang.getString("messages.ae.drive.display_name"));
            display.setItemMeta(meta);
        }
        return display;
    }

    @NotNull
    private static ItemStack buildAccessButton() {
        return Lang.getIcon("ae-drive-browse", Material.CHEST);
    }

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
    public static ItemStack backButton() {
        return Lang.getIcon("ae-back-button", Material.BARRIER);
    }

    @Nullable
    private static UUID parseUuid(@NotNull String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public static UUID getOwnerUuid(@NotNull Location location) {
        UUID cached = OWNER_CACHE.get(location);
        if (cached != null) {
            return cached;
        }
        String owner = StorageCacheUtils.getData(location, OWNER_KEY);
        UUID uuid = owner == null ? null : parseUuid(owner);
        if (uuid != null) {
            OWNER_CACHE.put(location.clone(), uuid);
        }
        return uuid;
    }

    private static boolean canAccess(@NotNull Player player, @NotNull Location location) {
        if (player.isOp() || player.hasPermission("slimefun.inventory.bypass")) {
            return true;
        }
        UUID ownerUuid = getOwnerUuid(location);
        return ownerUuid == null
            || ownerUuid.equals(player.getUniqueId())
            || AEDriveWhitelistManager.isWhitelisted(ownerUuid, player.getUniqueId());
    }

    private boolean canBreak(@NotNull Player player, @NotNull Block block) {
        if (player.hasPermission("slimefun.inventory.bypass") || player.isOp()) {
            return true;
        }
        if (!canUse(player, false)) {
            return false;
        }
        return canAccess(player, block.getLocation());
    }
}
