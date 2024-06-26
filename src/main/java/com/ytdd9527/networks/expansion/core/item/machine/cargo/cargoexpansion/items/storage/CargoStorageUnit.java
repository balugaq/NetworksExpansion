package com.ytdd9527.networks.expansion.core.item.machine.cargo.cargoexpansion.items.storage;


import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networks.expansion.core.item.AbstractMySlimefunItem;
import com.ytdd9527.networks.expansion.core.item.machine.cargo.cargoexpansion.data.DataStorage;
import com.ytdd9527.networks.expansion.core.item.machine.cargo.cargoexpansion.objects.ItemContainer;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

public class CargoStorageUnit extends AbstractMySlimefunItem {

    private static final Map<Location, StorageUnitData> storages = new HashMap<>();
    private static final Map<Location, TransportMode> transportModes = new HashMap<>();
    private static final Map<Location, CargoReceipt> cargoRecords = new HashMap<>();
    private static final Set<Location> locked = new HashSet<>();

    private static final Set<Location> voidExcesses = new HashSet<>();
    private static final int[] displaySlots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43,46,47,48,49,50,51,52};
    private static final int storageInfoSlot = 4;
    private static final NamespacedKey idKey = new NamespacedKey(Networks.getInstance(),"CONTAINER_ID");
    private final StorageUnitType sizeType;
    private final int[] border = {0,1,2,3,5,6,9,17,18,26,27,35,36,44,45,53};
    private final int voidModeSlot = 7;
    private final int lockModeSlot = 8;
    private static final ItemStack errorBorder = new CustomItemStack(Material.BARRIER, " ", " ", " ", " ");

    public CargoStorageUnit(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, StorageUnitType sizeType) {
        super(itemGroup, item, recipeType, recipe);

        this.sizeType = sizeType;

        new BlockMenuPreset(this.getId(), this.getItemName()) {
            @Override
            public void init() {
                for (int slot : border) {
                    addItem(slot, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
                }
                addItem(storageInfoSlot, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
                addItem(lockModeSlot, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
                addItem(voidModeSlot, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
            }

            @Override
            public void newInstance(@NotNull BlockMenu menu, @NotNull Block b) {
                Location l = b.getLocation();
                requestData(l, getContainerId(l));
                // Restore mode
                SlimefunBlockData blockData = StorageCacheUtils.getBlock(l);
                String modeStr = blockData.getData("transportMode");
                TransportMode mode = modeStr == null ? TransportMode.REJECT : TransportMode.valueOf(modeStr);
                transportModes.put(l, mode);
                if(blockData.getData("locked") != null) {
                    locked.add(l);
                    menu.replaceExistingItem(lockModeSlot, getContentLockItem(true));
                } else {
                    menu.replaceExistingItem(lockModeSlot, getContentLockItem(false));
                }

                if(blockData.getData("voidExcess") != null) {
                    voidExcesses.add(l);
                    menu.replaceExistingItem(voidModeSlot, getVoidExcessItem(true));
                } else {
                    menu.replaceExistingItem(voidModeSlot, getVoidExcessItem(false));
                }

                // Add lock mode switcher
                menu.addMenuClickHandler(lockModeSlot, (p, slot, item1, action) -> {
                    switchLock(menu, l);
                    return false;
                });

                menu.addMenuClickHandler(voidModeSlot, (p, slot, item1, action) -> {
                    switchVoidExcess(menu, l);
                    return false;
                });

                StorageUnitData data = storages.get(l);
                if (data != null) {
                    CargoReceipt receipt = cargoRecords.get(l);
                    if(receipt != null) {
                        update(l, receipt, true);
                    } else {
                        update(l, new CargoReceipt(data.getId(), 0, 0, data.getTotalAmount(), data.getStoredTypeCount(), data.getSizeType()), true);
                    }
                }
            }

            @Override
            public boolean canOpen(@NotNull Block b, @NotNull Player p) {
                return p.hasPermission("slimefun.inventory.bypass") || (canUse(p, false) && Slimefun.getProtectionManager().hasPermission(p, b, Interaction.INTERACT_BLOCK));
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }
        };

    }

    @Nullable
    public static StorageUnitData getStorageData(Location l) {
        return storages.get(l);
    }

    @NotNull
    public static TransportMode getTransportMode(Location l) {
        return transportModes.getOrDefault(l, TransportMode.REJECT);
    }

    public static void update(Location l, CargoReceipt receipt, boolean force) {
        BlockMenu menu = StorageCacheUtils.getMenu(l);
        if (menu != null && (force||menu.hasViewer())) {

            int maxEach = receipt.getSizeType().getEachMaxSize();

            // Update information
            menu.replaceExistingItem(storageInfoSlot, getStorageInfoItem(receipt.getContainerId(), receipt.getTypeCount(),receipt.getSizeType().getMaxItemCount(),maxEach, isLocked(l), isVoidExcess(l)));

            // Update item display
            List<ItemContainer> itemStored = storages.get(l).getStoredItems();
            for (int i = 0; i < displaySlots.length; i++) {
                if (i < itemStored.size()) {
                    ItemContainer each = itemStored.get(i);
                    menu.replaceExistingItem(displaySlots[i], getDisplayItem(each.getSample(), each.getAmount(), maxEach));
                } else {
                    menu.replaceExistingItem(displaySlots[i], errorBorder);
                }
            }
        }
    }

    @Override
    public void preRegister() {
        addItemHandler(new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(@NotNull BlockPlaceEvent e) {
                Location l = e.getBlock().getLocation();
                ItemStack itemInHand = e.getItemInHand();
                Player p = e.getPlayer();
                int id = getBoundId(itemInHand);
                if (id != -1) {
                    StorageUnitData data = DataStorage.getCachedStorageData(id).orElse(null);
                    if (data != null && data.isPlaced() && !l.equals(data.getLastLocation())){
                        // This container already exists and placed in another location
                        p.sendMessage(ChatColor.RED+"该容器已在其它位置存在！");
                        Location currLoc = data.getLastLocation();
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&',"&e"+currLoc.getWorld().getName()+" &7| &e"+currLoc.getBlockX()+"&7/&e"+currLoc.getBlockY()+"&7/&e"+currLoc.getBlockZ()+"&7;"));
                        e.setCancelled(true);
                        return;
                    }
                    // Request data
                    requestData(l, id);
                    // This prevent creative player from getting too many same id item
                    if (p.getGameMode() == GameMode.CREATIVE) {
                        itemInHand.setAmount(itemInHand.getAmount()-1);
                        p.getEquipment().setItem(e.getHand(), itemInHand);
                    }
                } else {
                    StorageUnitData data = DataStorage.createStorageUnitData(p, sizeType, l);
                    id = data.getId();
                    storages.put(l, data);
                }

                StorageUnitData cache = storages.get(l);
                BlockMenu menu = StorageCacheUtils.getMenu(l);
                if (menu != null) {
                    update(l, new CargoReceipt(id, 0, 0, cache.getTotalAmount(), cache.getStoredTypeCount(), cache.getSizeType()), true);
                    // 如果存在菜单，添加点击事件
                    addClickHandler(l);
                }

                // Save to block storage
                addBlockInfo(l, id);
            }
        });

        addItemHandler(new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(BlockBreakEvent e, ItemStack item, List<ItemStack> drops) {
                e.setCancelled(true);
                Block b = e.getBlock();
                Location l = b.getLocation();
                // Remove data cache
                StorageUnitData data = storages.remove(l);
                // Remove block
                Slimefun.getDatabaseManager().getBlockDataController().removeBlock(l);
                b.setType(Material.AIR);
                if(data != null) {
                    // Drop our custom drop
                    data.setPlaced(false);
                    b.getWorld().dropItemNaturally(l,bindId(getItem(), data.getId()));
                } else {
                    // Data not loaded, just drop with the stored one.
                    int id = getContainerId(l);
                    if (id != -1) {
                        // Ensure that the current location bound a container
                        DataStorage.setContainerStatus(id, false);
                        b.getWorld().dropItemNaturally(l,bindId(getItem(), id));
                    }
                }
                // Remove cache
                transportModes.remove(l);
                cargoRecords.remove(l);
                locked.remove(l);
            }
        });

        addItemHandler(new BlockTicker() {
            @Override
            public boolean isSynchronized() {
                return false;
            }

            @Override
            public void tick(Block block, SlimefunItem item, Config conf) {
                onTick(block);
            }
        });
    }

    private void performTickOperationAsync(@Nonnull Block block) {
        new BukkitRunnable() {
            @Override
            public void run() {
                onTick(block);
            }
        }.runTaskAsynchronously(Networks.getInstance());
    }

    private void onTick(@Nonnull Block block) {
        Location l = block.getLocation();
        StorageUnitData data = storages.get(l);
        if (data == null) {
            requestData(l, getContainerId(l));
            return;
        }
        for (ItemContainer each : data.getStoredItems()) {
            if (each.getAmount() == 0) {
                data.removeItem(each.getId());
            }
        }
        BlockMenu menu = StorageCacheUtils.getMenu(l);
        if (!l.equals(data.getLastLocation())) {
            ItemStack itemInBorder = menu.getItemInSlot(0);
            if (data.isPlaced() && itemInBorder != null) {
                menu.replaceExistingItem(storageInfoSlot, getLocationErrorItem(data.getId(), data.getLastLocation()));

                for (int slot : border) {
                    menu.replaceExistingItem(slot, errorBorder);
                }
                return;
            }
            // Not placed, update state
            data.setPlaced(true);
            data.setLastLocation(l);
        }
        if(menu.hasViewer()) {
            // Update display item
            CargoReceipt receipt = cargoRecords.get(l);
            if(receipt != null) {
                CargoStorageUnit.update(l, receipt, false);
            } else {
                CargoStorageUnit.update(l, new CargoReceipt(data.getId(), 0, 0, data.getTotalAmount(), data.getStoredTypeCount(), data.getSizeType()), false);
            }
        }
    }

    public static boolean canAddMoreType(Location l, ItemStack itemStack) {
        return storages.get(l).getStoredItems().size() < storages.get(l).getSizeType().getMaxItemCount() && !isLocked(l) && !CargoStorageUnit.contains(l, itemStack);
    }

    public static boolean contains(Location l, ItemStack itemStack) {
        for (ItemContainer each : storages.get(l).getStoredItems()) {
            if (StackUtils.itemsMatch(each.getSample(), itemStack)) {
                return true;
            }
        }
        return false;
    }

    public static int getBoundId(@NotNull ItemStack item) {
        // Get meta
        ItemMeta meta = item.getItemMeta();
        int id = -1;
        // Check if has bound id
        if (meta != null && meta.getPersistentDataContainer().has(idKey, PersistentDataType.INTEGER)) {
            id = meta.getPersistentDataContainer().get(idKey, PersistentDataType.INTEGER);
        }
        return id;
    }

    public static ItemStack bindId(@NotNull ItemStack itemSample, int id) {
        ItemStack item = itemSample.clone();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        lore.add(ChatColor.BLUE+"已绑定容器ID: "+ChatColor.YELLOW+id);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.INTEGER, id);
        item.setItemMeta(meta);
        return item;
    }

    public static void addBlockInfo(Location l, int id) {
        // Save id
        StorageCacheUtils.setData(l, "containerId", String.valueOf(id));
        // Save mode
        setMode(l, TransportMode.ACCEPT);
    }

    public static boolean isLocked(Location l) {
        return locked.contains(l);
    }

    public static boolean isVoidExcess(Location l) {
        return voidExcesses.contains(l);
    }

    private static ItemStack getDisplayItem(ItemStack item, int amount, int max) {
        if (item == null) {
            return errorBorder;
        }
        try {
            return new CustomItemStack(item, (String) null, "", "&b存储数量: &e" + amount + " &7/ &6" + max);
        } catch (NullPointerException e) {
            return item.clone();
        }
    }

    public static void requestData(Location l, int id) {
        if (id == -1) return;
        if (DataStorage.isContainerLoaded(id)) {
            DataStorage.getCachedStorageData(id).ifPresent(data -> {
                if (data != null) {
                    storages.put(l, data);
                } else {
                    DataStorage.requestStorageData(id);
                }
            });
        } else {
            DataStorage.requestStorageData(id);
        }
        addClickHandler(l);
    }

    private static void addClickHandler(Location l) {
        BlockMenu blockMenu = StorageCacheUtils.getMenu(l);
        StorageUnitData data = storages.get(l);
        // 遍历每一个显示槽
        for (int s : displaySlots) {
            // 添加点击事件
            blockMenu.addMenuClickHandler(s, (player, slot, clickItem, action) -> {
                ItemStack itemOnCursor = player.getItemOnCursor();
                if (StackUtils.itemsMatch(clickItem, errorBorder)) {
                    // 如果点击的是空白
                    if (itemOnCursor != null && itemOnCursor.getType() != Material.AIR) {
                        ItemStack clone = itemOnCursor.clone();
                        data.depositItemStack(clone, false);
                        if (clone.getAmount() != 0) {
                            // 如果存储可以增加更多类型
                            if (canAddMoreType(l, clone)) {
                                // 存储光标上的物品
                                data.depositItemStack(clone, true);
                            }
                        }
                        // 更新玩家光标上的物品
                        player.setItemOnCursor(clone);
                        // 数据更新
                        CargoStorageUnit.update(l, new CargoReceipt(data.getId(), itemOnCursor.getAmount(), 0, data.getTotalAmount(), data.getStoredTypeCount(), data.getSizeType()), true);
                    }
                } else {
                    // 如果点击的不是空白
                    int camount = 1;
                    if (action.isRightClicked()) {
                        camount = 64;
                    }

                    // 获取实际物品
                    int index = -1;
                    for (int i = 0; i < displaySlots.length; i++) {
                        if (displaySlots[i] == slot) {
                            index = i;
                            break;
                        }
                    }
                    if (index == -1) {
                        return false;
                    }
                    ItemStack clone = storages.get(l).getStoredItems().get(index).getSample().clone();


                    camount = Math.min(camount, clone.getMaxStackSize());
                    if (action.isShiftClicked()) {
                        // 如果Shift，加到背包里

                        // 取出物品
                        ItemStack requestingStack = data.requestItem(new ItemRequest(clone, camount));
                        if (requestingStack == null) {
                            return false;
                        }

                        HashMap<Integer, ItemStack> remnant = player.getInventory().addItem(requestingStack);
                        requestingStack = remnant.values().stream().findFirst().orElse(null);
                        if (requestingStack != null) {
                            data.depositItemStack(requestingStack, false);
                        }
                    } else {
                        // 没有Shift，放到光标上

                        // 取出物品
                        ItemStack requestingStack = data.requestItem(new ItemRequest(clone, camount));
                        if (requestingStack == null) {
                            return false;
                        }

                        if (itemOnCursor.getType() == Material.AIR) {
                            // 如果光标是空气，直接替换就行
                            player.setItemOnCursor(requestingStack.clone());
                        } else if (StackUtils.itemsMatch(requestingStack, itemOnCursor)) {
                            // 如果不是空气并且物品相同
                            ItemStack cursorClone = itemOnCursor.clone();
                            int cursorAmount = cursorClone.getAmount();
                            int takeAmount = requestingStack.getAmount();
                            if (cursorAmount + takeAmount <= cursorClone.getMaxStackSize()) {
                                // 可以直接存储
                                cursorClone.setAmount(cursorAmount + takeAmount);
                            } else {
                                // 超过最大容量，只取最大容量
                                cursorClone.setAmount(cursorClone.getMaxStackSize());
                                // 设置剩余的数量
                                requestingStack.setAmount(takeAmount - (cursorClone.getMaxStackSize() - cursorAmount));
                                // 数据更新
                                data.depositItemStack(requestingStack, false);
                            }
                            // 更新玩家光标上的物品
                            player.setItemOnCursor(cursorClone);
                        }
                    }
                }
                return false;
            });
        }
    }

    private static ItemStack getStorageInfoItem(int id, int typeCount, int maxType, int maxEach, boolean locked, boolean voidExcess) {
        return new CustomItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "&c存储信息", "",
                "&b容器ID: &a"+id,
                "&b物品种类: &e"+typeCount+" &7/ &6"+maxType,
                "&b容量上限: &e"+maxType+" &7* &6"+maxEach,
                "&b内容锁定模式: " + (locked ? (ChatColor.DARK_GREEN + "\u2714") : (ChatColor.DARK_RED + "\u2718")),
                "&b满载清空模式: " + (voidExcess ? (ChatColor.DARK_GREEN + "\u2714") : (ChatColor.DARK_RED + "\u2718"))
        );
    }

    private static void setMode(Location l, TransportMode mode) {
        transportModes.put(l, mode);
        StorageCacheUtils.setData(l, "transportMode", mode.name());
    }

    private ItemStack getLocationErrorItem(int id, Location lastLoc) {
        return new CustomItemStack(Material.REDSTONE_TORCH, "&c位置错误", "",
                "&e这个容器已在其它位置存在",
                "&e请不要将同ID的容器放在多个不同的位置",
                "&e如果您认为这是个意外，请联系管理员处理",
                "",
                "&6容器信息:",
                "&b容器ID: &a"+ id,
                "&b所在世界: &e"+ lastLoc.getWorld().getName(),
                "&b所在坐标: &e"+ lastLoc.getBlockX() + " &7/ &e" + lastLoc.getBlockY() + " &7/ &e" + lastLoc.getBlockZ()
        );
    }

    private int getContainerId(Location l) {
        String str = StorageCacheUtils.getData(l, "containerId");
        return str == null ? -1 : Integer.parseInt(str);
    }

    private void switchLock(BlockMenu menu, Location l) {
        if (locked.contains(l)) {
            StorageCacheUtils.removeData(l, "locked");
            locked.remove(l);
            menu.replaceExistingItem(lockModeSlot, getContentLockItem(false));
        } else {
            StorageCacheUtils.setData(l, "locked", "enable");
            locked.add(l);
            menu.replaceExistingItem(lockModeSlot, getContentLockItem(true));
        }
    }

    private void switchVoidExcess(BlockMenu menu, Location l) {
        if (voidExcesses.contains(l)) {
            StorageCacheUtils.removeData(l, "voidExcess");
            voidExcesses.remove(l);
            menu.replaceExistingItem(voidModeSlot, getVoidExcessItem(false));
        } else {
            StorageCacheUtils.setData(l, "voidExcess", "enable");
            voidExcesses.add(l);
            menu.replaceExistingItem(voidModeSlot, getVoidExcessItem(true));
        }
    }

    private ItemStack getContentLockItem(boolean locked) {
        return new CustomItemStack(
                locked ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE,
                "&6内容锁定模式",
                "",
                "&b状态: " + (locked ? "&c已锁定" : "&a未锁定"),
                "",
                "&7当容器锁定后，将仅允许当前存在的物品输入",
                "&7并且输出时将会留下至少一个物品",
                locked ? "&e点击禁用" : "&e点击启用"
        );
    }

    private ItemStack getVoidExcessItem(boolean voidExcess) {
        return new CustomItemStack(
                voidExcess ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE,
                "&6满载清空模式",
                "",
                "&b状态: " + (voidExcess ? "&c已锁定" : "&a未锁定"),
                "",
                "&7开启此模式后，超过存储上限的物品的数量不会再增加，但仍能存入",
                voidExcess ? "&e点击禁用" : "&e点击启用"
        );
    }

    public static void putRecord(Location l, CargoReceipt receipt) {
        cargoRecords.put(l, receipt);
    }
}
