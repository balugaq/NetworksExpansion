package io.github.sefiraat.networks.network;

import com.balugaq.netex.api.data.ItemContainer;
import com.balugaq.netex.api.data.ItemFlowRecord;
import com.balugaq.netex.api.data.StorageUnitData;
import com.balugaq.netex.api.enums.FeedbackType;
import com.balugaq.netex.api.enums.StorageType;
import com.balugaq.netex.api.events.NetworkRootLocateStorageEvent;
import com.balugaq.netex.api.interfaces.FeedbackSendable;
import com.balugaq.netex.utils.BlockMenuUtil;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive.CellDrive;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive.DriveCache;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ItemHashMap;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ItemKey;
import com.ytdd9527.networksexpansion.implementation.machines.networks.advanced.AdvancedGreedyBlock;
import com.ytdd9527.networksexpansion.implementation.machines.unit.NetworksDrawer;
import io.github.mooy1.infinityexpansion.items.storage.StorageCache;
import io.github.mooy1.infinityexpansion.items.storage.StorageUnit;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.network.barrel.FluffyBarrel;
import io.github.sefiraat.networks.network.barrel.InfinityBarrel;
import io.github.sefiraat.networks.network.barrel.NetworkStorage;
import io.github.sefiraat.networks.network.stackcaches.BarrelIdentity;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.slimefun.network.NetworkCell;
import io.github.sefiraat.networks.slimefun.network.NetworkDirectional;
import io.github.sefiraat.networks.slimefun.network.NetworkGreedyBlock;
import io.github.sefiraat.networks.slimefun.network.NetworkPowerNode;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import io.github.sefiraat.networks.utils.MatchOption;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.ncbpfluffybear.fluffymachines.items.Barrel;
import lombok.Getter;
import lombok.Setter;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Warning;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class NetworkRoot extends NetworkNode {
    public static final int persistentThreshold = Networks.getConfigManager().getPersistentThreshold();
    public static final int cacheMissThreshold = Networks.getConfigManager().getCacheMissThreshold();
    public static final int reduceMs = Networks.getConfigManager().getReduceMs();
    public static final int transportMissThreshold = Networks.getConfigManager().getTransportMissThreshold();
    public static final Map<Location, Map<Location, Integer /* Access times */>> observingAccessHistory =
        new ConcurrentHashMap<>();
    public static final Map<Location, Map<Location, Integer /* Cache miss times */>> persistentAccessHistory =
        new ConcurrentHashMap<>();
    public static final Map<Location, Integer /* Transport miss times */> transportMissInputHistory =
        new ConcurrentHashMap<>();
    public static final Map<Location, Integer /* Transport miss times */> transportMissOutputHistory =
        new ConcurrentHashMap<>();
    public static final Map<Location, Long> controlledAccessInputHistory = new ConcurrentHashMap<>();
    public static final Map<Location, Long> controlledAccessOutputHistory = new ConcurrentHashMap<>();
    @Getter
    private final long CREATED_TIME = System.currentTimeMillis();
    @Getter
    private final Set<Location> nodeLocations = ConcurrentHashMap.newKeySet();
    public static final int[] CELL_AVAILABLE_SLOTS =
        NetworkCell.SLOTS.stream().mapToInt(i -> i).toArray();
    public static final int[] GREEDY_BLOCK_AVAILABLE_SLOTS = new int[]{NetworkGreedyBlock.INPUT_SLOT};
    public static final int[] ADVANCED_GREEDY_BLOCK_AVAILABLE_SLOTS = AdvancedGreedyBlock.INPUT_SLOTS;

    @Getter
    private final Set<Location>
        bridges = ConcurrentHashMap.newKeySet(),
        monitors = ConcurrentHashMap.newKeySet(),
        importers = ConcurrentHashMap.newKeySet(),
        exporters = ConcurrentHashMap.newKeySet(),
        grids = ConcurrentHashMap.newKeySet(),
        cells = ConcurrentHashMap.newKeySet(),
        grabbers = ConcurrentHashMap.newKeySet(),
        pushers = ConcurrentHashMap.newKeySet(),
        purgers = ConcurrentHashMap.newKeySet(),
        crafters = ConcurrentHashMap.newKeySet(),
        powerNodes = ConcurrentHashMap.newKeySet(),
        powerDisplays = ConcurrentHashMap.newKeySet(),
        encoders = ConcurrentHashMap.newKeySet(),
        greedyBlocks = ConcurrentHashMap.newKeySet(),
        cutters = ConcurrentHashMap.newKeySet(),
        pasters = ConcurrentHashMap.newKeySet(),
        vacuums = ConcurrentHashMap.newKeySet(),
        wirelessTransmitters = ConcurrentHashMap.newKeySet(),
        wirelessReceivers = ConcurrentHashMap.newKeySet(),
        powerOutlets = ConcurrentHashMap.newKeySet(),
        transferPushers = ConcurrentHashMap.newKeySet(),
        transferGrabbers = ConcurrentHashMap.newKeySet(),
        transfers = ConcurrentHashMap.newKeySet(),
        advancedImporters = ConcurrentHashMap.newKeySet(),
        advancedExporters = ConcurrentHashMap.newKeySet(),
        advancedGreedyBlocks = ConcurrentHashMap.newKeySet(),
        advancedPurgers = ConcurrentHashMap.newKeySet(),
        advancedVacuums = ConcurrentHashMap.newKeySet(),
        lineTransferVanillaPushers = ConcurrentHashMap.newKeySet(),
        lineTransferVanillaGrabbers = ConcurrentHashMap.newKeySet(),
        inputOnlyMonitors = ConcurrentHashMap.newKeySet(),
        outputOnlyMonitors = ConcurrentHashMap.newKeySet(),
        linePowerOutlets = ConcurrentHashMap.newKeySet(),
        decoders = ConcurrentHashMap.newKeySet(),
        quantumManagers = ConcurrentHashMap.newKeySet(),
        drawerManagers = ConcurrentHashMap.newKeySet(),
        crafterManagers = ConcurrentHashMap.newKeySet(),
        itemFlowViewers = ConcurrentHashMap.newKeySet(),
        advancedWirelessTransmitters = ConcurrentHashMap.newKeySet(),
        aeSwitchers = ConcurrentHashMap.newKeySet(),
        itemDifferenters = ConcurrentHashMap.newKeySet(),
        storageCardConverters = ConcurrentHashMap.newKeySet(),
        facingPresetters = ConcurrentHashMap.newKeySet(),
        visualGrids = ConcurrentHashMap.newKeySet();
    @Deprecated
    private final boolean progressing = false;
    @Getter
    private final int maxNodes;
    @Getter
    private final boolean recordFlow;
    @Getter
    private final @Nullable ItemFlowRecord itemFlowRecord;
    private int cellsSize = -1;
    @Getter
    private @Nullable Location controller = null;

    @Getter
    private boolean isOverburdened = false;

    @Deprecated
    private @Nullable Set<BarrelIdentity> barrels = null;

    private @Nullable Set<BarrelIdentity> inputAbleBarrels = null;
    private @Nullable Set<BarrelIdentity> outputAbleBarrels = null;

    @Deprecated
    private @Nullable Map<StorageUnitData, Location> cargoStorageUnitDatas = null;

    private @Nullable Map<StorageUnitData, Location> inputAbleCargoStorageUnitDatas = null;
    private @Nullable Map<StorageUnitData, Location> outputAbleCargoStorageUnitDatas = null;
    private volatile @Nullable Map<Location, BarrelIdentity> mapInputAbleBarrels = null;
    private volatile @Nullable Map<Location, BarrelIdentity> mapOutputAbleBarrels = null;
    private volatile @Nullable Map<Location, StorageUnitData> mapInputAbleCargoStorageUnits = null;
    private volatile @Nullable Map<Location, StorageUnitData> mapOutputAbleCargoStorageUnits = null;
    private @Nullable Set<BlockMenu> cellDriveMenus = null;
    private @Nullable Set<BlockMenu> inputAbleCellDriveMenus = null;
    private @Nullable Set<BlockMenu> outputAbleCellDriveMenus = null;
    private @Nullable Set<BlockMenu> greedyBlockMenus = null;
    private @Nullable Set<BlockMenu> advancedGreedyBlockMenus = null;
    private @Nullable Set<BlockMenu> cellMenus = null;
    private final DriveCache driveCache = new DriveCache();

    /**
     * 聚合物品快照按网络（控制器位置）静态共享：root 实例每刻由 NetworkController 重建，
     * 实例级缓存无法跨刻复用，空闲网络（无写入）借此零重扫。失效条件：
     * 同刻——仅显式失效（{@link #markDirty()} / {@link #refreshRootItems()}）重建，
     * 刻内普通写沿用既有"至多 1 刻延迟"契约（写纪元不影响同刻复用）；
     * 跨刻——期间无任何写（写纪元不变）且未超 {@link #SNAPSHOT_MAX_AGE_MS} 才复用。
     * 时间上限兜底绕过 root 的直改存储（抽屉/单元 GUI 手改、区块加载、结构变更）——陈旧度至多 500ms。
     */
    private static final Map<Location, ItemSnapshot> SHARED_ITEM_SNAPSHOTS = new ConcurrentHashMap<>();
    private static final Map<Location, AtomicLong> INVALIDATION_EPOCHS = new ConcurrentHashMap<>();
    private static final Map<Location, AtomicLong> WRITE_EPOCHS = new ConcurrentHashMap<>();
    private static final long SNAPSHOT_MAX_AGE_MS = 500L;
    private static final long SNAPSHOT_IDLE_MS = 3_600_000L;

    /**
     * 不可变快照：内容与失效判定依据作为整体经 CHM 原子发布（单引用读），
     * 拆成多个字段会让读线程看到"新纪元 + 旧数据"的组合。
     * keyed 为内部聚合的原始视图（发布后不再变更），供 keyed 消费方零哈希查询；
     * items 为 keyed 的 ItemStack 出口视图，仅首个消费方需要时构建并缓存（keyed-only 消费零克隆）。
     */
    private static final class ItemSnapshot {
        private final @NotNull Map<ItemKey, Long> keyed;
        private final long invalidationEpoch;
        private final long writeEpoch;
        private final int tick;
        private final long publishedAtMs;
        private volatile @Nullable Map<ItemStack, Long> exported;

        private ItemSnapshot(
                @NotNull Map<ItemKey, Long> keyed,
                long invalidationEpoch,
                long writeEpoch,
                int tick,
                long publishedAtMs) {
            this.keyed = keyed;
            this.invalidationEpoch = invalidationEpoch;
            this.writeEpoch = writeEpoch;
            this.tick = tick;
            this.publishedAtMs = publishedAtMs;
        }

        private long invalidationEpoch() {
            return invalidationEpoch;
        }

        private long writeEpoch() {
            return writeEpoch;
        }

        private int tick() {
            return tick;
        }

        private long publishedAtMs() {
            return publishedAtMs;
        }

        private @NotNull Map<ItemKey, Long> keyed() {
            return keyed;
        }

        private @NotNull Map<ItemStack, Long> items() {
            Map<ItemStack, Long> items = exported;
            if (items != null) {
                return items;
            }
            synchronized (this) {
                items = exported;
                if (items == null) {
                    items = new HashMap<>(keyed.size() * 2);
                    for (Map.Entry<ItemKey, Long> entry : keyed.entrySet()) {
                        items.put(entry.getKey().getItemStack(), entry.getValue());
                    }
                    exported = items;
                }
                return items;
            }
        }
    }

    @Setter
    @Getter
    private volatile long rootPower = 0;

    @Setter
    @Getter
    private boolean displayParticles = false;

    public NetworkRoot(@NotNull Location location, @NotNull NodeType type, int maxNodes) {
        this(location, type, maxNodes, false, null);
    }

    public NetworkRoot(
        @NotNull Location location,
        @NotNull NodeType type,
        int maxNodes,
        boolean recordFlow,
        @Nullable ItemFlowRecord itemFlowRecord) {
        super(location, type);
        this.maxNodes = maxNodes;
        this.root = this;
        this.recordFlow = recordFlow;
        this.itemFlowRecord = itemFlowRecord;

        registerNode(location, type);
    }

    public static void addPersistentAccessHistory(Location location, Location accessLocation) {
        Map<Location, Integer> locations = persistentAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
        locations.put(accessLocation, 0);
        persistentAccessHistory.put(location, locations);
    }

    public static void addCacheMiss(Location location, Location accessLocation) {
        Map<Location, Integer> locations = persistentAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
        int value = locations.getOrDefault(accessLocation, 0) + 1;
        if (value > cacheMissThreshold) {
            removePersistentAccessHistory(location, accessLocation);
            return;
        }
        locations.put(accessLocation, value);
        persistentAccessHistory.put(location, locations);
    }

    public static void minusCacheMiss(Location location, Location accessLocation) {
        Map<Location, Integer> locations = persistentAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
        int value = Math.max(locations.getOrDefault(accessLocation, 0) - 1, 0);
        locations.put(accessLocation, value);
    }

    public static Map<Location, Integer> getPersistentAccessHistory(Location location) {
        return persistentAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
    }

    public static void removePersistentAccessHistory(Location location) {
        persistentAccessHistory.remove(location);
    }

    public static void removePersistentAccessHistory(Location location, Location accessLocation) {
        Map<Location, Integer> locations = persistentAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
        locations.remove(accessLocation);
        persistentAccessHistory.put(location, locations);
    }

    public static void addCountObservingAccessHistory(Location location, Location accessLocation) {
        Map<Location, Integer> locations = observingAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
        Integer count = locations.getOrDefault(accessLocation, 0);
        if (count >= persistentThreshold) {
            removeCountObservingAccessHistory(location, accessLocation);
            addPersistentAccessHistory(location, accessLocation);
            return;
        }
        locations.put(accessLocation, count + 1);
        observingAccessHistory.put(location, locations);
    }

    public static Map<Location, Integer> getCountObservingAccessHistory(Location location) {
        return observingAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
    }

    public static void removeCountObservingAccessHistory(Location location) {
        observingAccessHistory.remove(location);
    }

    public static void removeCountObservingAccessHistory(Location location, Location accessLocation) {
        Map<Location, Integer> locations = observingAccessHistory.getOrDefault(location, new ConcurrentHashMap<>());
        locations.remove(accessLocation);
        observingAccessHistory.put(location, locations);
    }

    @Nullable
    public static InfinityBarrel getInfinityBarrel(@NotNull BlockMenu blockMenu, @NotNull StorageUnit storageUnit) {
        return getInfinityBarrel(blockMenu, storageUnit, false);
    }

    @Nullable
    public static InfinityBarrel getInfinityBarrel(
        @NotNull BlockMenu blockMenu, @NotNull StorageUnit storageUnit, boolean includeEmpty) {
        final ItemStack itemStack = blockMenu.getItemInSlot(16);
        final SlimefunBlockData data = StorageCacheUtils.getBlock(blockMenu.getLocation());
        if (data == null) {
            return null;
        }
        final String storedString = data.getData("stored");

        if (storedString == null) {
            return null;
        }

        final int storedInt = Integer.parseInt(storedString);

        if (!includeEmpty && (itemStack == null || itemStack.getType() == Material.AIR)) {
            return null;
        }

        final StorageCache cache = storageUnit.getCache(blockMenu.getLocation());

        if (cache == null) {
            return null;
        }

        final ItemStack clone;
        if (itemStack == null) {
            clone = null;
        } else {
            clone = itemStack.clone();
            clone.setAmount(1);
        }

        return new InfinityBarrel(
            blockMenu.getLocation(), clone, storedInt + (itemStack == null ? 0 : itemStack.getAmount()), cache);
    }

    @Nullable
    public static FluffyBarrel getFluffyBarrel(@NotNull BlockMenu blockMenu, @NotNull Barrel barrel) {
        return getFluffyBarrel(blockMenu, barrel, false);
    }

    @Nullable
    public static FluffyBarrel getFluffyBarrel(
        @NotNull BlockMenu blockMenu, @NotNull Barrel barrel, boolean includeEmpty) {
        Block block = blockMenu.getBlock();
        ItemStack itemStack;
        try {
            itemStack = barrel.getStoredItem(block);
        } catch (NullPointerException ignored) {
            return null;
        }

        if (!includeEmpty && (itemStack == null || itemStack.getType() == Material.AIR)) {
            return null;
        }

        final ItemStack clone;
        if (itemStack == null) {
            clone = null;
        } else {
            clone = itemStack.clone();
            clone.setAmount(1);
        }

        int stored = barrel.getStored(block);

        if (stored <= 0) {
            return null;
        }
        int limit = barrel.getCapacity(block);
        boolean voidExcess = Boolean.parseBoolean(StorageCacheUtils.getData(blockMenu.getLocation(), "trash"));

        return new FluffyBarrel(blockMenu.getLocation(), clone, stored, limit, voidExcess);
    }

    @Nullable
    public static NetworkStorage getNetworkStorage(@NotNull BlockMenu blockMenu) {
        return getNetworkStorage(blockMenu, false);
    }

    @Nullable
    public static NetworkStorage getNetworkStorage(@NotNull BlockMenu blockMenu, boolean includeEmpty) {

        final QuantumCache cache = NetworkQuantumStorage.getCaches().get(blockMenu.getLocation());

        if (cache == null) {
            return null;
        }

        final ItemStack itemStack = cache.getItemStack();
        if ((itemStack == null || itemStack.getType() == Material.AIR) && !includeEmpty) {
            return null;
        }

        final ItemStack output = blockMenu.getItemInSlot(NetworkQuantumStorage.OUTPUT_SLOT);
        long storedInt = cache.getAmountLong();
        if (output != null && output.getType() != Material.AIR && StackUtils.itemsMatch(cache, output)) {
            storedInt = storedInt + output.getAmount();
        }

        final ItemStack clone;

        if (itemStack != null) {
            clone = itemStack.clone();
            clone.setAmount(1);
        } else {
            clone = null;
        }

        return new NetworkStorage(blockMenu.getLocation(), clone, storedInt);
    }

    @Nullable
    public static BarrelIdentity getBarrel(@NotNull Location barrelLocation) {
        return getBarrel(barrelLocation, false);
    }

    @Nullable
    public static BarrelIdentity getBarrel(@NotNull Location barrelLocation, boolean includeEmpty) {
        SlimefunItem item = StorageCacheUtils.getSfItem(barrelLocation);
        BlockMenu menu = StorageCacheUtils.getMenu(barrelLocation);
        if (menu == null) {
            return null;
        }

        if (item instanceof NetworkQuantumStorage) {
            return getNetworkStorage(menu, includeEmpty);
        } else if (Networks.getSupportedPluginManager().isFluffyMachines() && item instanceof Barrel barrel) {
            return getFluffyBarrel(menu, barrel, includeEmpty);
        } else if (Networks.getSupportedPluginManager().isInfinityExpansion() && item instanceof StorageUnit storageUnit) {
            return getInfinityBarrel(menu, storageUnit, includeEmpty);
        } else {
            return null;
        }
    }

    @Nullable
    public static StorageUnitData getCargoStorageUnitData(@NotNull BlockMenu blockMenu) {
        return NetworksDrawer.getStorageData(blockMenu.getLocation());
    }

    @Nullable
    public static StorageUnitData getCargoStorageUnitData(@NotNull Location location) {
        return NetworksDrawer.getStorageData(location);
    }

    public void registerNode(@NotNull Location location, @NotNull NodeType type) {
        nodeLocations.add(location);
        switch (type) {
            case CONTROLLER -> this.controller = location;
            case BRIDGE -> bridges.add(location);
            case STORAGE_MONITOR -> monitors.add(location);
            case IMPORT -> importers.add(location);
            case EXPORT -> exporters.add(location);
            case GRID -> grids.add(location);
            case CELL -> {
                /*
                 * Fix https://github.com/Sefiraat/Networks/issues/211
                 */
                BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
                if (blockMenu == null) {
                    return;
                }
                if (StorageCacheUtils.getSfItem(location) instanceof NetworkCell) {
                    cells.add(location);
                }
            }
            case GRABBER -> grabbers.add(location);
            case PUSHER -> pushers.add(location);
            case PURGER -> purgers.add(location);
            case CRAFTER -> crafters.add(location);
            case POWER_NODE -> powerNodes.add(location);
            case POWER_DISPLAY -> powerDisplays.add(location);
            case ENCODER -> encoders.add(location);
            case GREEDY_BLOCK -> {
                /*
                 * Fix https://github.com/Sefiraat/Networks/issues/211
                 */
                BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
                if (blockMenu == null) {
                    return;
                }
                if (StorageCacheUtils.getSfItem(location) instanceof NetworkGreedyBlock) {
                    greedyBlocks.add(location);
                }
            }
            case CUTTER -> cutters.add(location);
            case PASTER -> pasters.add(location);
            case VACUUM -> vacuums.add(location);
            case WIRELESS_TRANSMITTER -> wirelessTransmitters.add(location);
            case WIRELESS_RECEIVER -> wirelessReceivers.add(location);
            case POWER_OUTLET -> powerOutlets.add(location);
            // from networks expansion
            case ADVANCED_IMPORT -> advancedImporters.add(location);
            case ADVANCED_EXPORT -> advancedExporters.add(location);
            case ADVANCED_GREEDY_BLOCK -> {
                /*
                 * Fix https://github.com/Sefiraat/Networks/issues/211
                 */
                BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
                if (blockMenu == null) {
                    return;
                }
                if (StorageCacheUtils.getSfItem(location) instanceof AdvancedGreedyBlock) {
                    advancedGreedyBlocks.add(location);
                }
            }
            case ADVANCED_PURGER -> advancedPurgers.add(location);
            case ADVANCED_VACUUM -> advancedVacuums.add(location);
            case TRANSFER -> transfers.add(location);
            case TRANSFER_PUSHER -> transferPushers.add(location);
            case TRANSFER_GRABBER -> transferGrabbers.add(location);
            case LINE_TRANSFER_VANILLA_GRABBER -> lineTransferVanillaGrabbers.add(location);
            case LINE_TRANSFER_VANILLA_PUSHER -> lineTransferVanillaPushers.add(location);
            case INPUT_ONLY_MONITOR -> inputOnlyMonitors.add(location);
            case OUTPUT_ONLY_MONITOR -> outputOnlyMonitors.add(location);
            case LINE_POWER_OUTLET -> linePowerOutlets.add(location);
            case DECODER -> decoders.add(location);
            case QUANTUM_MANAGER -> quantumManagers.add(location);
            case DRAWER_MANAGER -> drawerManagers.add(location);
            case CRAFTER_MANAGER -> crafterManagers.add(location);
            case FLOW_VIEWER -> itemFlowViewers.add(location);
            case ADVANCED_WIRELESS_TRANSMITTER -> advancedWirelessTransmitters.add(location);
            case ITEM_DIFFERENTER -> itemDifferenters.add(location);
            case STORAGE_CARD_CONVERTER -> storageCardConverters.add(location);
            case FACING_PRESETTER -> facingPresetters.add(location);
            case VISUAL_GRID -> visualGrids.add(location);
        }
    }

    public int getNodeCount() {
        return this.nodeLocations.size();
    }

    /** 显式失效纪元：markDirty / refreshRootItems 递增，同刻与跨刻复用都会校验。 */
    private void bumpInvalidationEpoch() {
        INVALIDATION_EPOCHS.computeIfAbsent(this.nodePosition, k -> new AtomicLong()).incrementAndGet();
    }

    /** 写纪元：物品存取（addItemStack0 / getItemStack0）递增，仅影响跨刻复用。 */
    private void bumpWriteEpoch() {
        WRITE_EPOCHS.computeIfAbsent(this.nodePosition, k -> new AtomicLong()).incrementAndGet();
    }

    private static long currentInvalidationEpoch(@NotNull Location networkId) {
        AtomicLong counter = INVALIDATION_EPOCHS.get(networkId);
        return counter == null ? 0L : counter.get();
    }

    private static long currentWriteEpoch(@NotNull Location networkId) {
        AtomicLong counter = WRITE_EPOCHS.get(networkId);
        return counter == null ? 0L : counter.get();
    }

    /**
     * 外部代码绕过 {@link #addItemStack0(Location, ItemStack)} / {@link #getItemStack0(Location, ItemRequest)}
     * 直接改动网络存储内容（如机器菜单槽位、元件驱动器内部缓存）后，必须调用本方法使聚合物品缓存失效。
     */
    public void markDirty() {
        bumpInvalidationEpoch();
    }

    public void setOverburdened(boolean overburdened) {
        this.isOverburdened = overburdened;
    }

    // for display needs, don't operate items based on the return value!
    @NotNull
    public Map<ItemStack, Long> getAllNetworkItemsLongTypeView() {
        return getAllNetworkItemsLongType();
    }

    /**
     * 返回网络全量物品的聚合快照。快照按网络静态共享：
     * 同刻内——仅显式失效（{@link #refreshRootItems()} / {@link #markDirty()}）重建，
     * 普通物品写沿用既有"至多 1 刻延迟"契约；
     * 跨刻——期间无任何写（写纪元不变）且距发布不超过 500ms 时直接复用（空闲网络零重扫），
     * 否则重建。绕过 root 的直改存储（GUI 手改、区块加载、结构变更）由时间上限兜底。
     * <p>
     * 注意：返回的是<b>共享缓存实例</b>，调用方不得修改（含 entrySet/keySet 视图）；
     * 需要独立副本请自行 clone。
     */
    @NotNull
    public Map<ItemStack, Long> getAllNetworkItemsLongType() {
        return obtainItemSnapshot().items();
    }

    /**
     * 聚合物品的 keyed 只读视图（{@link ItemKey} 精确身份，amount 归一为 1 的模板）：
     * 与 {@link #getAllNetworkItemsLongType()} 同一份快照、同一套失效校验，
     * 键的哈希在聚合时已算好——查询零重复深度哈希。返回共享实例，不得修改。
     */
    @NotNull
    public Map<ItemKey, Long> getAllNetworkItemsKeyedView() {
        return obtainItemSnapshot().keyed();
    }

    @NotNull
    private ItemSnapshot obtainItemSnapshot() {
        final Location networkId = this.nodePosition;
        // 读取顺序：先快照、后纪元——若两步之间发生失效递增，会判定不命中而重扫（fail-safe）
        final ItemSnapshot snapshot = SHARED_ITEM_SNAPSHOTS.get(networkId);
        final long invalidationEpoch = currentInvalidationEpoch(networkId);
        final long writeEpoch = currentWriteEpoch(networkId);
        final int currentTick = Bukkit.getCurrentTick();
        if (snapshot != null) {
            if (snapshot.tick == currentTick) {
                if (snapshot.invalidationEpoch == invalidationEpoch) {
                    return snapshot;
                }
            } else if (snapshot.invalidationEpoch == invalidationEpoch
                && snapshot.writeEpoch == writeEpoch
                && System.currentTimeMillis() - snapshot.publishedAtMs <= SNAPSHOT_MAX_AGE_MS) {
                return snapshot;
            }
        }

        final ItemHashMap<Long> itemStacks = new ItemHashMap<>();

        // Barrels
        for (BarrelIdentity barrelIdentity : getOutputAbleBarrels()) {
            addKeyedAmount(itemStacks, barrelIdentity.getItemStack(), barrelIdentity.getAmount());
        }

        // Cargo storage units
        Map<StorageUnitData, Location> cacheMap = getOutputAbleCargoStorageUnitDatas();
        for (StorageUnitData cache : cacheMap.keySet()) {
            for (ItemContainer itemContainer : cache.getStoredItems()) {
                addKeyedAmount(itemStacks, itemContainer.getSample(), itemContainer.getAmount());
            }
        }

        for (BlockMenu blockMenu : getAdvancedGreedyBlockMenus()) {
            ItemStack template = blockMenu.getItemInSlot(AdvancedGreedyBlock.TEMPLATE_SLOT);
            if (template == null || template.getType() == Material.AIR) continue;
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            for (int slot : slots) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                final ItemStack identity = itemStack == null || itemStack.getType() == Material.AIR ? template : itemStack;
                addKeyedAmount(itemStacks, identity, itemStack);
            }
        }

        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            ItemStack template = blockMenu.getItemInSlot(NetworkGreedyBlock.TEMPLATE_SLOT);
            if (template == null || template.getType() == Material.AIR) continue;
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            final ItemStack itemStack = blockMenu.getItemInSlot(slots[0]);
            final ItemStack identity = itemStack == null || itemStack.getType() == Material.AIR ? template : itemStack;
            addKeyedAmount(itemStacks, identity, itemStack);
        }

        for (BlockMenu blockMenu : getCrafterOutputs()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            for (int slot : slots) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    addKeyedAmount(itemStacks, itemStack, itemStack);
                }
            }
        }

        for (BlockMenu blockMenu : getCellMenus()) {
            if (!isRealCell(blockMenu)) continue;
            for (int slot : CELL_AVAILABLE_SLOTS) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    addKeyedAmount(itemStacks, itemStack, itemStack);
                }
            }
        }

        // Cell Drives：直取 keyed 视图，零身份构造合并
        ItemHashMap<Long> cellItems = CellDrive.getStorage().getAllCellItemsKeyed(driveCache, getOutputAbleCellDriveMenus());
        for (Map.Entry<ItemKey, Long> entry : cellItems.keyEntrySet()) {
            ItemKey key = entry.getKey();
            Long current = itemStacks.getKey(key);
            itemStacks.putKey(key, (current == null ? 0L : current) + entry.getValue());
        }

        // 发布快照：用读取开始前捕获的纪元打戳——扫描中途发生的失效递增会让本快照立即过期重扫；
        // ItemStack 出口视图惰性构建（见 ItemSnapshot.items）
        ItemSnapshot published = new ItemSnapshot(
            itemStacks.keyedView(), invalidationEpoch, writeEpoch, currentTick, System.currentTimeMillis());
        SHARED_ITEM_SNAPSHOTS.put(networkId, published);
        evictStaleSnapshotNetworks();
        return published;
    }

    /** 内部聚合：以精确身份（保留全部 meta，仅数量归一）为键累计数量，每条目一次深度哈希。 */
    private static void addKeyedAmount(@NotNull ItemHashMap<Long> all, @Nullable ItemStack key, long amount) {
        if (key == null || key.getType() == Material.AIR) return;
        ItemKey itemKey = ItemKey.exact(key);
        Long current = all.getKey(itemKey);
        all.putKey(itemKey, (current == null ? 0L : current) + amount);
    }

    private static void addKeyedAmount(@NotNull ItemHashMap<Long> all, @Nullable ItemStack key, @Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        addKeyedAmount(all, key, item.getAmount());
    }

    /** 清理长期无发布的网络快照与纪元条目，防止网络拆除后残留。 */
    private static void evictStaleSnapshotNetworks() {
        if (SHARED_ITEM_SNAPSHOTS.size() <= 1) {
            return;
        }
        long now = System.currentTimeMillis();
        SHARED_ITEM_SNAPSHOTS.entrySet().removeIf(entry -> {
            if (now - entry.getValue().publishedAtMs() > SNAPSHOT_IDLE_MS) {
                INVALIDATION_EPOCHS.remove(entry.getKey());
                WRITE_EPOCHS.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    // fallback api, don't remove
    public @NotNull Map<ItemStack, Integer> getAllNetworkItems() {
        return getAllNetworkItemsLongType().entrySet()
            .stream()
            .map(e -> Map.entry(e.getKey(), e.getValue() > Integer.MAX_VALUE ? Integer.MAX_VALUE : e.getValue().intValue()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a
            ));
    }

    @Deprecated
    @NotNull
    public Set<BarrelIdentity> getBarrels() {

        if (this.barrels != null) {
            return this.barrels;
        }

        final Set<Location> addedLocations = ConcurrentHashMap.newKeySet();
        final Set<BarrelIdentity> barrelSet = ConcurrentHashMap.newKeySet();

        for (Location cellLocation : this.monitors) {
            final BlockFace face = NetworkDirectional.getSelectedFace(cellLocation);

            if (face == null) {
                continue;
            }

            final Location testLocation = cellLocation.clone().add(face.getDirection());

            if (addedLocations.contains(testLocation)) {
                continue;
            } else {
                addedLocations.add(testLocation);
            }

            final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(testLocation);

            if (Networks.getSupportedPluginManager().isInfinityExpansion()
                && slimefunItem instanceof StorageUnit unit) {
                final BlockMenu menu = StorageCacheUtils.getMenu(testLocation);
                if (menu == null) {
                    continue;
                }
                final InfinityBarrel infinityBarrel = getInfinityBarrel(menu, unit);
                if (infinityBarrel != null) {
                    barrelSet.add(infinityBarrel);
                }
            } else if (Networks.getSupportedPluginManager().isFluffyMachines()
                && slimefunItem instanceof Barrel barrel) {
                final BlockMenu menu = StorageCacheUtils.getMenu(testLocation);
                if (menu == null) {
                    continue;
                }
                final FluffyBarrel fluffyBarrel = getFluffyBarrel(menu, barrel);
                if (fluffyBarrel != null) {
                    barrelSet.add(fluffyBarrel);
                }
            } else if (slimefunItem instanceof NetworkQuantumStorage) {
                final BlockMenu menu = StorageCacheUtils.getMenu(testLocation);
                if (menu == null) {
                    continue;
                }
                final NetworkStorage storage = getNetworkStorage(menu);
                if (storage != null) {
                    barrelSet.add(storage);
                }
            }
        }

        this.barrels = barrelSet;
        NetworkRootLocateStorageEvent event =
            new NetworkRootLocateStorageEvent(this, StorageType.BARREL, true, true, Bukkit.isPrimaryThread());
        Bukkit.getPluginManager().callEvent(event);
        return barrelSet;
    }

    @Deprecated
    @NotNull
    public Map<StorageUnitData, Location> getCargoStorageUnitDatas() {
        if (this.cargoStorageUnitDatas != null) {
            return this.cargoStorageUnitDatas;
        }

        final Set<Location> addedLocations = ConcurrentHashMap.newKeySet();
        final Map<StorageUnitData, Location> dataSet = new ConcurrentHashMap<>();

        for (Location cellLocation : this.monitors) {
            final BlockFace face = NetworkDirectional.getSelectedFace(cellLocation);

            if (face == null) {
                continue;
            }

            final Location testLocation = cellLocation.clone().add(face.getDirection());

            if (addedLocations.contains(testLocation)) {
                continue;
            } else {
                addedLocations.add(testLocation);
            }

            final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(testLocation);

            if (slimefunItem instanceof NetworksDrawer) {
                final StorageUnitData data = getCargoStorageUnitData(testLocation);
                if (data != null) {
                    dataSet.put(data, testLocation);
                }
            }
        }

        this.cargoStorageUnitDatas = dataSet;
        NetworkRootLocateStorageEvent event =
            new NetworkRootLocateStorageEvent(this, StorageType.DRAWER, true, true, Bukkit.isPrimaryThread());
        Bukkit.getPluginManager().callEvent(event);
        return dataSet;
    }

    @NotNull
    public Set<BlockMenu> getCellMenus() {
        if (this.cellMenus != null) {
            return this.cellMenus;
        }
        final Set<BlockMenu> menus = new HashSet<>();
        for (Location cellLocation : this.cells) {
            BlockMenu menu = StorageCacheUtils.getMenu(cellLocation);
            if (menu != null) {
                menus.add(menu);
            }
        }
        this.cellMenus = menus;
        return menus;
    }

    @NotNull
    public Set<BlockMenu> getCellDriveMenus() {
        if (this.cellDriveMenus != null) {
            return this.cellDriveMenus;
        }
        final Set<Location> monitor = new HashSet<>();
        monitor.addAll(this.inputOnlyMonitors);
        monitor.addAll(this.outputOnlyMonitors);
        monitor.addAll(this.monitors);
        this.cellDriveMenus = collectCellDriveMenus(monitor);
        return this.cellDriveMenus;
    }

    @NotNull
    public Set<BlockMenu> getInputAbleCellDriveMenus() {
        if (this.inputAbleCellDriveMenus != null) {
            return this.inputAbleCellDriveMenus;
        }
        final Set<Location> monitor = new HashSet<>();
        monitor.addAll(this.inputOnlyMonitors);
        monitor.addAll(this.monitors);
        this.inputAbleCellDriveMenus = collectCellDriveMenus(monitor);
        return this.inputAbleCellDriveMenus;
    }

    @NotNull
    public Set<BlockMenu> getOutputAbleCellDriveMenus() {
        if (this.outputAbleCellDriveMenus != null) {
            return this.outputAbleCellDriveMenus;
        }
        final Set<Location> monitor = new HashSet<>();
        monitor.addAll(this.outputOnlyMonitors);
        monitor.addAll(this.monitors);
        this.outputAbleCellDriveMenus = collectCellDriveMenus(monitor);
        return this.outputAbleCellDriveMenus;
    }

    @NotNull
    public DriveCache getDriveCache() {
        return this.driveCache;
    }

    @NotNull
    private Set<BlockMenu> collectCellDriveMenus(@NotNull Set<Location> monitors) {
        final Set<BlockMenu> menus = new HashSet<>();
        final Set<Location> addedLocations = ConcurrentHashMap.newKeySet();
        for (Location monitorLocation : monitors) {
            final BlockFace face = NetworkDirectional.getSelectedFace(monitorLocation);
            if (face == null) {
                continue;
            }
            final Location testLocation = monitorLocation.clone().add(face.getDirection());
            if (addedLocations.contains(testLocation)) {
                continue;
            }
            addedLocations.add(testLocation);
            final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(testLocation);
            if (slimefunItem instanceof CellDrive) {
                BlockMenu menu = StorageCacheUtils.getMenu(testLocation);
                if (menu != null) {
                    menus.add(menu);
                }
            }
        }
        return menus;
    }

    @NotNull
    public Set<BlockMenu> getCrafterOutputs() {
        final Set<BlockMenu> menus = new HashSet<>();
        for (Location location : this.crafters) {
            BlockMenu menu = StorageCacheUtils.getMenu(location);
            if (menu != null) {
                menus.add(menu);
            }
        }
        return menus;
    }

    @NotNull
    public Set<BlockMenu> getGreedyBlockMenus() {
        if (this.greedyBlockMenus != null) {
            return this.greedyBlockMenus;
        }
        final Set<BlockMenu> menus = new HashSet<>();
        for (Location location : this.greedyBlocks) {
            BlockMenu menu = StorageCacheUtils.getMenu(location);
            if (menu != null) {
                menus.add(menu);
            }
        }
        this.greedyBlockMenus = menus;
        return menus;
    }

    @NotNull
    public Set<BlockMenu> getAdvancedGreedyBlockMenus() {
        if (this.advancedGreedyBlockMenus != null) {
            return this.advancedGreedyBlockMenus;
        }
        final Set<BlockMenu> menus = new HashSet<>();
        for (Location location : this.advancedGreedyBlocks) {
            BlockMenu menu = StorageCacheUtils.getMenu(location);
            if (menu != null) {
                menus.add(menu);
            }
        }
        this.advancedGreedyBlockMenus = menus;
        return menus;
    }

    @Warning(
        reason =
            "This method is deprecated and will be removed in the future. Use getItemStack0(Location, ItemRequest) instead.")
    @Deprecated(forRemoval = true)
    @Nullable
    public ItemStack getItemStack(@NotNull ItemRequest request) {
        ItemStack stackToReturn = null;

        if (request.getAmount() <= 0) {
            return null;
        }

        // Barrels first
        for (BarrelIdentity barrelIdentity : getOutputAbleBarrels()) {

            final ItemStack itemStack = barrelIdentity.getItemStack();

            if (itemStack == null || !StackUtils.itemsMatch(request, itemStack)) {
                continue;
            }

            boolean infinity = barrelIdentity instanceof InfinityBarrel;
            final ItemStack fetched = barrelIdentity.requestItem(request);
            if (fetched == null || fetched.getType() == Material.AIR || (infinity && fetched.getAmount() == 1)) {
                continue;
            }

            // Stack is null, so we can fill it here
            if (stackToReturn == null) {
                stackToReturn = fetched.clone();
                stackToReturn.setAmount(0);
            }

            final int preserveAmount = infinity ? fetched.getAmount() - 1 : fetched.getAmount();

            if (request.getAmount() <= preserveAmount) {
                stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                fetched.setAmount(fetched.getAmount() - request.getAmount());
                return stackToReturn;
            } else {
                stackToReturn.setAmount(stackToReturn.getAmount() + preserveAmount);
                request.receiveAmount(preserveAmount);
                fetched.setAmount(fetched.getAmount() - preserveAmount);
            }
        }

        // Units
        for (StorageUnitData cache : getOutputAbleCargoStorageUnitDatas().keySet()) {
            ItemStack take = cache.requestItem(request);
            if (take != null) {
                if (stackToReturn == null) {
                    stackToReturn = take.clone();
                } else {
                    stackToReturn.setAmount(stackToReturn.getAmount() + take.getAmount());
                }
                request.receiveAmount(take.getAmount());

                if (request.getAmount() <= 0) {
                    return stackToReturn;
                }
            }
        }

        // Cells
        for (BlockMenu blockMenu : getCellMenus()) {
            if (!isRealCell(blockMenu)) continue;
            for (int slot : CELL_AVAILABLE_SLOTS) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null
                    || itemStack.getType() == Material.AIR
                    || !StackUtils.itemsMatch(request, itemStack)) {
                    continue;
                }

                // Mark the Cell as dirty otherwise the changes will not save on shutdown
                blockMenu.markDirty();

                // If the return stack is null, we need to set it up
                if (stackToReturn == null) {
                    stackToReturn = itemStack.clone();
                    stackToReturn.setAmount(0);
                }

                if (request.getAmount() <= itemStack.getAmount()) {
                    // We can't take more than this stack. Level to request amount, remove items and then return
                    stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                    itemStack.setAmount(itemStack.getAmount() - request.getAmount());
                    return stackToReturn;
                } else {
                    // We can take more than what is here, consume before trying to take more
                    stackToReturn.setAmount(stackToReturn.getAmount() + itemStack.getAmount());
                    request.receiveAmount(itemStack.getAmount());
                    itemStack.setAmount(0);
                }
            }
        }

        // Crafters
        for (BlockMenu blockMenu : getCrafterOutputs()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            for (int slot : slots) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null
                    || itemStack.getType() == Material.AIR
                    || !StackUtils.itemsMatch(request, itemStack)) {
                    continue;
                }

                // Stack is null, so we can fill it here
                if (stackToReturn == null) {
                    stackToReturn = itemStack.clone();
                    stackToReturn.setAmount(0);
                }

                if (request.getAmount() <= itemStack.getAmount()) {
                    stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                    itemStack.setAmount(itemStack.getAmount() - request.getAmount());
                    return stackToReturn;
                } else {
                    stackToReturn.setAmount(stackToReturn.getAmount() + itemStack.getAmount());
                    request.receiveAmount(itemStack.getAmount());
                    itemStack.setAmount(0);
                }
            }
        }

        for (BlockMenu blockMenu : getAdvancedGreedyBlockMenus()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            for (int slot : slots) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null
                    || itemStack.getType() == Material.AIR
                    || !StackUtils.itemsMatch(request, itemStack)) {
                    continue;
                }

                // Stack is null, so we can fill it here
                if (stackToReturn == null) {
                    stackToReturn = itemStack.clone();
                    stackToReturn.setAmount(0);
                }

                if (request.getAmount() <= itemStack.getAmount()) {
                    stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                    itemStack.setAmount(itemStack.getAmount() - request.getAmount());
                    return stackToReturn;
                } else {
                    stackToReturn.setAmount(stackToReturn.getAmount() + itemStack.getAmount());
                    request.receiveAmount(itemStack.getAmount());
                    itemStack.setAmount(0);
                }
            }
        }

        // Greedy Blocks
        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            final ItemStack itemStack = blockMenu.getItemInSlot(slots[0]);
            if (itemStack == null
                || itemStack.getType() == Material.AIR
                || !StackUtils.itemsMatch(request, itemStack)) {
                continue;
            }

            // Mark the Cell as dirty otherwise the changes will not save on shutdown
            blockMenu.markDirty();

            // If the return stack is null, we need to set it up
            if (stackToReturn == null) {
                stackToReturn = itemStack.clone();
                stackToReturn.setAmount(0);
            }

            if (request.getAmount() <= itemStack.getAmount()) {
                // We can't take more than this stack. Level to request amount, remove items and then return
                stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                itemStack.setAmount(itemStack.getAmount() - request.getAmount());
                return stackToReturn;
            } else {
                // We can take more than what is here, consume before trying to take more
                stackToReturn.setAmount(stackToReturn.getAmount() + itemStack.getAmount());
                request.receiveAmount(itemStack.getAmount());
                itemStack.setAmount(0);
            }
        }

        if (stackToReturn == null || stackToReturn.getAmount() == 0) {
            return null;
        }

        return stackToReturn;
    }

    public static void addAmount(Map<ItemStack, Long> all, ItemStack key, @Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        long current = all.getOrDefault(key, 0L);
        all.put(key, current + item.getAmount());
    }

    public static void addAmount(Map<ItemStack, Long> all, ItemStack key, long amount) {
        all.put(key, all.getOrDefault(key, 0L) + amount);
    }

    public boolean contains(@NotNull ItemStack itemStack) {
        return contains(new ItemRequest(itemStack, 1));
    }

    public boolean contains(@NotNull ItemRequest request) {

        long found = 0;

        // Barrels
        for (BarrelIdentity barrelIdentity : getOutputAbleBarrels()) {
            final ItemStack itemStack = barrelIdentity.getItemStack();

            if (itemStack == null || !StackUtils.itemsMatch(request, itemStack)) {
                continue;
            }

            if (barrelIdentity instanceof InfinityBarrel) {
                if (barrelIdentity.getItemStack().getMaxStackSize() > 1) {
                    found += barrelIdentity.getAmount() - 2;
                }
            } else {
                found += barrelIdentity.getAmount();
            }

            // Escape if found all we need
            if (found >= request.getAmount()) {
                return true;
            }
        }

        // Crafters
        for (BlockMenu blockMenu : getCrafterOutputs()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            for (int slot : slots) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null
                    || itemStack.getType() == Material.AIR
                    || !StackUtils.itemsMatch(request, itemStack)) {
                    continue;
                }

                found += itemStack.getAmount();

                // Escape if found all we need
                if (found >= request.getAmount()) {
                    return true;
                }
            }
        }

        Map<StorageUnitData, Location> cacheMap = getOutputAbleCargoStorageUnitDatas();
        for (StorageUnitData cache : cacheMap.keySet()) {
            final List<ItemContainer> storedItems = cache.getStoredItems();
            for (ItemContainer itemContainer : storedItems) {
                if (!StackUtils.itemsMatch(request, itemContainer.getItemStack())) {
                    continue;
                }

                int amount = itemContainer.getAmount();
                found += amount;

                // Escape if found all we need
                if (found >= request.getAmount()) {
                    return true;
                }
            }
        }

        for (BlockMenu blockMenu : getAdvancedGreedyBlockMenus()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            for (int slot : slots) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null
                    || itemStack.getType() == Material.AIR
                    || !StackUtils.itemsMatch(request, itemStack)) {
                    continue;
                }

                found += itemStack.getAmount();

                // Escape if found all we need
                if (found >= request.getAmount()) {
                    return true;
                }
            }
        }

        // Greedy Blocks
        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            final ItemStack itemStack = blockMenu.getItemInSlot(slots[0]);
            if (itemStack == null
                || itemStack.getType() == Material.AIR
                || !StackUtils.itemsMatch(request, itemStack)) {
                continue;
            }

            found += itemStack.getAmount();

            // Escape if found all we need
            if (found >= request.getAmount()) {
                return true;
            }
        }

        // Cells
        for (BlockMenu blockMenu : getCellMenus()) {
            if (!isRealCell(blockMenu)) continue;
            for (int slot : CELL_AVAILABLE_SLOTS) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null
                    || itemStack.getType() == Material.AIR
                    || !StackUtils.itemsMatch(request, itemStack)) {
                    continue;
                }

                found += itemStack.getAmount();

                // Escape if found all we need
                if (found >= request.getAmount()) {
                    return true;
                }
            }
        }

        // Cell Drives
        found += CellDrive.getStorage().getAmount(driveCache, getOutputAbleCellDriveMenus(), request.getItemStack());

        if (found >= request.getAmount()) {
            return true;
        }

        return false;
    }

    public int getAmount(@NotNull ItemStack itemStack) {
        long totalAmount = 0;
        for (BlockMenu blockMenu : getAdvancedGreedyBlockMenus()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            for (int slot : slots) {
                final ItemStack inputSlotItem = blockMenu.getItemInSlot(slot);
                if (inputSlotItem != null && StackUtils.itemsMatch(inputSlotItem, itemStack)) {
                    totalAmount += inputSlotItem.getAmount();
                }
            }
        }

        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            ItemStack inputSlotItem = blockMenu.getItemInSlot(slots[0]);
            if (inputSlotItem != null && StackUtils.itemsMatch(inputSlotItem, itemStack)) {
                totalAmount += inputSlotItem.getAmount();
            }
        }

        for (BarrelIdentity barrelIdentity : getOutputAbleBarrels()) {
            if (StackUtils.itemsMatch(barrelIdentity, itemStack)) {
                totalAmount += barrelIdentity.getAmount();
                if (barrelIdentity instanceof InfinityBarrel) {
                    totalAmount -= 2;
                }
            }
        }
        Map<StorageUnitData, Location> cacheMap = getOutputAbleCargoStorageUnitDatas();
        for (StorageUnitData cache : cacheMap.keySet()) {
            final List<ItemContainer> storedItems = cache.getStoredItems();
            for (ItemContainer itemContainer : storedItems) {
                if (StackUtils.itemsMatch(itemContainer, itemStack)) {
                    totalAmount += itemContainer.getAmount();
                }
            }
        }

        for (BlockMenu blockMenu : getCellMenus()) {
            if (!isRealCell(blockMenu)) continue;
            for (int slot : CELL_AVAILABLE_SLOTS) {
                final ItemStack cellItem = blockMenu.getItemInSlot(slot);
                if (cellItem != null && StackUtils.itemsMatch(cellItem, itemStack)) {
                    totalAmount += cellItem.getAmount();
                }
            }
        }

        // Cell Drives
        totalAmount += CellDrive.getStorage().getAmount(driveCache, getOutputAbleCellDriveMenus(), itemStack);

        if (totalAmount > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int) totalAmount;
        }
    }

    public @NotNull HashMap<ItemStack, Long> getAmount(@NotNull Set<ItemStack> itemStacks) {
        HashMap<ItemStack, Long> totalAmounts = new HashMap<>();
        for (BlockMenu menu : getAdvancedGreedyBlockMenus()) {
            int[] slots = menu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            for (int slot : slots) {
                final ItemStack inputSlotItem = menu.getItemInSlot(slot);
                if (inputSlotItem != null) {
                    for (ItemStack itemStack : itemStacks) {
                        if (StackUtils.itemsMatch(inputSlotItem, itemStack)) {
                            totalAmounts.put(
                                itemStack, totalAmounts.getOrDefault(itemStack, 0L) + inputSlotItem.getAmount());
                        }
                    }
                }
            }
        }

        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            ItemStack inputSlotItem = blockMenu.getItemInSlot(slots[0]);
            if (inputSlotItem != null) {
                for (ItemStack itemStack : itemStacks) {
                    if (StackUtils.itemsMatch(inputSlotItem, itemStack)) {
                        totalAmounts.put(
                            itemStack, totalAmounts.getOrDefault(itemStack, 0L) + inputSlotItem.getAmount());
                    }
                }
            }
        }

        for (BarrelIdentity barrelIdentity : getOutputAbleBarrels()) {
            for (ItemStack itemStack : itemStacks) {
                if (StackUtils.itemsMatch(barrelIdentity, itemStack)) {
                    long totalAmount = barrelIdentity.getAmount();
                    if (barrelIdentity instanceof InfinityBarrel) {
                        totalAmount -= 2;
                    }
                    totalAmounts.put(itemStack, totalAmounts.getOrDefault(itemStack, 0L) + totalAmount);
                }
            }
        }
        Map<StorageUnitData, Location> cacheMap = getOutputAbleCargoStorageUnitDatas();
        for (StorageUnitData cache : cacheMap.keySet()) {
            final List<ItemContainer> storedItems = cache.getStoredItems();
            for (ItemContainer itemContainer : storedItems) {
                for (ItemStack itemStack : itemStacks) {
                    if (StackUtils.itemsMatch(itemContainer, itemStack)) {
                        long totalAmount = itemContainer.getAmount();
                        totalAmounts.put(itemStack, totalAmounts.getOrDefault(itemStack, 0L) + totalAmount);
                    }
                }
            }
        }

        for (BlockMenu blockMenu : getCellMenus()) {
            if (!isRealCell(blockMenu)) continue;
            for (int slot : CELL_AVAILABLE_SLOTS) {
                final ItemStack cellItem = blockMenu.getItemInSlot(slot);
                if (cellItem != null) {
                    for (ItemStack itemStack : itemStacks) {
                        if (StackUtils.itemsMatch(cellItem, itemStack)) {
                            totalAmounts.put(
                                itemStack, totalAmounts.getOrDefault(itemStack, 0L) + cellItem.getAmount());
                        }
                    }
                }
            }
        }

        // Cell Drives
        for (ItemStack itemStack : itemStacks) {
            long cellAmount = CellDrive.getStorage().getAmount(driveCache, getOutputAbleCellDriveMenus(), itemStack);
            if (cellAmount > 0) {
                totalAmounts.put(itemStack, totalAmounts.getOrDefault(itemStack, 0L) + cellAmount);
            }
        }

        return totalAmounts;
    }

    @Warning(
        reason =
            "This method is deprecated and will be removed in the future. Use addItemStack0(Location, ItemStack) instead.")
    @Deprecated(forRemoval = true)
    public void addItemStack(@NotNull ItemStack incoming) {
        if (StackUtils.isBlacklisted(incoming)) {
            return;
        }

        for (BlockMenu blockMenu : getAdvancedGreedyBlockMenus()) {
            final ItemStack template = blockMenu.getItemInSlot(AdvancedGreedyBlock.TEMPLATE_SLOT);

            if (template == null || template.getType() == Material.AIR || !StackUtils.itemsMatch(incoming, template)) {
                continue;
            }

            blockMenu.markDirty();
            BlockMenuUtil.pushItem(blockMenu, incoming, ADVANCED_GREEDY_BLOCK_AVAILABLE_SLOTS);
            // Given we have found a match, it doesn't matter if the item moved or not, we will not bring it in
            return;
        }

        // Run for matching greedy blocks
        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            final ItemStack template = blockMenu.getItemInSlot(NetworkGreedyBlock.TEMPLATE_SLOT);

            if (template == null || template.getType() == Material.AIR || !StackUtils.itemsMatch(incoming, template)) {
                continue;
            }

            blockMenu.markDirty();
            BlockMenuUtil.pushItem(blockMenu, incoming, GREEDY_BLOCK_AVAILABLE_SLOTS[0]);
            // Given we have found a match, it doesn't matter if the item moved or not, we will not bring it in
            return;
        }

        // Run for matching barrels
        for (BarrelIdentity barrelIdentity : getInputAbleBarrels()) {
            if (StackUtils.itemsMatch(barrelIdentity, incoming)) {
                barrelIdentity.depositItemStack(incoming);

                // All distributed, can escape
                if (incoming.getAmount() == 0) {
                    return;
                }
            }
        }

        for (StorageUnitData cache : getInputAbleCargoStorageUnitDatas().keySet()) {
            cache.depositItemStack(incoming, true);

            if (incoming.getAmount() == 0) {
                return;
            }
        }

        for (BlockMenu blockMenu : getCellMenus()) {
            if (!isRealCell(blockMenu)) continue;
            blockMenu.markDirty();
            BlockMenuUtil.pushItem(blockMenu, incoming, CELL_AVAILABLE_SLOTS);
            if (incoming.getAmount() == 0) {
                return;
            }
        }
    }

    @Override
    public long retrieveBlockCharge() {
        return 0;
    }

    public void addRootPower(long power) {
        this.rootPower += power;
    }

    public void removeRootPower(long power) {
        if (power <= 0) {
            return;
        }

        long removed = 0;
        for (Location node : powerNodes) {
            final SlimefunItem item = StorageCacheUtils.getSfItem(node);
            if (item instanceof NetworkPowerNode powerNode) {
                final int charge = powerNode.getCharge(node);
                if (charge <= 0) {
                    continue;
                }
                final int toRemove = (int) Math.min(power - removed, charge);
                powerNode.removeCharge(node, toRemove);
                removed += toRemove;
            }
            if (removed >= power) {
                break;
            }
        }
        this.rootPower -= removed;
    }

    @Warning(
        reason =
            "This method is deprecated and will be removed in the future. Use getItemStacks0(Location, List<ItemRequest>) instead.")
    @Deprecated(forRemoval = true)
    @NotNull
    public List<ItemStack> getItemStacks(@NotNull List<ItemRequest> itemRequests) {
        List<ItemStack> retrievedItems = new ArrayList<>();

        for (ItemRequest request : itemRequests) {
            ItemStack retrieved = getItemStack(request);
            if (retrieved != null) {
                retrievedItems.add(retrieved);
            }
        }
        return retrievedItems;
    }

    @NotNull
    public List<ItemStack> getItemStacks0(@NotNull Location location, @NotNull List<ItemRequest> itemRequests) {
        List<ItemStack> retrievedItems = new ArrayList<>();
        for (ItemRequest request : itemRequests) {
            ItemStack retrieved = getItemStack0(location, request);
            if (retrieved != null) {
                retrievedItems.add(retrieved);
            }
        }
        return retrievedItems;
    }

    @NotNull
    public List<BarrelIdentity> getBarrels(
        @NotNull Predicate<BarrelIdentity> filter,
        NetworkRootLocateStorageEvent.Strategy strategy,
        boolean includeEmpty) {
        final Set<Location> addedLocations = ConcurrentHashMap.newKeySet();
        final List<BarrelIdentity> barrelSet = new ArrayList<>();

        final Set<Location> monitor = new HashSet<>();
        monitor.addAll(this.inputOnlyMonitors);
        monitor.addAll(this.outputOnlyMonitors);
        monitor.addAll(this.monitors);
        for (Location cellLocation : monitor) {
            final BlockFace face = NetworkDirectional.getSelectedFace(cellLocation);

            if (face == null) {
                continue;
            }

            final Location testLocation = cellLocation.clone().add(face.getDirection());

            if (addedLocations.contains(testLocation)) {
                continue;
            } else {
                addedLocations.add(testLocation);
            }

            final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(testLocation);

            if (Networks.getSupportedPluginManager().isInfinityExpansion()
                && slimefunItem instanceof StorageUnit unit) {
                final BlockMenu menu = StorageCacheUtils.getMenu(testLocation);
                if (menu == null) {
                    continue;
                }
                final InfinityBarrel infinityBarrel = getInfinityBarrel(menu, unit, includeEmpty);
                if (infinityBarrel != null) {
                    if (filter.test(infinityBarrel)) {
                        barrelSet.add(infinityBarrel);
                    }
                }
                continue;
            }
            if (Networks.getSupportedPluginManager().isFluffyMachines() && slimefunItem instanceof Barrel barrel) {
                final BlockMenu menu = StorageCacheUtils.getMenu(testLocation);
                if (menu == null) {
                    continue;
                }
                final FluffyBarrel fluffyBarrel = getFluffyBarrel(menu, barrel, includeEmpty);
                if (fluffyBarrel != null) {
                    if (filter.test(fluffyBarrel)) {
                        barrelSet.add(fluffyBarrel);
                    }
                }
                continue;
            }
            if (slimefunItem instanceof NetworkQuantumStorage) {
                final BlockMenu menu = StorageCacheUtils.getMenu(testLocation);
                if (menu == null) {
                    continue;
                }
                final NetworkStorage storage = getNetworkStorage(menu, includeEmpty);
                if (storage != null) {
                    if (filter.test(storage)) {
                        barrelSet.add(storage);
                    }
                }
            }
        }

        NetworkRootLocateStorageEvent event =
            new NetworkRootLocateStorageEvent(this, StorageType.BARREL, strategy, Bukkit.isPrimaryThread());
        Bukkit.getPluginManager().callEvent(event);
        return barrelSet;
    }

    @NotNull
    public Map<StorageUnitData, Location> getCargoStorageUnitDatas(
        NetworkRootLocateStorageEvent.Strategy strategy, boolean includeEmpty) {
        final Set<Location> addedLocations = ConcurrentHashMap.newKeySet();
        final Map<StorageUnitData, Location> dataSet = new HashMap<>();

        final Set<Location> monitor = new HashSet<>();
        monitor.addAll(this.inputOnlyMonitors);
        monitor.addAll(this.outputOnlyMonitors);
        monitor.addAll(this.monitors);
        for (Location cellLocation : monitor) {
            final BlockFace face = NetworkDirectional.getSelectedFace(cellLocation);

            if (face == null) {
                continue;
            }

            final Location testLocation = cellLocation.clone().add(face.getDirection());

            if (addedLocations.contains(testLocation)) {
                continue;
            } else {
                addedLocations.add(testLocation);
            }

            final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(testLocation);

            if (slimefunItem instanceof NetworksDrawer) {
                final StorageUnitData data = getCargoStorageUnitData(testLocation);
                if (data != null) {
                    dataSet.put(data, testLocation);
                }
            }
        }

        NetworkRootLocateStorageEvent event =
            new NetworkRootLocateStorageEvent(this, StorageType.DRAWER, strategy, Bukkit.isPrimaryThread());
        Bukkit.getPluginManager().callEvent(event);
        return dataSet;
    }

    @NotNull
    public Set<BarrelIdentity> getInputAbleBarrels() {
        if (this.inputAbleBarrels != null) {
            return this.inputAbleBarrels;
        }

        final Set<Location> addedLocations = ConcurrentHashMap.newKeySet();
        final Set<BarrelIdentity> barrelSet = ConcurrentHashMap.newKeySet();

        final Set<Location> monitor = new HashSet<>();
        monitor.addAll(this.inputOnlyMonitors);
        monitor.addAll(this.monitors);
        for (Location cellLocation : monitor) {
            final BlockFace face = NetworkDirectional.getSelectedFace(cellLocation);

            if (face == null) {
                continue;
            }

            final Location testLocation = cellLocation.clone().add(face.getDirection());

            if (addedLocations.contains(testLocation)) {
                continue;
            } else {
                addedLocations.add(testLocation);
            }

            final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(testLocation);

            if (Networks.getSupportedPluginManager().isInfinityExpansion()
                && slimefunItem instanceof StorageUnit unit) {
                final BlockMenu menu = StorageCacheUtils.getMenu(testLocation);
                if (menu == null) {
                    continue;
                }
                final InfinityBarrel infinityBarrel = getInfinityBarrel(menu, unit);
                if (infinityBarrel != null) {
                    barrelSet.add(infinityBarrel);
                }
                continue;
            }
            if (Networks.getSupportedPluginManager().isFluffyMachines() && slimefunItem instanceof Barrel barrel) {
                final BlockMenu menu = StorageCacheUtils.getMenu(testLocation);
                if (menu == null) {
                    continue;
                }
                final FluffyBarrel fluffyBarrel = getFluffyBarrel(menu, barrel);
                if (fluffyBarrel != null) {
                    barrelSet.add(fluffyBarrel);
                }
                continue;
            }
            if (slimefunItem instanceof NetworkQuantumStorage) {
                final BlockMenu menu = StorageCacheUtils.getMenu(testLocation);
                if (menu == null) {
                    continue;
                }
                final NetworkStorage storage = getNetworkStorage(menu);
                if (storage != null) {
                    barrelSet.add(storage);
                }
            }
        }

        this.inputAbleBarrels = barrelSet;
        final Map<Location, BarrelIdentity> inputAbleBarrelMap = new ConcurrentHashMap<>();
        for (BarrelIdentity storage : barrelSet) {
            inputAbleBarrelMap.put(storage.getLocation(), storage);
        }
        this.mapInputAbleBarrels = inputAbleBarrelMap;
        NetworkRootLocateStorageEvent event =
            new NetworkRootLocateStorageEvent(this, StorageType.BARREL, true, false, Bukkit.isPrimaryThread());
        Bukkit.getPluginManager().callEvent(event);
        return barrelSet;
    }

    @NotNull
    public Set<BarrelIdentity> getOutputAbleBarrels() {

        if (this.outputAbleBarrels != null) {
            return this.outputAbleBarrels;
        }

        final Set<Location> addedLocations = ConcurrentHashMap.newKeySet();
        final Set<BarrelIdentity> barrelSet = ConcurrentHashMap.newKeySet();

        final Set<Location> monitor = new HashSet<>();
        monitor.addAll(this.outputOnlyMonitors);
        monitor.addAll(this.monitors);
        for (Location cellLocation : monitor) {
            final BlockFace face = NetworkDirectional.getSelectedFace(cellLocation);

            if (face == null) {
                continue;
            }

            final Location testLocation = cellLocation.clone().add(face.getDirection());

            if (addedLocations.contains(testLocation)) {
                continue;
            } else {
                addedLocations.add(testLocation);
            }

            final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(testLocation);

            if (Networks.getSupportedPluginManager().isInfinityExpansion()
                && slimefunItem instanceof StorageUnit unit) {
                final BlockMenu menu = StorageCacheUtils.getMenu(testLocation);
                if (menu == null) {
                    continue;
                }
                final InfinityBarrel infinityBarrel = getInfinityBarrel(menu, unit);
                if (infinityBarrel != null) {
                    barrelSet.add(infinityBarrel);
                }
                continue;
            }
            if (Networks.getSupportedPluginManager().isFluffyMachines() && slimefunItem instanceof Barrel barrel) {
                final BlockMenu menu = StorageCacheUtils.getMenu(testLocation);
                if (menu == null) {
                    continue;
                }
                final FluffyBarrel fluffyBarrel = getFluffyBarrel(menu, barrel);
                if (fluffyBarrel != null) {
                    barrelSet.add(fluffyBarrel);
                }
                continue;
            }
            if (slimefunItem instanceof NetworkQuantumStorage) {
                final BlockMenu menu = StorageCacheUtils.getMenu(testLocation);
                if (menu == null) {
                    continue;
                }
                final NetworkStorage storage = getNetworkStorage(menu);
                if (storage != null) {
                    barrelSet.add(storage);
                }
            }
        }

        this.outputAbleBarrels = barrelSet;
        final Map<Location, BarrelIdentity> outputAbleBarrelMap = new ConcurrentHashMap<>();
        for (BarrelIdentity storage : barrelSet) {
            outputAbleBarrelMap.put(storage.getLocation(), storage);
        }
        this.mapOutputAbleBarrels = outputAbleBarrelMap;
        NetworkRootLocateStorageEvent event =
            new NetworkRootLocateStorageEvent(this, StorageType.BARREL, false, true, Bukkit.isPrimaryThread());
        Bukkit.getPluginManager().callEvent(event);
        return barrelSet;
    }

    @NotNull
    public Map<StorageUnitData, Location> getInputAbleCargoStorageUnitDatas() {
        if (this.inputAbleCargoStorageUnitDatas != null) {
            return this.inputAbleCargoStorageUnitDatas;
        }

        final Set<Location> addedLocations = ConcurrentHashMap.newKeySet();
        final Map<StorageUnitData, Location> dataSet = new ConcurrentHashMap<>();

        final Set<Location> monitor = new HashSet<>();
        monitor.addAll(this.inputOnlyMonitors);
        monitor.addAll(this.monitors);
        for (Location cellLocation : monitor) {
            final BlockFace face = NetworkDirectional.getSelectedFace(cellLocation);

            if (face == null) {
                continue;
            }

            final Location testLocation = cellLocation.clone().add(face.getDirection());

            if (addedLocations.contains(testLocation)) {
                continue;
            } else {
                addedLocations.add(testLocation);
            }

            final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(testLocation);

            if (slimefunItem instanceof NetworksDrawer) {
                final StorageUnitData data = getCargoStorageUnitData(testLocation);
                if (data != null) {
                    dataSet.put(data, testLocation);
                }
            }
        }

        this.inputAbleCargoStorageUnitDatas = dataSet;
        final Map<Location, StorageUnitData> inputAbleCargoMap = new ConcurrentHashMap<>();
        for (Map.Entry<StorageUnitData, Location> entry : dataSet.entrySet()) {
            inputAbleCargoMap.put(entry.getValue(), entry.getKey());
        }
        this.mapInputAbleCargoStorageUnits = inputAbleCargoMap;
        NetworkRootLocateStorageEvent event =
            new NetworkRootLocateStorageEvent(this, StorageType.DRAWER, true, false, Bukkit.isPrimaryThread());
        Bukkit.getPluginManager().callEvent(event);
        return dataSet;
    }

    @NotNull
    public Map<StorageUnitData, Location> getOutputAbleCargoStorageUnitDatas() {
        if (this.outputAbleCargoStorageUnitDatas != null) {
            return this.outputAbleCargoStorageUnitDatas;
        }

        final Set<Location> addedLocations = ConcurrentHashMap.newKeySet();
        final Map<StorageUnitData, Location> dataSet = new ConcurrentHashMap<>();

        final Set<Location> monitor = new HashSet<>();
        monitor.addAll(this.outputOnlyMonitors);
        monitor.addAll(this.monitors);
        for (Location cellLocation : monitor) {
            final BlockFace face = NetworkDirectional.getSelectedFace(cellLocation);

            if (face == null) {
                continue;
            }

            final Location testLocation = cellLocation.clone().add(face.getDirection());

            if (addedLocations.contains(testLocation)) {
                continue;
            } else {
                addedLocations.add(testLocation);
            }

            final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(testLocation);

            if (slimefunItem instanceof NetworksDrawer) {
                final StorageUnitData data = getCargoStorageUnitData(testLocation);
                if (data != null) {
                    dataSet.put(data, testLocation);
                }
            }
        }

        this.outputAbleCargoStorageUnitDatas = dataSet;
        final Map<Location, StorageUnitData> outputAbleCargoMap = new ConcurrentHashMap<>();
        for (Map.Entry<StorageUnitData, Location> entry : dataSet.entrySet()) {
            outputAbleCargoMap.put(entry.getValue(), entry.getKey());
        }
        this.mapOutputAbleCargoStorageUnits = outputAbleCargoMap;
        NetworkRootLocateStorageEvent event =
            new NetworkRootLocateStorageEvent(this, StorageType.DRAWER, false, true, Bukkit.isPrimaryThread());
        Bukkit.getPluginManager().callEvent(event);
        return dataSet;
    }

    public boolean refreshRootItems() {
        bumpInvalidationEpoch();
        this.barrels = null;
        this.cargoStorageUnitDatas = null;
        this.inputAbleBarrels = null;
        this.outputAbleBarrels = null;
        this.inputAbleCargoStorageUnitDatas = null;
        this.outputAbleCargoStorageUnitDatas = null;
        this.cellDriveMenus = null;
        this.inputAbleCellDriveMenus = null;
        this.outputAbleCellDriveMenus = null;
        this.greedyBlockMenus = null;
        this.advancedGreedyBlockMenus = null;
        this.cellMenus = null;

        getBarrels();
        getCargoStorageUnitDatas();
        getInputAbleBarrels();
        getOutputAbleBarrels();
        getInputAbleCargoStorageUnitDatas();
        getOutputAbleCargoStorageUnitDatas();
        return true;
    }

    @Nullable
    public BarrelIdentity accessInputAbleBarrel(Location barrelLocation) {
        return getMapInputAbleBarrels().get(barrelLocation);
    }

    @Nullable
    public BarrelIdentity accessOutputAbleBarrel(Location barrelLocation) {
        return getMapOutputAbleBarrels().get(barrelLocation);
    }

    @Nullable
    public StorageUnitData accessInputAbleDrawerData(Location drawerLocation) {
        return accessInputAbleCargoStorageUnitData(drawerLocation);
    }

    @Nullable
    public StorageUnitData accessOutputAbleDrawerData(Location drawerLocation) {
        return accessOutputAbleCargoStorageUnitData(drawerLocation);
    }

    @Nullable
    public StorageUnitData accessInputAbleCargoStorageUnitData(Location storageUnitLocation) {
        return getMapInputAbleCargoStorageUnits().get(storageUnitLocation);
    }

    @Nullable
    public StorageUnitData accessOutputAbleCargoStorageUnitData(Location storageUnitLocation) {
        return getMapOutputAbleCargoStorageUnits().get(storageUnitLocation);
    }

    @Nullable
    public ItemStack requestItem(@NotNull Location accessor, @NotNull ItemRequest request) {
        return getItemStack0(accessor, request);
    }

    @Nullable
    public ItemStack requestItem(@NotNull Location accessor, @NotNull ItemStack itemStack) {
        return requestItem(accessor, new ItemRequest(itemStack, itemStack.getAmount()));
    }

    public void tryRecord(@NotNull Location accessor, @NotNull ItemRequest request) {
        if (recordFlow && itemFlowRecord != null) {
            itemFlowRecord.addAction(accessor, request);
        }
    }

    public ItemStack getItemStack0(@NotNull Location accessor, @NotNull ItemRequest request) {
        return getItemStack0(accessor, request, null);
    }

    /**
     * @param accessor 取物的机器位置
     * @param request  请求（会被消费，{@code receiveAmount} 递减剩余需求）
     * @param option   物品匹配选项（忽略项），为 null 时使用 {@link MatchOption#DEFAULT}
     * @return 匹配 {@code option} 的物品；数量为 0 时返回 null
     */
    public ItemStack getItemStack0(
        @NotNull Location accessor, @NotNull ItemRequest request, @Nullable MatchOption option) {
        ItemStack stackToReturn = null;

        if (request.getAmount() <= 0) {
            FeedbackSendable.sendFeedback0(accessor, FeedbackType.ROOT_REQUEST_0);
            return null;
        }

        bumpWriteEpoch();

        Map<Location, Integer> m = getPersistentAccessHistory(accessor);
        if (m != null) {
            // Netex - Cache start
            boolean found = false;
            List<Location> misses = new ArrayList<>();
            // Netex - Cache end
            for (Map.Entry<Location, Integer> entry : m.entrySet()) {
                // try cache first
                BarrelIdentity barrelIdentity = accessOutputAbleBarrel(entry.getKey());
                if (barrelIdentity != null) {
                    // <editor-fold desc="do barrel">
                    final ItemStack itemStack = barrelIdentity.getItemStack();

                    if (itemStack == null || !StackUtils.itemsMatch(request, itemStack, option)) {
                        // Netex - Cache start
                        misses.add(entry.getKey());
                        // Netex - Cache end
                        continue;
                    }

                    // Netex - Cache start
                    minusCacheMiss(accessor, entry.getKey());
                    found = true;
                    // Netex - Cache end

                    boolean infinity = barrelIdentity instanceof InfinityBarrel;
                    final ItemStack fetched = barrelIdentity.requestItem(request);
                    if (fetched == null
                        || fetched.getType() == Material.AIR
                        || (infinity && fetched.getAmount() == 1)) {
                        continue;
                    }

                    // Stack is null, so we can fill it here
                    if (stackToReturn == null) {
                        stackToReturn = fetched.clone();
                        stackToReturn.setAmount(0);
                    }

                    final int preserveAmount = infinity ? fetched.getAmount() - 1 : fetched.getAmount();

                    if (request.getAmount() <= preserveAmount) {
                        // Netex - Reduce start
                        // Netex - Reduce end
                        stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                        fetched.setAmount(fetched.getAmount() - request.getAmount());
                        // Netex - Record start
                        tryRecord(accessor, request);
                        // Netex - Record end
                        return stackToReturn;
                    } else {
                        stackToReturn.setAmount(stackToReturn.getAmount() + preserveAmount);
                        request.receiveAmount(preserveAmount);
                        fetched.setAmount(fetched.getAmount() - preserveAmount);
                    }
                    // </editor-fold>
                } else {
                    StorageUnitData data = accessOutputAbleCargoStorageUnitData(entry.getKey());
                    if (data != null) {
                        // <editor-fold desc="do drawer">
                        ItemStack take = data.requestItem0(accessor, request);
                        if (take != null) {
                            // Netex - Cache start
                            minusCacheMiss(accessor, entry.getKey());
                            found = true;
                            // Netex - Cache end

                            if (stackToReturn == null) {
                                stackToReturn = take.clone();
                            } else {
                                stackToReturn.setAmount(stackToReturn.getAmount() + take.getAmount());
                            }
                            request.receiveAmount(take.getAmount());

                            if (request.getAmount() <= 0) {
                                // Netex - Reduce start
                                // Netex - Reduce end
                                // Netex - Record start
                                tryRecord(accessor, request);
                                // Netex - Record end
                                return stackToReturn;
                            }
                        } else {
                            // Netex - Cache start
                            misses.add(entry.getKey());
                            // Netex - Cache end
                        }
                        // </editor-fold>
                    } else {
                        // Netex - Cache start
                        misses.add(entry.getKey());
                        // Netex - Cache end
                    }
                }
            }

            // Netex - Cache start
            if (!found) {
                for (Location miss : misses) {
                    minusCacheMiss(accessor, miss);
                }
            }
            // Netex - Cache end
        }

        // Barrels first
        for (BarrelIdentity barrelIdentity : getOutputAbleBarrels()) {
            // <editor-fold desc="do barrel">
            final ItemStack itemStack = barrelIdentity.getItemStack();

            if (itemStack == null || !StackUtils.itemsMatch(request, itemStack, option)) {
                continue;
            }

            // Netex - Cache start
            addCountObservingAccessHistory(accessor, barrelIdentity.getLocation());
            // Netex - Cache end

            boolean infinity = barrelIdentity instanceof InfinityBarrel;
            final ItemStack fetched = barrelIdentity.requestItem(request);
            if (fetched == null || fetched.getType() == Material.AIR || (infinity && fetched.getAmount() == 1)) {
                continue;
            }

            // Stack is null, so we can fill it here
            if (stackToReturn == null) {
                stackToReturn = fetched.clone();
                stackToReturn.setAmount(0);
            }

            final int preserveAmount = infinity ? fetched.getAmount() - 1 : fetched.getAmount();

            if (request.getAmount() <= preserveAmount) {
                // Netex - Reduce start
                // Netex - Reduce end
                stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                fetched.setAmount(fetched.getAmount() - request.getAmount());
                // Netex - Record start
                tryRecord(accessor, request);
                // Netex - Record end
                return stackToReturn;
            } else {
                stackToReturn.setAmount(stackToReturn.getAmount() + preserveAmount);
                request.receiveAmount(preserveAmount);
                fetched.setAmount(fetched.getAmount() - preserveAmount);
            }
            // </editor-fold>
        }

        // Units
        for (StorageUnitData cache : getOutputAbleCargoStorageUnitDatas().keySet()) {
            // <editor-fold desc="do drawer">
            ItemStack take = cache.requestItem0(accessor, request);
            if (take != null) {
                // Netex - Cache start
                addCountObservingAccessHistory(accessor, cache.getLastLocation());
                // Netex - Cache end
                if (stackToReturn == null) {
                    stackToReturn = take.clone();
                } else {
                    stackToReturn.setAmount(stackToReturn.getAmount() + take.getAmount());
                }
                request.receiveAmount(take.getAmount());

                if (request.getAmount() <= 0) {
                    // Netex - Reduce start
                    // Netex - Reduce end
                    // Netex - Record start
                    tryRecord(accessor, request);
                    // Netex - Record end
                    return stackToReturn;
                }
            }
            // </editor-fold>
        }

        // Cell Drives
        ItemStack take = CellDrive.getStorage().takeItem(driveCache, getOutputAbleCellDriveMenus(), request);
        if (take != null) {
            if (stackToReturn == null) {
                stackToReturn = take.clone();
            } else {
                stackToReturn.setAmount(stackToReturn.getAmount() + take.getAmount());
            }
            request.receiveAmount(take.getAmount());

            if (request.getAmount() <= 0) {
                tryRecord(accessor, request);
                return stackToReturn;
            }
        }

        // Cells
        for (BlockMenu blockMenu : getCellMenus()) {
            if (!isRealCell(blockMenu)) continue;
            for (int slot : CELL_AVAILABLE_SLOTS) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null
                    || itemStack.getType() == Material.AIR
                    || !StackUtils.itemsMatch(request, itemStack, option)) {
                    continue;
                }

                // Mark the Cell as dirty otherwise the changes will not save on shutdown
                blockMenu.markDirty();

                // If the return stack is null, we need to set it up
                if (stackToReturn == null) {
                    stackToReturn = itemStack.clone();
                    stackToReturn.setAmount(0);
                }

                if (request.getAmount() <= itemStack.getAmount()) {
                    // Netex - Reduce start
                    // Netex - Reduce end
                    // We can't take more than this stack. Level to request amount, remove items and then return
                    stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                    itemStack.setAmount(itemStack.getAmount() - request.getAmount());
                    // Netex - Record start
                    tryRecord(accessor, request);
                    // Netex - Record end
                    return stackToReturn;
                } else {
                    // We can take more than what is here, consume before trying to take more
                    stackToReturn.setAmount(stackToReturn.getAmount() + itemStack.getAmount());
                    request.receiveAmount(itemStack.getAmount());
                    itemStack.setAmount(0);
                }
            }
        }

        // Crafters
        for (BlockMenu blockMenu : getCrafterOutputs()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            for (int slot : slots) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null
                    || itemStack.getType() == Material.AIR
                    || !StackUtils.itemsMatch(request, itemStack, option)) {
                    continue;
                }

                // Stack is null, so we can fill it here
                if (stackToReturn == null) {
                    stackToReturn = itemStack.clone();
                    stackToReturn.setAmount(0);
                }

                if (request.getAmount() <= itemStack.getAmount()) {
                    // Netex - Reduce start
                    // Netex - Reduce end
                    stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                    itemStack.setAmount(itemStack.getAmount() - request.getAmount());
                    // Netex - Record start
                    tryRecord(accessor, request);
                    // Netex - Record end
                    return stackToReturn;
                } else {
                    stackToReturn.setAmount(stackToReturn.getAmount() + itemStack.getAmount());
                    request.receiveAmount(itemStack.getAmount());
                    itemStack.setAmount(0);
                }
            }
        }

        for (BlockMenu blockMenu : getAdvancedGreedyBlockMenus()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            for (int slot : slots) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null
                    || itemStack.getType() == Material.AIR
                    || !StackUtils.itemsMatch(request, itemStack, option)) {
                    continue;
                }

                // Stack is null, so we can fill it here
                if (stackToReturn == null) {
                    stackToReturn = itemStack.clone();
                    stackToReturn.setAmount(0);
                }

                if (request.getAmount() <= itemStack.getAmount()) {
                    // Netex - Reduce start
                    // Netex - Reduce end
                    stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                    itemStack.setAmount(itemStack.getAmount() - request.getAmount());
                    // Netex - Record start
                    tryRecord(accessor, request);
                    // Netex - Record end
                    return stackToReturn;
                } else {
                    stackToReturn.setAmount(stackToReturn.getAmount() + itemStack.getAmount());
                    request.receiveAmount(itemStack.getAmount());
                    itemStack.setAmount(0);
                }
            }
        }

        // Greedy Blocks
        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            final ItemStack itemStack = blockMenu.getItemInSlot(slots[0]);
            if (itemStack == null
                || itemStack.getType() == Material.AIR
                || !StackUtils.itemsMatch(request, itemStack, option)) {
                continue;
            }

            // Mark the Cell as dirty otherwise the changes will not save on shutdown
            blockMenu.markDirty();

            // If the return stack is null, we need to set it up
            if (stackToReturn == null) {
                stackToReturn = itemStack.clone();
                stackToReturn.setAmount(0);
            }

            if (request.getAmount() <= itemStack.getAmount()) {
                // Netex - Reduce start
                // Netex - Reduce end
                // We can't take more than this stack. Level to request amount, remove items and then return
                stackToReturn.setAmount(stackToReturn.getAmount() + request.getAmount());
                itemStack.setAmount(itemStack.getAmount() - request.getAmount());
                // Netex - Record start
                tryRecord(accessor, request);
                // Netex - Record end
                return stackToReturn;
            } else {
                // We can take more than what is here, consume before trying to take more
                stackToReturn.setAmount(stackToReturn.getAmount() + itemStack.getAmount());
                request.receiveAmount(itemStack.getAmount());
                itemStack.setAmount(0);
            }
        }

        if (stackToReturn == null || stackToReturn.getAmount() == 0) {
            return null;
        }

        // Netex - Reduce start
        // Netex - Reduce end
        // Netex - Record start
        tryRecord(accessor, request);
        // Netex - Record end

        return stackToReturn;
    }

    /**
     * 批量取料：一次网络走查服务多个请求，走查家族顺序与 {@link #getItemStack0(Location, ItemRequest)} 完全一致
     * （访问历史优先 → 桶 → 抽屉 → 元件驱动器 → 细胞 → 合成器 → 高级贪心 → 贪心），
     * 唯一区别是每到访一个存储节点会尝试满足所有未完成请求，全部满足后立即结束走查。
     * <p>
     * 契约：消费各请求（{@link ItemRequest#receiveAmount(int)} 递减至 0）；返回列表与入参按位对齐，
     * 未取到任何物品的请求对应位为 null；相同身份的重复请求不去重、按序逐条尝试；
     * 访问历史命中/衰减与 transport-miss 簿记按请求各自对齐。
     * 与逐请求调用 getItemStack0 的行为差异：各请求在存储节点间的消耗顺序不同（各请求取到总量不变）、
     * recordFlow 记录顺序不同、access-limit 反馈整批至多一条。
     */
    @NotNull
    public List<ItemStack> getItemStacksBatch0(@NotNull Location accessor, @NotNull List<ItemRequest> requests) {
        final List<ItemStack> results = new ArrayList<>(requests.size());
        final List<BatchTake> pending = new ArrayList<>(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            results.add(null);
            final ItemRequest request = requests.get(i);
            if (request == null) {
                continue;
            }
            if (request.getAmount() <= 0) {
                FeedbackSendable.sendFeedback0(accessor, FeedbackType.ROOT_REQUEST_0);
                continue;
            }
            pending.add(new BatchTake(request, i));
        }
        if (pending.isEmpty()) {
            return results;
        }

        bumpWriteEpoch();

        Map<Location, Integer> m = getPersistentAccessHistory(accessor);
        if (m != null) {
            for (Map.Entry<Location, Integer> entry : m.entrySet()) {
                final Location historyLocation = entry.getKey();
                final BarrelIdentity barrelIdentity = accessOutputAbleBarrel(historyLocation);
                if (barrelIdentity != null) {
                    final ItemStack barrelItem = barrelIdentity.getItemStack();
                    if (barrelItem == null) {
                        for (BatchTake take : pending) {
                            take.historyMisses.add(historyLocation);
                        }
                        continue;
                    }
                    final boolean infinity = barrelIdentity instanceof InfinityBarrel;
                    for (Iterator<BatchTake> iterator = pending.iterator(); iterator.hasNext(); ) {
                        final BatchTake take = iterator.next();
                        final ItemRequest request = take.request;
                        if (!StackUtils.itemsMatch(request, barrelItem)) {
                            take.historyMisses.add(historyLocation);
                            continue;
                        }

                        // Netex - Cache start
                        minusCacheMiss(accessor, historyLocation);
                        take.historyFound = true;
                        // Netex - Cache end

                        final ItemStack fetched = barrelIdentity.requestItem(request);
                        if (fetched == null
                            || fetched.getType() == Material.AIR
                            || (infinity && fetched.getAmount() == 1)) {
                            continue;
                        }

                        if (take.collected == null) {
                            take.collected = fetched.clone();
                            take.collected.setAmount(0);
                        }

                        final int preserveAmount = infinity ? fetched.getAmount() - 1 : fetched.getAmount();
                        if (request.getAmount() <= preserveAmount) {
                            // Netex - Reduce start
                            // Netex - Reduce end
                            take.collected.setAmount(take.collected.getAmount() + request.getAmount());
                            fetched.setAmount(fetched.getAmount() - request.getAmount());
                            // Netex - Record start
                            tryRecord(accessor, request);
                            // Netex - Record end
                            results.set(take.index, take.collected);
                            iterator.remove();
                        } else {
                            take.collected.setAmount(take.collected.getAmount() + preserveAmount);
                            request.receiveAmount(preserveAmount);
                            fetched.setAmount(fetched.getAmount() - preserveAmount);
                        }
                    }
                } else {
                    StorageUnitData data = accessOutputAbleCargoStorageUnitData(historyLocation);
                    if (data != null) {
                        for (Iterator<BatchTake> iterator = pending.iterator(); iterator.hasNext(); ) {
                            final BatchTake take = iterator.next();
                            final ItemStack takeStack = data.requestItem0(accessor, take.request);
                            if (takeStack != null) {
                                // Netex - Cache start
                                minusCacheMiss(accessor, historyLocation);
                                take.historyFound = true;
                                // Netex - Cache end

                                if (take.collected == null) {
                                    take.collected = takeStack.clone();
                                } else {
                                    take.collected.setAmount(take.collected.getAmount() + takeStack.getAmount());
                                }
                                take.request.receiveAmount(takeStack.getAmount());

                                if (take.request.getAmount() <= 0) {
                                    // Netex - Reduce start
                                    // Netex - Reduce end
                                    // Netex - Record start
                                    tryRecord(accessor, take.request);
                                    // Netex - Record end
                                    results.set(take.index, take.collected);
                                    iterator.remove();
                                }
                            } else {
                                // Netex - Cache start
                                take.historyMisses.add(historyLocation);
                                // Netex - Cache end
                            }
                        }
                    } else {
                        for (BatchTake take : pending) {
                            take.historyMisses.add(historyLocation);
                        }
                    }
                }
                if (pending.isEmpty()) {
                    break;
                }
            }

            // Netex - Cache start
            for (BatchTake take : pending) {
                if (!take.historyFound) {
                    for (Location miss : take.historyMisses) {
                        minusCacheMiss(accessor, miss);
                    }
                }
            }
            // Netex - Cache end
        }
        if (pending.isEmpty()) {
            return results;
        }

        // Barrels first
        for (BarrelIdentity barrelIdentity : getOutputAbleBarrels()) {
            final ItemStack barrelItem = barrelIdentity.getItemStack();
            if (barrelItem == null) {
                continue;
            }
            final boolean infinity = barrelIdentity instanceof InfinityBarrel;
            for (Iterator<BatchTake> iterator = pending.iterator(); iterator.hasNext(); ) {
                final BatchTake take = iterator.next();
                final ItemRequest request = take.request;
                if (!StackUtils.itemsMatch(request, barrelItem)) {
                    continue;
                }

                // Netex - Cache start
                addCountObservingAccessHistory(accessor, barrelIdentity.getLocation());
                // Netex - Cache end

                final ItemStack fetched = barrelIdentity.requestItem(request);
                if (fetched == null || fetched.getType() == Material.AIR || (infinity && fetched.getAmount() == 1)) {
                    continue;
                }

                if (take.collected == null) {
                    take.collected = fetched.clone();
                    take.collected.setAmount(0);
                }

                final int preserveAmount = infinity ? fetched.getAmount() - 1 : fetched.getAmount();
                if (request.getAmount() <= preserveAmount) {
                    // Netex - Reduce start
                    // Netex - Reduce end
                    take.collected.setAmount(take.collected.getAmount() + request.getAmount());
                    fetched.setAmount(fetched.getAmount() - request.getAmount());
                    // Netex - Record start
                    tryRecord(accessor, request);
                    // Netex - Record end
                    results.set(take.index, take.collected);
                    iterator.remove();
                } else {
                    take.collected.setAmount(take.collected.getAmount() + preserveAmount);
                    request.receiveAmount(preserveAmount);
                    fetched.setAmount(fetched.getAmount() - preserveAmount);
                }
            }
            if (pending.isEmpty()) {
                return results;
            }
        }

        // Units
        for (StorageUnitData cache : getOutputAbleCargoStorageUnitDatas().keySet()) {
            for (Iterator<BatchTake> iterator = pending.iterator(); iterator.hasNext(); ) {
                final BatchTake take = iterator.next();
                final ItemStack takeStack = cache.requestItem0(accessor, take.request);
                if (takeStack != null) {
                    // Netex - Cache start
                    addCountObservingAccessHistory(accessor, cache.getLastLocation());
                    // Netex - Cache end
                    if (take.collected == null) {
                        take.collected = takeStack.clone();
                    } else {
                        take.collected.setAmount(take.collected.getAmount() + takeStack.getAmount());
                    }
                    take.request.receiveAmount(takeStack.getAmount());

                    if (take.request.getAmount() <= 0) {
                        // Netex - Reduce start
                        // Netex - Reduce end
                        // Netex - Record start
                        tryRecord(accessor, take.request);
                        // Netex - Record end
                        results.set(take.index, take.collected);
                        iterator.remove();
                    }
                }
            }
            if (pending.isEmpty()) {
                return results;
            }
        }

        // Cell Drives
        for (Iterator<BatchTake> iterator = pending.iterator(); iterator.hasNext(); ) {
            final BatchTake take = iterator.next();
            final ItemStack takeStack =
                CellDrive.getStorage().takeItem(driveCache, getOutputAbleCellDriveMenus(), take.request);
            if (takeStack != null) {
                if (take.collected == null) {
                    take.collected = takeStack.clone();
                } else {
                    take.collected.setAmount(take.collected.getAmount() + takeStack.getAmount());
                }
                take.request.receiveAmount(takeStack.getAmount());

                if (take.request.getAmount() <= 0) {
                    tryRecord(accessor, take.request);
                    results.set(take.index, take.collected);
                    iterator.remove();
                }
            }
        }
        if (pending.isEmpty()) {
            return results;
        }

        // Cells
        for (BlockMenu blockMenu : getCellMenus()) {
            if (!isRealCell(blockMenu)) continue;
            for (int slot : CELL_AVAILABLE_SLOTS) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null || itemStack.getType() == Material.AIR) {
                    continue;
                }
                for (Iterator<BatchTake> iterator = pending.iterator(); iterator.hasNext(); ) {
                    final BatchTake take = iterator.next();
                    if (!StackUtils.itemsMatch(take.request, itemStack)) {
                        continue;
                    }

                    // Mark the Cell as dirty otherwise the changes will not save on shutdown
                    blockMenu.markDirty();

                    // If the return stack is null, we need to set it up
                    if (take.collected == null) {
                        take.collected = itemStack.clone();
                        take.collected.setAmount(0);
                    }

                    if (take.request.getAmount() <= itemStack.getAmount()) {
                        // Netex - Reduce start
                        // Netex - Reduce end
                        // We can't take more than this stack. Level to request amount, remove items and then return
                        take.collected.setAmount(take.collected.getAmount() + take.request.getAmount());
                        itemStack.setAmount(itemStack.getAmount() - take.request.getAmount());
                        // Netex - Record start
                        tryRecord(accessor, take.request);
                        // Netex - Record end
                        results.set(take.index, take.collected);
                        iterator.remove();
                    } else {
                        // We can take more than what is here, consume before trying to take more
                        take.collected.setAmount(take.collected.getAmount() + itemStack.getAmount());
                        take.request.receiveAmount(itemStack.getAmount());
                        itemStack.setAmount(0);
                    }
                }
            }
            if (pending.isEmpty()) {
                return results;
            }
        }

        // Crafters
        for (BlockMenu blockMenu : getCrafterOutputs()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            for (int slot : slots) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null || itemStack.getType() == Material.AIR) {
                    continue;
                }
                for (Iterator<BatchTake> iterator = pending.iterator(); iterator.hasNext(); ) {
                    final BatchTake take = iterator.next();
                    if (!StackUtils.itemsMatch(take.request, itemStack)) {
                        continue;
                    }

                    if (take.collected == null) {
                        take.collected = itemStack.clone();
                        take.collected.setAmount(0);
                    }

                    if (take.request.getAmount() <= itemStack.getAmount()) {
                        // Netex - Reduce start
                        // Netex - Reduce end
                        take.collected.setAmount(take.collected.getAmount() + take.request.getAmount());
                        itemStack.setAmount(itemStack.getAmount() - take.request.getAmount());
                        // Netex - Record start
                        tryRecord(accessor, take.request);
                        // Netex - Record end
                        results.set(take.index, take.collected);
                        iterator.remove();
                    } else {
                        take.collected.setAmount(take.collected.getAmount() + itemStack.getAmount());
                        take.request.receiveAmount(itemStack.getAmount());
                        itemStack.setAmount(0);
                    }
                }
            }
            if (pending.isEmpty()) {
                return results;
            }
        }

        for (BlockMenu blockMenu : getAdvancedGreedyBlockMenus()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            for (int slot : slots) {
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null || itemStack.getType() == Material.AIR) {
                    continue;
                }
                for (Iterator<BatchTake> iterator = pending.iterator(); iterator.hasNext(); ) {
                    final BatchTake take = iterator.next();
                    if (!StackUtils.itemsMatch(take.request, itemStack)) {
                        continue;
                    }

                    if (take.collected == null) {
                        take.collected = itemStack.clone();
                        take.collected.setAmount(0);
                    }

                    if (take.request.getAmount() <= itemStack.getAmount()) {
                        // Netex - Reduce start
                        // Netex - Reduce end
                        take.collected.setAmount(take.collected.getAmount() + take.request.getAmount());
                        itemStack.setAmount(itemStack.getAmount() - take.request.getAmount());
                        // Netex - Record start
                        tryRecord(accessor, take.request);
                        // Netex - Record end
                        results.set(take.index, take.collected);
                        iterator.remove();
                    } else {
                        take.collected.setAmount(take.collected.getAmount() + itemStack.getAmount());
                        take.request.receiveAmount(itemStack.getAmount());
                        itemStack.setAmount(0);
                    }
                }
            }
            if (pending.isEmpty()) {
                return results;
            }
        }

        // Greedy Blocks
        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
            final ItemStack itemStack = blockMenu.getItemInSlot(slots[0]);
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                continue;
            }
            for (Iterator<BatchTake> iterator = pending.iterator(); iterator.hasNext(); ) {
                final BatchTake take = iterator.next();
                if (!StackUtils.itemsMatch(take.request, itemStack)) {
                    continue;
                }

                // Mark the Cell as dirty otherwise the changes will not save on shutdown
                blockMenu.markDirty();

                // If the return stack is null, we need to set it up
                if (take.collected == null) {
                    take.collected = itemStack.clone();
                    take.collected.setAmount(0);
                }

                if (take.request.getAmount() <= itemStack.getAmount()) {
                    // Netex - Reduce start
                    // Netex - Reduce end
                    // We can't take more than this stack. Level to request amount, remove items and then return
                    take.collected.setAmount(take.collected.getAmount() + take.request.getAmount());
                    itemStack.setAmount(itemStack.getAmount() - take.request.getAmount());
                    // Netex - Record start
                    tryRecord(accessor, take.request);
                    // Netex - Record end
                    results.set(take.index, take.collected);
                    iterator.remove();
                } else {
                    // We can take more than what is here, consume before trying to take more
                    take.collected.setAmount(take.collected.getAmount() + itemStack.getAmount());
                    take.request.receiveAmount(itemStack.getAmount());
                    itemStack.setAmount(0);
                }
            }
        }

        for (BatchTake take : pending) {
            if (take.collected == null || take.collected.getAmount() == 0) {
            } else {
                // Netex - Reduce start
                // Netex - Reduce end
                // Netex - Record start
                tryRecord(accessor, take.request);
                // Netex - Record end
                results.set(take.index, take.collected);
            }
        }

        return results;
    }

    /** getItemStacksBatch0 的单请求运行态：入参索引对齐、聚合结果与访问历史段簿记。 */
    private static final class BatchTake {
        private final ItemRequest request;
        private final int index;
        @Nullable
        private ItemStack collected;
        private boolean historyFound;
        private final List<Location> historyMisses = new ArrayList<>();

        private BatchTake(@NotNull ItemRequest request, int index) {
            this.request = request;
            this.index = index;
        }
    }

    public void addItem(@NotNull Location accessor, @NotNull ItemStack incoming) {
        addItemStack0(accessor, incoming);
    }

    public void tryRecord(@NotNull Location accessor, @Nullable ItemStack before, int after) {
        if (recordFlow && itemFlowRecord != null && before != null) {
            itemFlowRecord.addAction(accessor, before, after);
        }
    }

    public void addItemStack0(@NotNull Location accessor, @NotNull ItemStack incoming) {
        if (StackUtils.isBlacklisted(incoming)) {
            return;
        }

        bumpWriteEpoch();

        ItemStack beforeItemStack = null;
        if (recordFlow && itemFlowRecord != null) {
            beforeItemStack = incoming.clone();
        }

        int before = incoming.getAmount();

        Map<Location, Integer> m = getPersistentAccessHistory(accessor);
        if (m != null) {
            // Netex - Cache start
            boolean found = false;
            List<Location> misses = new ArrayList<>();
            // Netex - Cache end
            for (Map.Entry<Location, Integer> entry : m.entrySet()) {
                BarrelIdentity barrelIdentity = accessInputAbleBarrel(entry.getKey());
                if (barrelIdentity != null) {
                    // <editor-fold desc="do barrel">
                    if (StackUtils.itemsMatch(barrelIdentity, incoming)) {
                        // Netex - Cache start
                        minusCacheMiss(accessor, entry.getKey());
                        found = true;
                        // Netex - Cache end

                        barrelIdentity.depositItemStack(incoming);

                        // All distributed, can escape
                        if (incoming.getAmount() == 0) {
                            // Netex - Reduce start
                            // Netex - Reduce end
                            // Netex - Record start
                            tryRecord(accessor, beforeItemStack, 0);
                            // Netex - Record end
                            return;
                        }
                    } else {
                        // Netex - Cache start
                        misses.add(entry.getKey());
                        // Netex - Cache end
                    }
                    // </editor-fold>
                } else {
                    StorageUnitData data = accessInputAbleCargoStorageUnitData(entry.getKey());
                    if (data != null) {
                        // Netex - Cache start
                        int before2 = incoming.getAmount();
                        // Netex - Cache end
                        data.depositItemStack0(accessor, incoming, true);

                        // Netex - Cache start
                        if (incoming.getAmount() != before2) {
                            minusCacheMiss(accessor, entry.getKey());
                            found = true;
                        } else {
                            misses.add(entry.getKey());
                        }
                        // Netex - Cache end

                        if (incoming.getAmount() == 0) {
                            // Netex - Reduce start
                            // Netex - Reduce end
                            // Netex - Record start
                            tryRecord(accessor, beforeItemStack, 0);
                            // Netex - Record end
                            return;
                        }
                    }
                }
            }

            // Netex - Cache start
            if (!found) {
                for (Location miss : misses) {
                    addCacheMiss(accessor, miss);
                }
            }
            // Netex - Cache end
        }

        for (BlockMenu blockMenu : getAdvancedGreedyBlockMenus()) {
            final ItemStack template = blockMenu.getItemInSlot(AdvancedGreedyBlock.TEMPLATE_SLOT);

            if (template == null || template.getType() == Material.AIR || !StackUtils.itemsMatch(incoming, template)) {
                continue;
            }

            blockMenu.markDirty();
            BlockMenuUtil.pushItem(blockMenu, incoming, ADVANCED_GREEDY_BLOCK_AVAILABLE_SLOTS);
            // Netex - Reduce start
            // Netex - Reduce end
            // Netex - Record start
            tryRecord(accessor, beforeItemStack, incoming.getAmount());
            // Netex - Record end
            // Given we have found a match, it doesn't matter if the item moved or not, we will not bring it in
            return;
        }

        // Run for matching greedy blocks
        for (BlockMenu blockMenu : getGreedyBlockMenus()) {
            final ItemStack template = blockMenu.getItemInSlot(NetworkGreedyBlock.TEMPLATE_SLOT);

            if (template == null || template.getType() == Material.AIR || !StackUtils.itemsMatch(incoming, template)) {
                continue;
            }

            blockMenu.markDirty();
            BlockMenuUtil.pushItem(blockMenu, incoming, GREEDY_BLOCK_AVAILABLE_SLOTS[0]);
            // Netex - Reduce start
            // Netex - Reduce end
            // Netex - Record start
            tryRecord(accessor, beforeItemStack, incoming.getAmount());
            // Netex - Record end
            // Given we have found a match, it doesn't matter if the item moved or not, we will not bring it in
            return;
        }

        // Run for matching barrels
        for (BarrelIdentity barrelIdentity : getInputAbleBarrels()) {
            // <editor-fold desc="do barrel">
            if (StackUtils.itemsMatch(barrelIdentity, incoming)) {
                // Netex - Cache start
                addCountObservingAccessHistory(accessor, barrelIdentity.getLocation());
                // Netex - Cache end

                barrelIdentity.depositItemStack(incoming);

                // All distributed, can escape
                if (incoming.getAmount() == 0) {
                    // Netex - Reduce start
                    // Netex - Reduce end
                    // Netex - Record start
                    tryRecord(accessor, beforeItemStack, 0);
                    // Netex - Record end
                    return;
                }
            }
            // </editor-fold>
        }

        for (StorageUnitData cache : getInputAbleCargoStorageUnitDatas().keySet()) {
            // Netex - Cache start
            int before2 = incoming.getAmount();
            // Netex - Cache end

            cache.depositItemStack0(accessor, incoming, true);

            // Netex - Cache start
            if (incoming.getAmount() != before2) {
                // Netex - Reduce start
                // Netex - Reduce end
                addCountObservingAccessHistory(accessor, cache.getLastLocation());
            }
            // Netex - Cache end

            if (incoming.getAmount() == 0) {
                // Netex - Reduce start
                // Netex - Reduce end
                // Netex - Record start
                tryRecord(accessor, beforeItemStack, 0);
                // Netex - Record end
                return;
            }
        }

        // Cell Drives
        long cellRemaining = CellDrive.getStorage().pushSingle(
            driveCache, getInputAbleCellDriveMenus(), incoming, incoming.getAmount());
        incoming.setAmount((int) Math.min(cellRemaining, Integer.MAX_VALUE));
        if (incoming.getAmount() == 0) {
            tryRecord(accessor, beforeItemStack, 0);
            return;
        }

        for (BlockMenu blockMenu : getCellMenus()) {
            if (!isRealCell(blockMenu)) continue;
            blockMenu.markDirty();
            BlockMenuUtil.pushItem(blockMenu, incoming, CELL_AVAILABLE_SLOTS);
            if (incoming.getAmount() == 0) {
                // Netex - Reduce start
                // Netex - Reduce end
                // Netex - Record start
                tryRecord(accessor, beforeItemStack, 0);
                // Netex - Record end
                return;
            }
        }

        // Netex - Reduce start
        if (before == incoming.getAmount()) {
            // No item moved, limit the accessor
        } else {
        }
        // Netex - Reduce end
        // Netex - Record start
        tryRecord(accessor, beforeItemStack, incoming.getAmount());
        // Netex - Record end
    }

    /**
     * 批量入库：一批物品按原有优先级走一遍存储，每种仓库只访问一次。
     * 未收完的物品保留剩余数量，由调用方回扣来源容器。
     */
    public void addItemStacks0(@NotNull Location accessor, @NotNull List<ItemStack> incomings) {
        if (incomings.isEmpty()) {
            return;
        }

        bumpWriteEpoch();

        int size = incomings.size();
        int[] beforeAmounts = new int[size];
        boolean[] done = new boolean[size];
        ItemStack[] beforeClones = recordFlow && itemFlowRecord != null ? new ItemStack[size] : null;
        int leftover = 0;
        for (int i = 0; i < size; i++) {
            ItemStack incoming = incomings.get(i);
            if (incoming.getAmount() <= 0 || StackUtils.isBlacklisted(incoming)) {
                done[i] = true;
                continue;
            }
            leftover++;
            beforeAmounts[i] = incoming.getAmount();
            if (beforeClones != null) {
                beforeClones[i] = incoming.clone();
            }
        }

        Map<Location, Integer> m = getPersistentAccessHistory(accessor);
        if (m != null) {
            List<Location> misses = new ArrayList<>();
            for (Map.Entry<Location, Integer> entry : m.entrySet()) {
                if (leftover <= 0) {
                    break;
                }
                boolean found = false;
                BarrelIdentity barrelIdentity = accessInputAbleBarrel(entry.getKey());
                if (barrelIdentity != null) {
                    for (int i = 0; i < size; i++) {
                        if (done[i] || incomings.get(i).getAmount() <= 0) {
                            continue;
                        }
                        if (StackUtils.itemsMatch(barrelIdentity, incomings.get(i))) {
                            found = true;
                            minusCacheMiss(accessor, entry.getKey());
                            barrelIdentity.depositItemStack(incomings.get(i));
                            if (incomings.get(i).getAmount() <= 0) {
                                done[i] = true;
                                leftover--;
                            }
                        }
                    }
                } else {
                    StorageUnitData data = accessInputAbleCargoStorageUnitData(entry.getKey());
                    if (data != null) {
                        for (int i = 0; i < size; i++) {
                            if (done[i] || incomings.get(i).getAmount() <= 0) {
                                continue;
                            }
                            int before2 = incomings.get(i).getAmount();
                            data.depositItemStack0(accessor, incomings.get(i), true);
                            if (incomings.get(i).getAmount() != before2) {
                                found = true;
                                minusCacheMiss(accessor, entry.getKey());
                                if (incomings.get(i).getAmount() <= 0) {
                                    done[i] = true;
                                    leftover--;
                                }
                            }
                        }
                    }
                }
                if (!found) {
                    misses.add(entry.getKey());
                }
            }
            for (Location miss : misses) {
                addCacheMiss(accessor, miss);
            }
        }

        if (leftover > 0) {
            for (BlockMenu blockMenu : getAdvancedGreedyBlockMenus()) {
                final ItemStack template = blockMenu.getItemInSlot(AdvancedGreedyBlock.TEMPLATE_SLOT);
                if (template == null || template.getType() == Material.AIR) {
                    continue;
                }
                for (int i = 0; i < size; i++) {
                    if (done[i] || incomings.get(i).getAmount() <= 0) {
                        continue;
                    }
                    if (!StackUtils.itemsMatch(incomings.get(i), template)) {
                        continue;
                    }
                    done[i] = true;
                    leftover--;
                    blockMenu.markDirty();
                    BlockMenuUtil.pushItem(blockMenu, incomings.get(i), ADVANCED_GREEDY_BLOCK_AVAILABLE_SLOTS);
                }
                if (leftover <= 0) {
                    break;
                }
            }
        }

        if (leftover > 0) {
            for (BlockMenu blockMenu : getGreedyBlockMenus()) {
                final ItemStack template = blockMenu.getItemInSlot(NetworkGreedyBlock.TEMPLATE_SLOT);
                if (template == null || template.getType() == Material.AIR) {
                    continue;
                }
                for (int i = 0; i < size; i++) {
                    if (done[i] || incomings.get(i).getAmount() <= 0) {
                        continue;
                    }
                    if (!StackUtils.itemsMatch(incomings.get(i), template)) {
                        continue;
                    }
                    done[i] = true;
                    leftover--;
                    blockMenu.markDirty();
                    BlockMenuUtil.pushItem(blockMenu, incomings.get(i), GREEDY_BLOCK_AVAILABLE_SLOTS[0]);
                }
                if (leftover <= 0) {
                    break;
                }
            }
        }

        if (leftover > 0) {
            for (BarrelIdentity barrelIdentity : getInputAbleBarrels()) {
                for (int i = 0; i < size; i++) {
                    if (done[i] || incomings.get(i).getAmount() <= 0) {
                        continue;
                    }
                    if (!StackUtils.itemsMatch(barrelIdentity, incomings.get(i))) {
                        continue;
                    }
                    addCountObservingAccessHistory(accessor, barrelIdentity.getLocation());
                    barrelIdentity.depositItemStack(incomings.get(i));
                    if (incomings.get(i).getAmount() <= 0) {
                        done[i] = true;
                        leftover--;
                    }
                }
                if (leftover <= 0) {
                    break;
                }
            }
        }

        if (leftover > 0) {
            for (StorageUnitData cache : getInputAbleCargoStorageUnitDatas().keySet()) {
                for (int i = 0; i < size; i++) {
                    if (done[i] || incomings.get(i).getAmount() <= 0) {
                        continue;
                    }
                    int before2 = incomings.get(i).getAmount();
                    cache.depositItemStack0(accessor, incomings.get(i), true);
                    if (incomings.get(i).getAmount() != before2) {
                        addCountObservingAccessHistory(accessor, cache.getLastLocation());
                        if (incomings.get(i).getAmount() <= 0) {
                            done[i] = true;
                            leftover--;
                        }
                    }
                }
                if (leftover <= 0) {
                    break;
                }
            }
        }

        if (leftover > 0) {
            List<ItemStack> cellBatch = new ArrayList<>(leftover);
            for (int i = 0; i < size; i++) {
                if (!done[i] && incomings.get(i).getAmount() > 0) {
                    cellBatch.add(incomings.get(i));
                }
            }
            CellDrive.getStorage().pushMany(driveCache, getInputAbleCellDriveMenus(), cellBatch);
            for (int i = 0; i < size; i++) {
                if (!done[i] && incomings.get(i).getAmount() == 0) {
                    done[i] = true;
                    leftover--;
                }
            }
        }

        if (leftover > 0) {
            for (BlockMenu blockMenu : getCellMenus()) {
                if (!isRealCell(blockMenu)) {
                    continue;
                }
                boolean pushed = false;
                for (int i = 0; i < size; i++) {
                    if (done[i] || incomings.get(i).getAmount() <= 0) {
                        continue;
                    }
                    if (!pushed) {
                        blockMenu.markDirty();
                        pushed = true;
                    }
                    BlockMenuUtil.pushItem(blockMenu, incomings.get(i), CELL_AVAILABLE_SLOTS);
                    if (incomings.get(i).getAmount() == 0) {
                        done[i] = true;
                        leftover--;
                    }
                }
                if (leftover <= 0) {
                    break;
                }
            }
        }

        if (beforeClones != null) {
            for (int i = 0; i < size; i++) {
                if (beforeClones[i] != null) {
                    tryRecord(accessor, beforeClones[i], incomings.get(i).getAmount());
                }
            }
        }
    }

    public Map<Location, BarrelIdentity> getMapInputAbleBarrels() {
        if (this.mapInputAbleBarrels != null) {
            return this.mapInputAbleBarrels;
        }

        final Map<Location, BarrelIdentity> map = new ConcurrentHashMap<>();
        for (BarrelIdentity barrel : getInputAbleBarrels()) {
            map.put(barrel.getLocation(), barrel);
        }
        this.mapInputAbleBarrels = map;
        return map;
    }

    public Map<Location, BarrelIdentity> getMapOutputAbleBarrels() {
        if (this.mapOutputAbleBarrels != null) {
            return this.mapOutputAbleBarrels;
        }

        final Map<Location, BarrelIdentity> map = new ConcurrentHashMap<>();
        for (BarrelIdentity barrel : getOutputAbleBarrels()) {
            map.put(barrel.getLocation(), barrel);
        }
        this.mapOutputAbleBarrels = map;
        return map;
    }

    public Map<Location, StorageUnitData> getMapInputAbleCargoStorageUnits() {
        if (this.mapInputAbleCargoStorageUnits != null) {
            return this.mapInputAbleCargoStorageUnits;
        }

        final Map<Location, StorageUnitData> map = new ConcurrentHashMap<>();
        for (Map.Entry<StorageUnitData, Location> entry :
            getInputAbleCargoStorageUnitDatas().entrySet()) {
            map.put(entry.getValue(), entry.getKey());
        }
        this.mapInputAbleCargoStorageUnits = map;
        return map;
    }

    public Map<Location, StorageUnitData> getMapOutputAbleCargoStorageUnits() {
        if (this.mapOutputAbleCargoStorageUnits != null) {
            return this.mapOutputAbleCargoStorageUnits;
        }

        final Map<Location, StorageUnitData> map = new ConcurrentHashMap<>();
        for (Map.Entry<StorageUnitData, Location> entry :
            getOutputAbleCargoStorageUnitDatas().entrySet()) {
            map.put(entry.getValue(), entry.getKey());
        }
        this.mapOutputAbleCargoStorageUnits = map;
        return map;
    }


    public boolean allowAccessInput(@NotNull Location accessor) {
        Long lastTime = controlledAccessInputHistory.get(accessor);
        if (lastTime == null) {
            return true;
        } else {
            return System.currentTimeMillis() - lastTime > reduceMs;
        }
    }

    public boolean allowAccessOutput(@NotNull Location accessor) {
        Long lastTime = controlledAccessOutputHistory.get(accessor);
        if (lastTime == null) {
            return true;
        } else {
            return System.currentTimeMillis() - lastTime > reduceMs;
        }
    }

    public void addTransportInputMiss(@NotNull Location location) {
        transportMissInputHistory.merge(location, 1, (a, b) -> {
            if (a + b > transportMissThreshold) {
                controlAccessInput(location);
                return transportMissThreshold;
            } else {
                return a + b;
            }
        });
    }

    public void addTransportOutputMiss(@NotNull Location location) {
        transportMissOutputHistory.merge(location, 1, (a, b) -> {
            if (a + b > transportMissThreshold) {
                controlAccessOutput(location);
                return transportMissThreshold;
            } else {
                return a + b;
            }
        });
    }

    public void reduceTransportInputMiss(@NotNull Location location) {
        transportMissInputHistory.merge(location, -1, (a, b) -> Math.max(a + b, 0));
    }

    public void reduceTransportOutputMiss(@NotNull Location location) {
        transportMissOutputHistory.merge(location, -1, (a, b) -> Math.max(a + b, 0));
    }

    public void controlAccessInput(@NotNull Location accessor) {
        controlledAccessInputHistory.put(accessor, System.currentTimeMillis());
    }

    public void controlAccessOutput(@NotNull Location accessor) {
        controlledAccessOutputHistory.put(accessor, System.currentTimeMillis());
    }

    public void uncontrolAccessInput(@NotNull Location accessor) {
        controlledAccessInputHistory.remove(accessor);
        reduceTransportInputMiss(accessor);
    }

    public void uncontrolAccessOutput(@NotNull Location accessor) {
        controlledAccessOutputHistory.remove(accessor);
        reduceTransportOutputMiss(accessor);
    }

    public int getCellsSize() {
        if (cellsSize != -1) {
            return cellsSize;
        }

        cellsSize = getCells().size();
        return cellsSize;
    }

    public static boolean isRealCell(BlockMenu menu) {
        return StorageCacheUtils.getSfItem(menu.getLocation()) instanceof NetworkCell;
    }
}
