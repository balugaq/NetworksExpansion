package com.ytdd9527.networksexpansion.implementation.machines.cellnet.chain;

import com.balugaq.netex.api.enums.FeedbackType;
import com.balugaq.netex.api.enums.TransferType;
import com.balugaq.netex.api.enums.TransportMode;
import com.balugaq.netex.utils.Debug;
import com.balugaq.netex.utils.LineOperationUtil;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.slimefun.network.NetworkDirectional;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ItemKey;
import com.ytdd9527.networksexpansion.utils.itemstacks.ItemStackUtil;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractChainMachine extends NetworkObject {

    public enum WorkMode {
        GRAB,
        PUSH,
        BOTH
    }

    public static final int MODE_SLOT = 29;
    public static final int DISTANCE_SLOT = 21;
    public static final int[] TEMPLATE_SLOTS = {15, 16, 17, 24, 25, 26, 33, 34, 35};
    public static final int MODULE_START = 36;
    public static final int MODULE_COUNT = 8;

    static final String KEY_DISTANCE = "chain_distance";
    static final String KEY_MODE = "chain_mode";
    static final String KEY_MULTI_DIRS = "chain_dirs";
    static final String KEY_DIRECTION = "direction";
    private static final int BASE_TEMPLATE_SLOTS = 3;
    private static final int TEMPLATE_SLOTS_PER_MODULE = 3;
    private static final int MAX_COUNTED_RANGE_MODULES = 3;
    private static final int MAX_COUNTED_CAPACITY_MODULES = 2;
    private static final int NEG_CACHE_ROUNDS = 3;

    private static final Map<Location, LineState> STATES = new ConcurrentHashMap<>();

    private static final Map<Object, ItemKey> LIVE_KEY_CACHE = new IdentityHashMap<>();

    private static final int LIVE_KEY_CACHE_LIMIT = 1024;

    private final IntRangeSetting tickRate = new IntRangeSetting(this, "tick_rate", 1, 5, 10);

    protected AbstractChainMachine(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.CRAFTER);

        addItemSetting(tickRate);
        addItemHandler(new WorkTicker());
        if (getWorkMode() != WorkMode.GRAB) {
            for (int slot : TEMPLATE_SLOTS) {
                getSlotsToDrop().add(slot);
            }
        }
        for (int i = 0; i < MODULE_COUNT; i++) {
            getSlotsToDrop().add(MODULE_START + i);
        }
    }

    @Override
    protected void onBreak(@NotNull BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        ChainRangeParticles.stop(location);
        BlockMenu menu = StorageCacheUtils.getMenu(location);
        if (menu == null) {
            STATES.remove(location);
            super.onBreak(event);
            return;
        }
        clearDisplayIcons(menu);
        List<Integer> realSlots = new ArrayList<>();
        for (int slot : getSlotsToDrop()) {
            ItemStack current = menu.getItemInSlot(slot);
            if (current == null || current.getType().isAir()) {
                continue;
            }
            boolean moduleSlot = slot >= MODULE_START && slot < MODULE_START + MODULE_COUNT;
            if (isTemplatePlaceholder(current) || (moduleSlot && ChainModule.of(current) == null)) {
                menu.replaceExistingItem(slot, null);
                continue;
            }
            realSlots.add(slot);
        }
        menu.dropItems(location, realSlots.stream().mapToInt(Integer::intValue).toArray());
        Slimefun.getDatabaseManager().getBlockDataController().removeBlock(location);
        STATES.remove(location);
    }

    protected void clearDisplayIcons(@NotNull BlockMenu menu) {
    }

    @NotNull
    public abstract TransferType getTransferType();

    @NotNull
    public abstract WorkMode getWorkMode();

    protected abstract void wireMenu(@NotNull BlockMenu menu, @NotNull LineState state);

    protected int requiredPower() {
        return getTransferType().config("required-power", 0);
    }

    protected int workTtlTicks() {
        return Networks.getConfigManager().getChainCacheTtlTicks();
    }

    protected int idleTtlTicks() {
        return Networks.getConfigManager().getChainIdleTtlTicks();
    }

    protected int baseDistance() {
        return Networks.getConfigManager().getChainBaseDistance();
    }

    protected int distancePerModule() {
        return Networks.getConfigManager().getChainDistancePerModule();
    }

    protected int maxDistance() {
        return Networks.getConfigManager().getChainMaxDistance();
    }

    private final class WorkTicker extends BlockTicker {

        @Override
        public boolean isSynchronized() {
            return false;
        }

        @Override
        public void tick(@NotNull Block block, SlimefunItem item, @NotNull SlimefunBlockData data) {
            addToRegistry(block);
            BlockMenu menu = data.getBlockMenu();
            if (menu == null) {
                return;
            }
            LineState state = STATES.computeIfAbsent(menu.getLocation(), k -> new LineState());
            long now = System.currentTimeMillis();
            if (now < state.nextWorkMs) {
                return;
            }
            state.nextWorkMs = now + gateIntervalMs();
            if (!state.wired) {
                state.wired = true;
                Bukkit.getScheduler().runTask(Networks.getInstance(), () -> wireMenu(menu, state));
            }
            int power = requiredPower();
            NetworkRoot root = rootOf(menu);
            if (root == null) {
                sendFeedback(menu.getLocation(), FeedbackType.NO_NETWORK_FOUND);
                return;
            }
            if (root.getRootPower() < power) {
                sendFeedback(menu.getLocation(), FeedbackType.NOT_ENOUGH_POWER);
                return;
            }
            Bukkit.getScheduler().runTask(Networks.getInstance(),
                () -> runWorkBody(menu, state, root, power));
        }
    }

    private int gateIntervalMs() {
        int seconds = Networks.getConfigManager().getChainWorkIntervalSeconds();
        if (seconds >= 1) {
            return Math.min(seconds, 60) * 1000;
        }
        return 1000 / Math.max(1, tickRate.getValue());
    }

    private void runWorkBody(
        @NotNull BlockMenu menu, @NotNull LineState state, @NotNull NetworkRoot root, int power) {
        if (state.directions.isEmpty()) {
            loadWorkState(menu.getLocation(), state);
        }
        boolean worked = false;
        long acceptedTotal = 0L;
        Set<Location> seenTargets = new HashSet<>();
        List<BlockMenu> batchTargets = new ArrayList<>();
        List<long[]> batchSigArrays = new ArrayList<>();
        List<Integer> batchSigIndexes = new ArrayList<>();
        TransportMode mode = state.modeModule ? state.transportMode : TransportMode.NONE;
        int distance = effectiveDistance(state);
        Set<BlockFace> dirs = state.effectiveDirections();
        List<ItemStack> templates = getWorkMode() != WorkMode.GRAB
            ? collectTemplates(menu, state)
            : List.of();
        List<ItemRequest> templateRequests = getWorkMode() != WorkMode.GRAB
            ? templateRequestsOf(state, templates)
            : List.of();
        for (BlockFace direction : dirs) {
            List<Location> targets = ChainTargetCache.targets(menu.getLocation(), direction,
                distance, state.vanilla, workTtlTicks(), idleTtlTicks());
            if (targets.isEmpty()) {
                state.cursors.remove(direction);
                continue;
            }
            int stride = Math.max(1, targets.size() / 4);
            Integer cursor = state.cursors.get(direction);
            int start = cursor == null ? 0 : cursor % targets.size();
            List<Location> window = new ArrayList<>(stride);
            List<Integer> windowIndexes = state.bindingModule ? new ArrayList<>(stride) : null;
            for (int k = 0; k < stride; k++) {
                int index = (start + k) % targets.size();
                Location target = targets.get(index);
                if (seenTargets.add(target)) {
                    window.add(target);
                    if (windowIndexes != null) {
                        windowIndexes.add(index);
                    }
                }
            }
            state.cursors.put(direction, (start + stride) % targets.size());

            BlockMenu[] resolved = new BlockMenu[window.size()];
            for (int i = 0; i < window.size(); i++) {
                resolved[i] = ChainTargetCache.resolveMenu(window.get(i), state.vanilla);
            }
            if (getWorkMode() != WorkMode.GRAB) {
                if (state.bindingModule) {
                    if (pushWithBindings(menu, state, root, direction, window, windowIndexes,
                        resolved, templates, templateRequests, mode)) {
                        worked = true;
                    }
                } else if (!templateRequests.isEmpty()) {
                    for (int i = 0; i < resolved.length; i++) {
                        if (resolved[i] == null) {
                            continue;
                        }
                        LineOperationUtil.pushRequests(window.get(i), root, resolved[i], templateRequests, mode, Integer.MAX_VALUE);
                        worked = true;
                    }
                }
            }
            if (getWorkMode() != WorkMode.PUSH) {
                List<BlockMenu> grabTargets = new ArrayList<>(resolved.length);
                List<Integer> grabIndexes = new ArrayList<>(resolved.length);
                for (int i = 0; i < resolved.length; i++) {
                    if (resolved[i] != null) {
                        grabTargets.add(resolved[i]);
                        grabIndexes.add(i);
                    }
                }
                if (mode == TransportMode.NONE || mode == TransportMode.NONNULL_ONLY) {
                    long[] signatures = state.targetSignatures
                        .computeIfAbsent(direction, k -> new long[targets.size()]);
                    if (signatures.length != targets.size()) {
                        signatures = new long[targets.size()];
                        state.targetSignatures.put(direction, signatures);
                    }
                    for (int i = 0; i < grabTargets.size(); i++) {
                        batchTargets.add(grabTargets.get(i));
                        batchSigArrays.add(signatures);
                        batchSigIndexes.add(grabIndexes.get(i));
                    }
                } else {
                    for (BlockMenu targetMenu : grabTargets) {
                        LineOperationUtil.grabItem(menu.getLocation(), root, targetMenu, mode, Integer.MAX_VALUE);
                        worked = true;
                    }
                }
            }
        }
        if ((mode == TransportMode.NONE || mode == TransportMode.NONNULL_ONLY) && !batchTargets.isEmpty()) {
            int[] sigIndexes = new int[batchSigIndexes.size()];
            for (int i = 0; i < sigIndexes.length; i++) {
                sigIndexes[i] = batchSigIndexes.get(i);
            }
            acceptedTotal += runGrabBatch(menu.getLocation(), root, batchTargets, batchSigArrays, sigIndexes);
        }
        if (acceptedTotal > 0) {
            worked = true;
        }
        if (worked) {
            root.removeRootPower(power);
            sendFeedback(menu.getLocation(), FeedbackType.WORKING);
        }
    }

    private boolean pushWithBindings(
        @NotNull BlockMenu menu, @NotNull LineState state, @NotNull NetworkRoot root,
        @NotNull BlockFace direction, @NotNull List<Location> window,
        @NotNull List<Integer> windowIndexes, BlockMenu[] resolved,
        @NotNull List<ItemStack> templates, @NotNull List<ItemRequest> templateRequests,
        @NotNull TransportMode machineMode) {
        boolean worked = false;
        Map<Integer, List<ItemStack>> bindingMap = bindingsFor(state, menu.getLocation(), direction);
        for (int i = 0; i < resolved.length; i++) {
            if (resolved[i] == null) {
                continue;
            }
            int distance = windowIndexes.get(i) + 1;
            List<ItemStack> bound = bindingMap.get(distance);
            List<ItemRequest> use;
            boolean boundPush;
            if (bound == null) {
                if (templates.isEmpty()) {
                    continue;
                }
                use = templateRequests;
                boundPush = false;
            } else {
                if (bound.isEmpty()) {
                    continue;
                }
                use = boundRequestsOf(state, bound);
                boundPush = true;
            }
            TransportMode mode = resolveMode(state, direction, machineMode);
            if (boundPush && negCacheSkip(state, direction, distance)) {
                continue;
            }
            LineOperationUtil.pushRequests(window.get(i), root, resolved[i], use, mode, Integer.MAX_VALUE);
            if (boundPush) {
                afterBoundPush(state, root, direction, distance, bound);
            }
            worked = true;
        }
        return worked;
    }

    private @NotNull List<ItemRequest> templateRequestsOf(
        @NotNull LineState state, @NotNull List<ItemStack> templates) {
        List<ItemStack> refs = state.templateRefs;
        List<ItemRequest> cached = state.templateRequests;
        if (cached != null && refs != null && refs.size() == templates.size()) {
            boolean same = true;
            for (int i = 0; i < templates.size(); i++) {
                if (refs.get(i) != templates.get(i)) {
                    same = false;
                    break;
                }
            }
            if (same) {
                return cached;
            }
        }
        List<ItemRequest> built = new ArrayList<>(templates.size());
        for (ItemStack template : templates) {
            built.add(new ItemRequest(template, template.getMaxStackSize()));
        }
        state.templateRefs = templates;
        state.templateRequests = built;
        return built;
    }

    private @NotNull List<ItemRequest> boundRequestsOf(
        @NotNull LineState state, @NotNull List<ItemStack> bound) {
        List<ItemRequest> cached = state.boundRequestCache.get(bound);
        if (cached != null) {
            return cached;
        }
        List<ItemRequest> built = new ArrayList<>(bound.size());
        for (ItemStack template : bound) {
            built.add(new ItemRequest(template, template.getMaxStackSize()));
        }
        if (state.boundRequestCache.size() >= 64) {
            state.boundRequestCache.clear();
        }
        state.boundRequestCache.put(bound, built);
        return built;
    }

    @NotNull
    private TransportMode resolveMode(
        @NotNull LineState state, @NotNull BlockFace direction, @NotNull TransportMode machineMode) {
        TransportMode override = dirModeOverride(state, direction);
        return state.modeModule && override != null ? override : machineMode;
    }

    @Nullable
    private TransportMode dirModeOverride(@NotNull LineState state, @NotNull BlockFace direction) {
        Map<BlockFace, TransportMode> overrides = state.dirModes;
        return overrides == null ? null : overrides.get(direction);
    }

    @NotNull
    private Map<Integer, List<ItemStack>> bindingsFor(
        @NotNull LineState state, @NotNull Location location, @NotNull BlockFace direction) {
        Map<BlockFace, Map<Integer, List<ItemStack>>> cache = state.bindings;
        if (cache == null) {
            cache = new EnumMap<>(BlockFace.class);
            state.bindings = cache;
        }
        return cache.computeIfAbsent(direction, face -> ChainBindingStore.load(location, face));
    }

    private boolean negCacheSkip(@NotNull LineState state, @NotNull BlockFace direction, int distance) {
        Map<BlockFace, Map<Integer, Integer>> neg = state.negCache;
        if (neg == null) {
            return false;
        }
        Map<Integer, Integer> rounds = neg.get(direction);
        if (rounds == null) {
            return false;
        }
        Integer remaining = rounds.get(distance);
        if (remaining == null) {
            return false;
        }
        if (remaining <= 1) {
            rounds.remove(distance);
        } else {
            rounds.put(distance, remaining - 1);
        }
        return true;
    }

    private void afterBoundPush(
        @NotNull LineState state, @NotNull NetworkRoot root,
        @NotNull BlockFace direction, int distance, @NotNull List<ItemStack> bound) {
        if (anyInStock(root, bound)) {
            clearNegCache(state, direction, distance);
            return;
        }
        Map<BlockFace, Map<Integer, Integer>> neg = state.negCache;
        if (neg == null) {
            neg = new EnumMap<>(BlockFace.class);
            state.negCache = neg;
        }
        neg.computeIfAbsent(direction, face -> new HashMap<>()).put(distance, NEG_CACHE_ROUNDS);
    }

    private boolean anyInStock(@NotNull NetworkRoot root, @NotNull List<ItemStack> bound) {
        for (ItemStack sample : bound) {
            if (root.getAmount(sample) > 0) {
                return true;
            }
        }
        return false;
    }

    private void clearNegCache(@NotNull LineState state, @NotNull BlockFace direction, int distance) {
        Map<BlockFace, Map<Integer, Integer>> neg = state.negCache;
        if (neg == null) {
            return;
        }
        Map<Integer, Integer> rounds = neg.get(direction);
        if (rounds != null) {
            rounds.remove(distance);
        }
    }

    protected static void invalidateBindings(@NotNull LineState state, @Nullable BlockFace direction) {
        state.boundRequestCache.clear();
        if (direction == null) {
            state.bindings = null;
            state.negCache = null;
            return;
        }
        Map<BlockFace, Map<Integer, List<ItemStack>>> bindings = state.bindings;
        if (bindings != null) {
            bindings.remove(direction);
        }
        Map<BlockFace, Map<Integer, Integer>> neg = state.negCache;
        if (neg != null) {
            neg.remove(direction);
        }
    }

    private static @NotNull ItemKey liveKeyOf(@NotNull ItemStack item) {
        final Object identity = ItemStackUtil.nmsIdentity(item);
        if (identity == null) {
            return ItemKey.exactLive(item);
        }
        final ItemKey cached = LIVE_KEY_CACHE.get(identity);
        if (cached != null) {
            return cached;
        }
        final ItemKey key = ItemKey.exactLive(item);
        if (LIVE_KEY_CACHE.size() >= LIVE_KEY_CACHE_LIMIT) {
            LIVE_KEY_CACHE.clear();
        }
        LIVE_KEY_CACHE.put(identity, key);
        return key;
    }

    private long runGrabBatch(
        @NotNull Location accessor, @NotNull NetworkRoot root,
        @NotNull List<BlockMenu> grabTargets,
        @NotNull List<long[]> sigArrays, @NotNull int[] sigIndexes) {
        Map<Material, Long> plainMerged = new EnumMap<>(Material.class);
        Map<Material, List<int[]>> plainHolders = new EnumMap<>(Material.class);
        List<ItemStack> metaReps = new ArrayList<>();
        List<Long> metaTotals = new ArrayList<>();
        List<List<int[]>> metaHolders = new ArrayList<>();
        Map<ItemKey, Integer> metaGroupIndex = new HashMap<>();
        List<ItemStack[]> liveSlots = new ArrayList<>(grabTargets.size());
        List<Integer> changedIndexes = new ArrayList<>(grabTargets.size());
        List<long[]> changedSigArrays = new ArrayList<>(grabTargets.size());
        List<Integer> changedSigIndexes = new ArrayList<>(grabTargets.size());
        List<int[]> changedSlotArrays = new ArrayList<>(grabTargets.size());
        for (int m = 0; m < grabTargets.size(); m++) {
            BlockMenu targetMenu = grabTargets.get(m);
            int[] slots = targetMenu.getPreset()
                .getSlotsAccessedByItemTransport(targetMenu, ItemTransportFlow.WITHDRAW, null);
            ItemStack[] live = new ItemStack[slots.length];
            liveSlots.add(live);
            for (int si = 0; si < slots.length; si++) {
                live[si] = targetMenu.getItemInSlot(slots[si]);
            }
            long hash = signature(live);
            long[] sigArray = sigArrays.get(m);
            int sigIndex = sigIndexes[m];
            if (sigArray != null && sigIndex < sigArray.length && sigArray[sigIndex] == hash) {
                continue;
            }
            int ci = changedIndexes.size();
            changedIndexes.add(m);
            changedSigArrays.add(sigArray);
            changedSigIndexes.add(sigIndex);
            changedSlotArrays.add(slots);
            for (int si = 0; si < live.length; si++) {
                ItemStack item = live[si];
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }
                if (ItemStackUtil.hasCustomComponents(item)) {
                    long amount = item.getAmount();
                    ItemKey key = liveKeyOf(item);
                    Integer group = metaGroupIndex.get(key);
                    if (group == null) {
                        group = metaReps.size();
                        metaGroupIndex.put(key, group);
                        metaReps.add(item.asOne());
                        metaTotals.add(amount);
                        metaHolders.add(new ArrayList<>());
                    } else {
                        metaTotals.set(group, metaTotals.get(group) + amount);
                    }
                    metaHolders.get(group).add(new int[]{ci, si});
                } else {
                    Material type = item.getType();
                    long amount = item.getAmount();
                    plainMerged.merge(type, amount, Long::sum);
                    plainHolders.computeIfAbsent(type, k -> new ArrayList<>()).add(new int[]{ci, si});
                }
            }
        }
        if (plainMerged.isEmpty() && metaReps.isEmpty()) {
            return 0L;
        }
        List<ItemStack> requests = new ArrayList<>();
        List<Long> wants = new ArrayList<>();
        List<List<int[]>> holderLists = new ArrayList<>();
        List<Material> holderTypes = new ArrayList<>();
        for (Map.Entry<Material, Long> entry : plainMerged.entrySet()) {
            Material type = entry.getKey();
            ItemStack request = new ItemStack(type);
            request.setAmount((int) Math.min(entry.getValue(), Integer.MAX_VALUE));
            requests.add(request);
            wants.add(entry.getValue());
            holderLists.add(plainHolders.get(type));
            holderTypes.add(type);
        }
        for (int g = 0; g < metaReps.size(); g++) {
            ItemStack request = metaReps.get(g).asOne();
            request.setAmount((int) Math.min(metaTotals.get(g), Integer.MAX_VALUE));
            requests.add(request);
            wants.add(metaTotals.get(g));
            holderLists.add(metaHolders.get(g));
            holderTypes.add(metaReps.get(g).getType());
        }
        root.addItemStacks0(accessor, requests);

        long acceptedTotal = 0L;
        for (int i = 0; i < requests.size(); i++) {
            long accepted = wants.get(i) - requests.get(i).getAmount();
            if (accepted <= 0) {
                continue;
            }
            acceptedTotal += accepted;
            long remaining = accepted;
            for (int[] holder : holderLists.get(i)) {
                if (remaining <= 0) {
                    break;
                }
                int m = changedIndexes.get(holder[0]);
                ItemStack item = liveSlots.get(m)[holder[1]];
                if (item == null || item.getType() != holderTypes.get(i)) {
                    continue;
                }
                long take = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - (int) take);
                remaining -= take;
                if (item.getAmount() == 0) {
                    grabTargets.get(m).replaceExistingItem(
                        changedSlotArrays.get(holder[0])[holder[1]], null);
                }
            }
        }
        if (acceptedTotal > 0) {
            for (int ci = 0; ci < changedIndexes.size(); ci++) {
                long[] sigArray = changedSigArrays.get(ci);
                if (sigArray == null || changedSigIndexes.get(ci) >= sigArray.length) {
                    continue;
                }
                int m = changedIndexes.get(ci);
                ItemStack[] live = liveSlots.get(m);
                sigArray[changedSigIndexes.get(ci)] = signature(live);
            }
        }
        return acceptedTotal;
    }

    private static long signature(@NotNull ItemStack @NotNull [] slots) {
        long hash = 17L;
        for (ItemStack item : slots) {
            int v = item == null || item.getType() == Material.AIR
                ? 0
                : (item.getType().ordinal() * 31 + item.getAmount());
            hash = hash * 31 + v;
        }
        return hash;
    }
    private @Nullable NetworkRoot rootOf(@NotNull BlockMenu menu) {
        NodeDefinition definition = NetworkStorage.getNode(menu.getLocation());
        if (definition == null || definition.getNode() == null) {
            return null;
        }
        return definition.getNode().getRoot();
    }

    private @NotNull List<ItemStack> collectTemplates(@NotNull BlockMenu menu, @NotNull LineState state) {
        List<ItemStack> templates = new ArrayList<>();
        for (int i = 0; i < templateSlotCount(state); i++) {
            ItemStack item = menu.getItemInSlot(TEMPLATE_SLOTS[i]);
            if (item != null && item.getType() != Material.AIR && !isTemplatePlaceholder(item)) {
                templates.add(item);
            }
        }
        return templates;
    }

    protected static boolean isTemplatePlaceholder(@Nullable ItemStack item) {
        if (item == null || item.getType() != Material.RED_STAINED_GLASS_PANE) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null
            && meta.getPersistentDataContainer().has(PLACEHOLDER_KEY, PersistentDataType.BYTE);
    }

    static final NamespacedKey PLACEHOLDER_KEY =
        new NamespacedKey(Networks.getInstance(), "chain_placeholder");

    void recountModules(@NotNull BlockMenu menu, @NotNull LineState state) {
        state.rangeModules = 0;
        state.capacityModules = 0;
        state.modeModule = false;
        state.vanilla = false;
        state.multiDirection = false;
        boolean bindingModule = false;
        for (int i = 0; i < MODULE_COUNT; i++) {
            ChainModule module = ChainModule.of(menu.getItemInSlot(MODULE_START + i));
            if (module == null) {
                continue;
            }
            switch (module) {
                case RANGE -> state.rangeModules++;
                case CAPACITY -> state.capacityModules++;
                case MODE -> state.modeModule = true;
                case VANILLA -> state.vanilla = true;
                case MULTI_DIRECTION -> state.multiDirection = true;
                case BINDING -> bindingModule = true;
            }
        }
        if (state.bindingModule != bindingModule) {
            state.bindingModule = bindingModule;
            invalidateBindings(state, null);
        }
        if (bindingModule) {
            state.dirModes = loadDirModes(menu.getLocation());
        } else {
            state.dirModes = null;
        }
    }

    @NotNull
    private static Map<BlockFace, TransportMode> loadDirModes(@NotNull Location location) {
        Map<BlockFace, TransportMode> overrides = new EnumMap<>(BlockFace.class);
        for (BlockFace face : NetworkDirectional.VALID_FACES) {
            TransportMode mode = ChainBindingStore.loadDirMode(location, face);
            if (mode != null) {
                overrides.put(face, mode);
            }
        }
        return overrides;
    }

    protected int distanceCap(@NotNull LineState state) {
        int counted = Math.min(state.rangeModules, MAX_COUNTED_RANGE_MODULES);
        return Math.min(baseDistance() + counted * distancePerModule(), maxDistance());
    }

    protected int effectiveDistance(@NotNull LineState state) {
        return Math.max(1, Math.min(state.distance, distanceCap(state)));
    }

    protected int templateSlotCount(@NotNull LineState state) {
        int counted = Math.min(state.capacityModules, MAX_COUNTED_CAPACITY_MODULES);
        return Math.min(BASE_TEMPLATE_SLOTS + counted * TEMPLATE_SLOTS_PER_MODULE, TEMPLATE_SLOTS.length);
    }

    protected static @NotNull LineState state(@NotNull Location location) {
        return STATES.computeIfAbsent(location, k -> new LineState());
    }

    protected static @NotNull LineState loadedState(@NotNull Location location) {
        LineState state = state(location);
        if (state.directions.isEmpty()) {
            loadWorkState(location, state);
        }
        return state;
    }

    static @Nullable AbstractChainMachine machineAt(@NotNull Location location) {
        SlimefunItem item = StorageCacheUtils.getSfItem(location);
        return item instanceof AbstractChainMachine machine ? machine : null;
    }

    static boolean hasModule(@NotNull Location location, @NotNull ChainModule module) {
        BlockMenu menu = StorageCacheUtils.getMenu(location);
        if (menu == null) {
            return false;
        }
        for (int i = 0; i < MODULE_COUNT; i++) {
            if (ChainModule.of(menu.getItemInSlot(MODULE_START + i)) == module) {
                return true;
            }
        }
        return false;
    }

    protected static void saveWorkState(@NotNull Location location, @NotNull LineState state) {
        if (!state.directions.isEmpty()) {
            List<String> dirs = new ArrayList<>();
            for (BlockFace face : state.directions) {
                dirs.add(face.name());
            }
            StorageCacheUtils.setData(location, KEY_MULTI_DIRS, String.join(",", dirs));
        }
        StorageCacheUtils.setData(location, KEY_DISTANCE, String.valueOf(state.distance));
        StorageCacheUtils.setData(location, KEY_MODE, state.transportMode.name());
    }

    protected static void loadWorkState(@NotNull Location location, @NotNull LineState state) {
        String dirs = StorageCacheUtils.getData(location, KEY_MULTI_DIRS);
        if (dirs == null || dirs.isEmpty()) {
            dirs = StorageCacheUtils.getData(location, KEY_DIRECTION);
        }
        if (dirs != null && !dirs.isEmpty()) {
            state.directions.clear();
            for (String name : dirs.split(",")) {
                try {
                    state.directions.add(BlockFace.valueOf(name));
                } catch (IllegalArgumentException e) {
                    Debug.debug("多方向记录非法，跳过: " + name);
                }
            }
        }
        String distance = StorageCacheUtils.getData(location, KEY_DISTANCE);
        if (distance != null) {
            try {
                state.distance = Math.max(1, Integer.parseInt(distance));
            } catch (NumberFormatException e) {
                state.distance = 1;
            }
        }
        String mode = StorageCacheUtils.getData(location, KEY_MODE);
        if (mode != null) {
            try {
                state.transportMode = TransportMode.valueOf(mode);
            } catch (IllegalArgumentException e) {
                state.transportMode = TransportMode.NONE;
            }
        }
    }

    public static final class LineState {
        final Set<BlockFace> directions = ConcurrentHashMap.newKeySet();
        final Map<BlockFace, long[]> targetSignatures = new ConcurrentHashMap<>();
        final Map<BlockFace, Integer> cursors = new ConcurrentHashMap<>();
        volatile long nextWorkMs;
        volatile boolean multiDirection;
        volatile int distance = 16;
        volatile TransportMode transportMode = TransportMode.NONE;
        volatile boolean vanilla;
        volatile int rangeModules;
        volatile int capacityModules;
        volatile boolean modeModule;
        volatile boolean bindingModule;
        volatile boolean wired;
        volatile Map<BlockFace, Map<Integer, List<ItemStack>>> bindings;
        volatile Map<BlockFace, Map<Integer, Integer>> negCache;
        volatile Map<BlockFace, TransportMode> dirModes;
        volatile List<ItemStack> templateRefs;
        volatile List<ItemRequest> templateRequests;
        final Map<List<ItemStack>, List<ItemRequest>> boundRequestCache =
            Collections.synchronizedMap(new IdentityHashMap<>());

        public @NotNull Set<BlockFace> effectiveDirections() {
            if (multiDirection) {
                return Set.copyOf(directions);
            }
            for (BlockFace face : NetworkDirectional.VALID_FACES) {
                if (directions.contains(face)) {
                    return Set.of(face);
                }
            }
            return Set.of();
        }
    }
}
