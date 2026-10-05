package com.ytdd9527.networksexpansion.implementation.machines.cellnet.assembly;

import com.balugaq.netex.utils.Debug;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.assembly.core.SmartCore;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.StorageCell;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.VoidCellSupport;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive.CellDrive;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive.DriveStorage;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive.EnderDrive;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ItemKey;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

final class AssemblyRound {

    static final long MIN_INTERVAL_MS = 1000L;
    static final long BACKOFF_INTERVAL_MS = 3000L;
    static final long PROBE_RETRY_MS = 2000L;
    static final int GEAR_MIN = 0;
    static final int GEAR_MAX = 13;
    static final long[] GEAR_LADDER = {1L, 4L, 8L, 16L, 32L, 64L, 128L, 256L, 512L, 1024L, 1728L, 3456L};

    static final Map<Location, DriveRuntimeState> RUNTIME_STATES = new ConcurrentHashMap<>();
    static final Set<Location> ACTIVE_DRIVES = ConcurrentHashMap.newKeySet();
    private static final long SNAPSHOT_TTL_MS = 5000L;
    private static final Map<Location, SharedSnapshot> SHARED_SNAPSHOT_BY_NETWORK = new ConcurrentHashMap<>();
    private static final long SHARED_SNAPSHOT_IDLE_MS = 3_600_000L;

    private AssemblyRound() {
    }

    private record SharedSnapshot(
        @NotNull Map<ItemKey, Long> keyed,
        long expiresAtMs,
        long lastAccessAtMs) {
    }

    static final class DriveRuntimeState {
        MachineState machineState;
        SlotCache recipeCache;
        Map<ItemKey, Long> pendingOutputs;
    }

    static DriveRuntimeState runtimeState(@NotNull Location loc) {
        return RUNTIME_STATES.computeIfAbsent(loc, k -> new DriveRuntimeState());
    }

    static final class MachineState {
        final boolean[] blocked = new boolean[AssemblyDrive.RECIPE_SLOTS.length];
        final long[] craftedSince = new long[AssemblyDrive.RECIPE_SLOTS.length];
        final long[] nextProbeAt = new long[AssemblyDrive.RECIPE_SLOTS.length];
        final long[] cachedMode = new long[AssemblyDrive.RECIPE_SLOTS.length];
        final long[] cachedTarget = new long[AssemblyDrive.RECIPE_SLOTS.length];
        final long[] cachedKeep = new long[AssemblyDrive.RECIPE_SLOTS.length];
        final long[] cachedRemaining = new long[AssemblyDrive.RECIPE_SLOTS.length];
        final int[] cachedGear = new int[AssemblyDrive.RECIPE_SLOTS.length];
        final ItemStack[] slotItemRefs = new ItemStack[AssemblyDrive.RECIPE_SLOTS.length];
        final Recipe[] slotRecipes = new Recipe[AssemblyDrive.RECIPE_SLOTS.length];
        final boolean[] lineNeedsStock = new boolean[AssemblyDrive.RECIPE_SLOTS.length];
        boolean enabled = true;
        boolean needsStockSnapshot;
        int maxGear = GEAR_MAX - 1;
        long nextRun;
        @Nullable Boolean lastStatusEnabled;
        @Nullable Boolean lastStatusHadRoot;
        @Nullable Boolean lastToggleEnabled;
        final RowDisplay[] rowDisplays = new RowDisplay[AssemblyDrive.RECIPE_SLOTS.length];
        @Nullable CachedSnapshot snapshot;
    }

    static final class RowDisplay {
        @Nullable Recipe recipe;
        @Nullable Object snapshotRef;
        long mode;
        long target;
        long remaining;
        long keep;
        long crafted;
        int gear;
        boolean blocked;
        boolean empty;
        @Nullable ItemStack icon;
    }

    record CachedSnapshot(long expiresAtMs, @NotNull Map<ItemKey, Long> keyed) {
    }

    @NotNull
    static CachedSnapshot sharedSnapshot(@NotNull NetworkRoot root) {
        long now = System.currentTimeMillis();
        Location networkId = root.getNodePosition();
        SharedSnapshot shared = SHARED_SNAPSHOT_BY_NETWORK.compute(networkId, (k, old) ->
            old != null && now < old.expiresAtMs()
                ? new SharedSnapshot(old.keyed(), old.expiresAtMs(), now)
                : new SharedSnapshot(root.getAllNetworkItemsKeyedView(), now + SNAPSHOT_TTL_MS, now));
        evictStaleNetworks(now);
        return new CachedSnapshot(shared.expiresAtMs(), shared.keyed());
    }

    private static void evictStaleNetworks(long now) {
        if (SHARED_SNAPSHOT_BY_NETWORK.size() <= 1) {
            return;
        }
        SHARED_SNAPSHOT_BY_NETWORK.values().removeIf(e -> now - e.lastAccessAtMs() > SHARED_SNAPSHOT_IDLE_MS);
    }

    private static final class SlotCache {
        final String[] tokens = new String[AssemblyDrive.RECIPE_SLOTS.length];
        final Recipe[] recipes = new Recipe[AssemblyDrive.RECIPE_SLOTS.length];
    }

    private record Ingredient(@NotNull ItemStack template, int amountPerCraft, @NotNull ItemKey itemKey, @NotNull ItemKey exactKey) {
    }

    record Recipe(@NotNull ItemStack output, @NotNull List<Ingredient> ingredients, @NotNull ItemKey outputKey, @NotNull ItemKey outputExactKey) {
    }

    private record CraftLimit(long mode, long remaining, long want) {
    }

    @Nullable
    private static CraftLimit resolveCraftLimit(
            @NotNull BlockMenu blockMenu,
            int index,
            @NotNull MachineState state,
            @Nullable CachedSnapshot snapshot,
            @NotNull Recipe recipe) {
        int gear = state.cachedGear[index];
        if (gear <= GEAR_MIN) {
            return null;
        }
        long mode = state.cachedMode[index];
        long target = mode == AssemblyDrive.MODE_PLAN ? state.cachedTarget[index] : 0L;
        if (target > AssemblyDrive.MAX_TARGET) {
            return null;
        }
        long remaining = mode == AssemblyDrive.MODE_PLAN ? state.cachedRemaining[index] : 0L;
        long keep = mode == AssemblyDrive.MODE_REPLENISH ? state.cachedKeep[index] : 0L;
        long units = Math.max(1L, recipe.output().getAmount());

        long materialCap = snapshot == null ? Long.MAX_VALUE : craftableFromSnapshot(recipe, snapshot);
        long cap;
        if (mode == AssemblyDrive.MODE_PLAN) {
            cap = (remaining + units - 1) / units;
        } else if (mode == AssemblyDrive.MODE_REPLENISH) {
            if (keep <= 0L || snapshot == null) {
                return null;
            }
            long stock = snapshotStock(snapshot, recipe.outputExactKey()) + Math.max(0L, state.craftedSince[index]);
            String channel = smartChannel(blockMenu);
            if (channel != null) {
                stock += EnderDrive.channelStock(channel, recipe.output());
            }
            if (stock >= keep) {
                state.blocked[index] = false;
                state.nextProbeAt[index] = System.currentTimeMillis() + PROBE_RETRY_MS;
                return null;
            }
            cap = (keep - stock) / units;
        } else {
            cap = Long.MAX_VALUE;
        }

        long gearLim = gearCap(gear);
        long want = Math.min(materialCap, cap);
        if (gearLim >= 0) {
            want = Math.min(want, gearLim);
        }
        if (want <= 0) {
            return null;
        }
        return new CraftLimit(mode, remaining, want);
    }

    static boolean craftSlot(
            @NotNull BlockMenu blockMenu,
            @NotNull NetworkRoot root,
            int index,
            @Nullable CachedSnapshot snapshot,
            @NotNull MachineState state) {
        long now = System.currentTimeMillis();
        if (now < state.nextProbeAt[index]) {
            return false;
        }
        int slot = AssemblyDrive.RECIPE_SLOTS[index];
        ItemStack blueprint = blockMenu.getItemInSlot(slot);
        Recipe recipe = readRecipe(blockMenu, index, blueprint, state);
        if (recipe == null || blueprint == null) {
            return false;
        }
        CraftLimit limit = resolveCraftLimit(blockMenu, index, state, snapshot, recipe);
        if (limit == null) {
            return false;
        }
        Location location = blockMenu.getLocation();
        long units = Math.max(1L, recipe.output().getAmount());
        long batch = probeBatchCapacity(root, location, recipe, units, limit.want(), state, index, now);
        if (batch <= 0) {
            return false;
        }
        long feasible = fetchMaterials(root, location, recipe, batch);
        if (feasible <= 0) {
            state.nextProbeAt[index] = now + PROBE_RETRY_MS;
            return false;
        }
        boolean fullyAbsorbed = pushOutput(root, location, recipe.output(), feasible * units);
        state.craftedSince[index] += feasible;
        if (!fullyAbsorbed) {
            state.snapshot = null;
            SHARED_SNAPSHOT_BY_NETWORK.computeIfPresent(root.getNodePosition(), (k, old) ->
                new SharedSnapshot(old.keyed(), 0L, old.lastAccessAtMs()));
        }

        if (limit.mode() == AssemblyDrive.MODE_PLAN) {
            settlePlanProgress(blockMenu, slot, blueprint, limit.remaining(), feasible, units, state, index);
        }
        return true;
    }

    private static void settlePlanProgress(
            @NotNull BlockMenu blockMenu,
            int slot,
            @NotNull ItemStack blueprint,
            long remaining,
            long feasible,
            long units,
            @NotNull MachineState state,
            int index) {
        long left = Math.max(0L, remaining - feasible * units);
        BlueprintMeta.setMetaLong(blueprint, Keys.CRAFT_REMAINING, left);
        if (left <= 0L) {
            BlueprintMeta.setMetaLong(blueprint, Keys.CRAFT_TARGET, 0L);
        }
        state.cachedRemaining[index] = left;
        if (left <= 0L) {
            state.cachedTarget[index] = 0L;
        }
        blockMenu.replaceExistingItem(slot, blueprint);
        state.slotItemRefs[index] = blockMenu.getItemInSlot(slot);
    }

    @Nullable
    private static String smartChannel(@NotNull BlockMenu blockMenu) {
        ItemStack core = blockMenu.getItemInSlot(AssemblyDrive.SMART_SLOT);
        if (core == null || !SmartCore.isSmartCore(core)) {
            return null;
        }
        return SmartCore.getChannel(core);
    }

    static void updateStockFlag(@NotNull MachineState state, int index, @Nullable ItemStack blueprint) {
        state.nextProbeAt[index] = 0L;
        state.slotItemRefs[index] = null;
        state.slotRecipes[index] = null;
        if (blueprint == null || blueprint.getType().isAir() || !AssemblyCard.isCard(blueprint)) {
            state.lineNeedsStock[index] = false;
            state.cachedMode[index] = AssemblyDrive.MODE_CONTINUOUS;
            state.cachedTarget[index] = 0L;
            state.cachedKeep[index] = 0L;
            state.cachedRemaining[index] = 0L;
            state.cachedGear[index] = GEAR_MIN;
        } else {
            state.cachedMode[index] = BlueprintMeta.getMetaLong(blueprint, Keys.CRAFT_MODE, AssemblyDrive.MODE_CONTINUOUS);
            state.cachedTarget[index] = BlueprintMeta.getMetaLong(blueprint, Keys.CRAFT_TARGET, 0L);
            state.cachedKeep[index] = BlueprintMeta.getMetaLong(blueprint, Keys.CRAFT_KEEP, 0L);
            state.cachedGear[index] = Math.min(gearOf(blueprint), state.maxGear);
            long target = state.cachedTarget[index];
            state.cachedRemaining[index] = target > 0
                ? BlueprintMeta.getMetaLong(blueprint, Keys.CRAFT_REMAINING, target)
                : 0L;
            state.lineNeedsStock[index] =
                state.cachedMode[index] == AssemblyDrive.MODE_REPLENISH && state.cachedGear[index] > GEAR_MIN;
        }
        state.needsStockSnapshot = anyNeedsStock(state);
    }

    private static boolean anyNeedsStock(@NotNull MachineState state) {
        for (boolean need : state.lineNeedsStock) {
            if (need) {
                return true;
            }
        }
        return false;
    }

    private static long gearCap(int gear) {
        if (gear <= GEAR_MIN) {
            return 0L;
        }
        if (gear > GEAR_LADDER.length) {
            return -1L;
        }
        return GEAR_LADDER[gear - 1];
    }

    static int gearOf(@NotNull ItemStack blueprint) {
        long raw = BlueprintMeta.getMetaLong(blueprint, Keys.CRAFT_GEAR, 1L);
        return (int) Math.max(GEAR_MIN, Math.min(GEAR_MAX, raw));
    }

    static long craftableFromSnapshot(@NotNull Recipe recipe, @NotNull CachedSnapshot snapshot) {
        long craftable = Long.MAX_VALUE;
        for (Ingredient ingredient : recipe.ingredients()) {
            Long have = snapshot.keyed().get(ingredient.exactKey());
            long count = have == null ? 0L : Math.max(0L, have);
            craftable = Math.min(craftable, count / ingredient.amountPerCraft());
            if (craftable == 0L) {
                return 0L;
            }
        }
        return craftable;
    }

    static long snapshotStock(@NotNull CachedSnapshot snapshot, @NotNull ItemKey key) {
        Long have = snapshot.keyed().get(key);
        return have == null ? 0L : Math.max(0L, have);
    }

    private static long probeCapacity(
            @NotNull NetworkRoot root,
            @NotNull Location accessor,
            @NotNull Recipe recipe,
            long amount) {
        if (amount <= 0) {
            return 0L;
        }
        DriveStorage.CapacityProbe capacityProbe = CellDrive.getStorage().probeReceive(
            root.getDriveCache(), root.getInputAbleCellDriveMenus(), recipe.outputKey(), amount);
        if (capacityProbe.capacity() >= amount || capacityProbe.voidAccepts()) {
            return amount;
        }
        ItemStack template = recipe.output();
        ItemStack probe = template.clone();
        probe.setAmount((int) Math.min(amount, Integer.MAX_VALUE));
        return withVoidSuspended(() -> {
            root.addItemStack0(accessor, probe);
            long absorbed = amount - Math.max(0L, probe.getAmount());
            if (absorbed <= 0) {
                return 0L;
            }
            ItemStack got = root.getItemStack0(accessor,
                new ItemRequest(template.clone(), (int) Math.min(absorbed, Integer.MAX_VALUE)));
            long gotAmount = got == null || got.getType().isAir() ? 0L : got.getAmount();
            if (gotAmount < absorbed) {
                Debug.debug("装配驱动器容量探针取回不足");
            }
            return gotAmount;
        });
    }

    private static long fetchMaterials(
            @NotNull NetworkRoot root,
            @NotNull Location accessor,
            @NotNull Recipe recipe,
            long batch) {
        return withVoidSuspended(() -> {
            List<Ingredient> ingredients = recipe.ingredients();
            ItemStack[] taken = new ItemStack[ingredients.size()];
            long[] per = new long[ingredients.size()];
            long feasible = batch;
            for (int i = 0; i < ingredients.size(); i++) {
                Ingredient ingredient = ingredients.get(i);
                per[i] = Math.max(1L, ingredient.amountPerCraft());
                long need = Math.min(batch * per[i], Integer.MAX_VALUE);
                ItemStack stack = takeMaterial(root, accessor, ingredient, (int) need);
                taken[i] = stack;
                long gotAmount = stack == null || stack.getType().isAir() ? 0L : stack.getAmount();
                feasible = Math.min(feasible, gotAmount / per[i]);
            }
            if (feasible <= 0) {
                returnTaken(root, accessor, taken);
                return 0L;
            }
            List<ItemStack> excess = new ArrayList<>();
            for (int i = 0; i < ingredients.size(); i++) {
                ItemStack stack = taken[i];
                if (stack == null || stack.getType().isAir()) {
                    continue;
                }
                long keep = feasible * per[i];
                if (stack.getAmount() > keep) {
                    ItemStack extra = stack.clone();
                    extra.setAmount((int) (stack.getAmount() - keep));
                    excess.add(extra);
                }
            }
            returnAll(root, accessor, excess);
            return feasible;
        });
    }

    private record RoundLine(int index, @NotNull ItemStack blueprint, @NotNull Recipe recipe, @NotNull CraftLimit limit, long units, long batch) {
    }

    private record RoundFetch(long @NotNull [] feasible, @NotNull Map<ItemKey, Long> outputs, @NotNull Map<ItemKey, ItemStack> outputTemplates) {
    }

    static boolean craftRound(
            @NotNull BlockMenu blockMenu,
            @NotNull NetworkRoot root,
            @Nullable CachedSnapshot snapshot,
            @NotNull MachineState state) {
        long now = System.currentTimeMillis();
        Location location = blockMenu.getLocation();
        List<RoundLine> lines = collectRoundLines(blockMenu, root, location, snapshot, state, now);
        if (lines.isEmpty()) {
            return false;
        }
        RoundFetch fetch = withVoidSuspended(() -> fetchRoundMaterials(root, location, lines));
        boolean active = settleRoundProgress(blockMenu, lines, fetch, state, now);
        if (!pushRoundOutputs(root, location, fetch)) {
            state.snapshot = null;
            SHARED_SNAPSHOT_BY_NETWORK.computeIfPresent(root.getNodePosition(), (k, old) ->
                new SharedSnapshot(old.keyed(), 0L, old.lastAccessAtMs()));
        }
        return active;
    }

    @NotNull
    private static List<RoundLine> collectRoundLines(
            @NotNull BlockMenu blockMenu,
            @NotNull NetworkRoot root,
            @NotNull Location location,
            @Nullable CachedSnapshot snapshot,
            @NotNull MachineState state,
            long now) {
        List<RoundLine> lines = new ArrayList<>();
        for (int i = 0; i < AssemblyDrive.RECIPE_SLOTS.length; i++) {
            if (now < state.nextProbeAt[i]) {
                continue;
            }
            ItemStack blueprint = blockMenu.getItemInSlot(AssemblyDrive.RECIPE_SLOTS[i]);
            Recipe recipe = readRecipe(blockMenu, i, blueprint, state);
            if (recipe == null || blueprint == null) {
                continue;
            }
            CraftLimit limit = resolveCraftLimit(blockMenu, i, state, snapshot, recipe);
            if (limit == null) {
                continue;
            }
            long units = Math.max(1L, recipe.output().getAmount());
            long batch = probeBatchCapacity(root, location, recipe, units, limit.want(), state, i, now);
            if (batch <= 0) {
                continue;
            }
            lines.add(new RoundLine(i, blueprint, recipe, limit, units, batch));
        }
        return lines;
    }

    private static long probeBatchCapacity(
            @NotNull NetworkRoot root,
            @NotNull Location location,
            @NotNull Recipe recipe,
            long units,
            long want,
            @NotNull MachineState state,
            int index,
            long now) {
        long maxWant = Integer.MAX_VALUE / Math.max(1L, units);
        long probeUnits = Math.min(want, maxWant) * units;
        long absorbed = probeCapacity(root, location, recipe, probeUnits);
        if (absorbed <= 0) {
            state.blocked[index] = true;
            state.nextProbeAt[index] = now + PROBE_RETRY_MS;
            return 0L;
        }
        state.blocked[index] = false;
        state.nextProbeAt[index] = 0L;
        long batch = Math.min(want, absorbed / units);
        if (batch <= 0) {
            state.blocked[index] = true;
            state.nextProbeAt[index] = now + PROBE_RETRY_MS;
            return 0L;
        }
        return batch;
    }

    private static boolean settleRoundProgress(
            @NotNull BlockMenu blockMenu,
            @NotNull List<RoundLine> lines,
            @NotNull RoundFetch fetch,
            @NotNull MachineState state,
            long now) {
        boolean active = false;
        for (RoundLine line : lines) {
            long feasible = fetch.feasible()[line.index()];
            if (feasible <= 0) {
                state.nextProbeAt[line.index()] = now + PROBE_RETRY_MS;
                continue;
            }
            active = true;
            state.craftedSince[line.index()] += feasible;
            if (line.limit().mode() == AssemblyDrive.MODE_PLAN) {
                settlePlanProgress(blockMenu, AssemblyDrive.RECIPE_SLOTS[line.index()], line.blueprint(),
                    line.limit().remaining(), feasible, line.units(), state, line.index());
            }
        }
        return active;
    }

    private static boolean pushRoundOutputs(
            @NotNull NetworkRoot root,
            @NotNull Location location,
            @NotNull RoundFetch fetch) {
        boolean allAbsorbed = true;
        for (Map.Entry<ItemKey, Long> entry : fetch.outputs().entrySet()) {
            long remaining = entry.getValue();
            ItemStack template = fetch.outputTemplates().get(entry.getKey());
            long cellRemaining = CellDrive.getStorage().pushSingle(
                root.getDriveCache(), root.getInputAbleCellDriveMenus(), template, remaining);
            remaining = Math.max(0L, cellRemaining);
            while (remaining > 0) {
                long chunk = Math.min(remaining, Integer.MAX_VALUE);
                ItemStack out = template.clone();
                out.setAmount((int) chunk);
                root.addItemStack0(location, out);
                long leftover = Math.max(0L, out.getAmount());
                if (leftover > 0) {
                    bufferPending(location, template, leftover);
                    allAbsorbed = false;
                }
                remaining -= chunk;
            }
        }
        return allAbsorbed;
    }

    private static long ingredientWant(@NotNull RoundLine line, @NotNull Ingredient ingredient) {
        return Math.min(line.batch() * ingredient.amountPerCraft(), Integer.MAX_VALUE);
    }

    @NotNull
    private static RoundFetch fetchRoundMaterials(
            @NotNull NetworkRoot root,
            @NotNull Location accessor,
            @NotNull List<RoundLine> lines) {
        Map<ItemKey, Long> needTotals = new LinkedHashMap<>();
        Map<ItemKey, ItemStack> templates = new HashMap<>();
        for (RoundLine line : lines) {
            for (Ingredient ingredient : line.recipe().ingredients()) {
                needTotals.merge(ingredient.itemKey(), ingredientWant(line, ingredient), Long::sum);
                templates.putIfAbsent(ingredient.itemKey(), ingredient.template());
            }
        }

        Map<ItemKey, Long> pool = new HashMap<>();
        for (Map.Entry<ItemKey, Long> entry : needTotals.entrySet()) {
            long got = CellDrive.getStorage().takeItemDirectAmount(
                root.getDriveCache(), root.getInputAbleCellDriveMenus(), entry.getKey(), entry.getValue());
            if (got > 0) {
                pool.put(entry.getKey(), got);
            }
        }

        Map<ItemKey, Long> shortfalls = new LinkedHashMap<>();
        for (Map.Entry<ItemKey, Long> entry : needTotals.entrySet()) {
            long missing = entry.getValue() - pool.getOrDefault(entry.getKey(), 0L);
            if (missing > 0) {
                shortfalls.put(entry.getKey(), missing);
            }
        }
        if (!shortfalls.isEmpty()) {
            List<ItemRequest> requests = new ArrayList<>(shortfalls.size());
            List<ItemKey> order = new ArrayList<>(shortfalls.size());
            for (Map.Entry<ItemKey, Long> entry : shortfalls.entrySet()) {
                long missing = entry.getValue();
                while (missing > 0) {
                    requests.add(new ItemRequest(templates.get(entry.getKey()).clone(),
                        (int) Math.min(missing, Integer.MAX_VALUE)));
                    order.add(entry.getKey());
                    missing -= Integer.MAX_VALUE;
                }
            }
            List<ItemStack> fetched = root.getItemStacksBatch0(accessor, requests);
            for (int i = 0; i < fetched.size(); i++) {
                ItemStack got = fetched.get(i);
                long amount = got == null || got.getType().isAir() ? 0L : got.getAmount();
                if (amount > 0) {
                    pool.merge(order.get(i), amount, Long::sum);
                }
            }
        }

        long[] feasible = new long[AssemblyDrive.RECIPE_SLOTS.length];
        for (RoundLine line : lines) {
            long cap = line.batch();
            for (Ingredient ingredient : line.recipe().ingredients()) {
                long want = ingredientWant(line, ingredient);
                long avail = pool.getOrDefault(ingredient.itemKey(), 0L);
                long take = Math.min(want, avail);
                pool.put(ingredient.itemKey(), avail - take);
                cap = Math.min(cap, take / ingredient.amountPerCraft());
            }
            feasible[line.index()] = Math.max(0L, cap);
        }

        Map<ItemKey, Long> outputs = new LinkedHashMap<>();
        Map<ItemKey, ItemStack> outputTemplates = new HashMap<>();
        for (RoundLine line : lines) {
            long feasibleAmount = feasible[line.index()];
            if (feasibleAmount <= 0) {
                continue;
            }
            long units = line.units();
            outputs.merge(line.recipe().outputKey(), feasibleAmount * units, Long::sum);
            outputTemplates.putIfAbsent(line.recipe().outputKey(), line.recipe().output());
        }

        List<ItemStack> excess = new ArrayList<>();
        for (Map.Entry<ItemKey, Long> entry : pool.entrySet()) {
            long leftover = entry.getValue();
            while (leftover > 0) {
                ItemStack stack = templates.get(entry.getKey()).clone();
                stack.setAmount((int) Math.min(leftover, Integer.MAX_VALUE));
                excess.add(stack);
                leftover -= Integer.MAX_VALUE;
            }
        }
        returnAll(root, accessor, excess);
        return new RoundFetch(feasible, outputs, outputTemplates);
    }

    @Nullable
    private static ItemStack takeMaterial(
            @NotNull NetworkRoot root,
            @NotNull Location accessor,
            @NotNull Ingredient ingredient,
            int need) {
        ItemStack direct = CellDrive.getStorage().takeItemDirect(
            root.getDriveCache(), root.getInputAbleCellDriveMenus(), ingredient.itemKey(), need);
        int directAmount = direct == null || direct.getType().isAir() ? 0 : direct.getAmount();
        if (directAmount >= need) {
            return direct;
        }
        int shortfall = need - directAmount;
        ItemStack fallback = root.getItemStack0(accessor,
            new ItemRequest(ingredient.template().clone(), shortfall));
        if (fallback == null || fallback.getType().isAir()) {
            return direct;
        }
        if (direct == null || direct.getType().isAir()) {
            return fallback;
        }
        direct.setAmount(directAmount + fallback.getAmount());
        return direct;
    }

    @Nullable
    private static <T> T withVoidSuspended(@NotNull Supplier<T> action) {
        VoidCellSupport.suspend();
        try {
            return action.get();
        } finally {
            VoidCellSupport.resume();
        }
    }

    private static void returnTaken(
            @NotNull NetworkRoot root,
            @NotNull Location accessor,
            @Nullable ItemStack @NotNull [] stacks) {
        List<ItemStack> items = new ArrayList<>(stacks.length);
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.getType().isAir() && stack.getAmount() > 0) {
                items.add(stack);
            }
        }
        returnAll(root, accessor, items);
    }

    private static boolean pushOutput(
            @NotNull NetworkRoot root,
            @NotNull Location accessor,
            @NotNull ItemStack template,
            long total) {
        ItemStack out = template.clone();
        out.setAmount((int) Math.min(total, Integer.MAX_VALUE));
        root.addItemStack0(accessor, out);
        if (!out.getType().isAir() && out.getAmount() > 0) {
            bufferPending(accessor, template, out.getAmount());
            return false;
        }
        return true;
    }

    private static void returnAll(
            @NotNull NetworkRoot root,
            @NotNull Location accessor,
            @NotNull List<ItemStack> items) {
        for (ItemStack item : items) {
            if (item.getType().isAir() || item.getAmount() <= 0) {
                continue;
            }
            root.addItemStack0(accessor, item);
            if (!item.getType().isAir() && item.getAmount() > 0) {
                bufferPending(accessor, item, item.getAmount());
            }
        }
    }

    private static void bufferPending(@NotNull Location accessor, @NotNull ItemStack template, long amount) {
        if (amount <= 0 || template.getType().isAir()) {
            return;
        }
        DriveRuntimeState s = runtimeState(accessor);
        if (s.pendingOutputs == null) {
            s.pendingOutputs = new ConcurrentHashMap<>();
        }
        s.pendingOutputs.merge(ItemKey.exact(template), amount, Long::sum);
    }

    static boolean flushPendingOutputs(@NotNull NetworkRoot root, @NotNull Location accessor) {
        Map<ItemKey, Long> pending = runtimeState(accessor).pendingOutputs;
        if (pending == null || pending.isEmpty()) {
            return true;
        }
        Iterator<Map.Entry<ItemKey, Long>> iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ItemKey, Long> entry = iterator.next();
            long remaining = entry.getValue();
            if (remaining <= 0L) {
                iterator.remove();
                continue;
            }
            long left = returnToNetwork(root, accessor, entry.getKey(), remaining);
            if (left > 0L) {
                entry.setValue(left);
                return false;
            }
            iterator.remove();
        }
        return true;
    }

    private static long returnToNetwork(
            @NotNull NetworkRoot root,
            @NotNull Location accessor,
            @NotNull ItemKey key,
            long amount) {
        if (amount <= 0L) {
            return 0L;
        }
        return withVoidSuspended(() -> {
            long remaining = amount;
            while (remaining > 0L) {
                long push = Math.min(remaining, Integer.MAX_VALUE);
                ItemStack stack = key.getItemStack();
                stack.setAmount((int) push);
                root.addItemStack0(accessor, stack);
                long absorbed = push - Math.max(0L, stack.getAmount());
                if (absorbed <= 0L) {
                    break;
                }
                remaining -= absorbed;
            }
            return remaining;
        });
    }

    static void dropPending(@NotNull Location location, @Nullable NetworkRoot root, @Nullable Map<ItemKey, Long> pending) {
        if (pending == null || pending.isEmpty()) {
            return;
        }
        for (Map.Entry<ItemKey, Long> entry : pending.entrySet()) {
            long remaining = entry.getValue();
            if (root != null) {
                remaining = returnToNetwork(root, location, entry.getKey(), remaining);
            }
            if (remaining > 0L && location.getWorld() != null) {
                while (remaining > 0L) {
                    ItemStack stack = entry.getKey().getItemStack();
                    stack.setAmount((int) Math.min(remaining, Integer.MAX_VALUE));
                    location.getWorld().dropItemNaturally(location.clone().add(0.5, 1.0, 0.5), stack);
                    remaining -= stack.getAmount();
                }
            }
        }
    }

    @Nullable
    static Recipe readRecipe(@NotNull BlockMenu blockMenu, int index, @Nullable ItemStack item, @Nullable MachineState state) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        if (state != null) {
            if (state.slotItemRefs[index] == item) {
                return state.slotRecipes[index];
            }
            ItemStack cachedRef = state.slotItemRefs[index];
            if (cachedRef != null && item.isSimilar(cachedRef)) {
                state.slotItemRefs[index] = item;
                return state.slotRecipes[index];
            }
        }
        Recipe recipe = readRecipeSlow(blockMenu, index, item);
        if (state != null) {
            state.slotItemRefs[index] = item;
            state.slotRecipes[index] = recipe;
        }
        return recipe;
    }

    @Nullable
    private static Recipe readRecipeSlow(@NotNull BlockMenu blockMenu, int index, @NotNull ItemStack item) {
        if (isSlotMarker(item)) {
            return null;
        }
        String token = cardToken(item);
        if (token == null) {
            return null;
        }
        DriveRuntimeState s = runtimeState(blockMenu.getLocation().clone());
        if (s.recipeCache == null) {
            s.recipeCache = new SlotCache();
        }
        SlotCache cache = s.recipeCache;
        if (token.equals(cache.tokens[index])) {
            return cache.recipes[index];
        }
        for (int other = 0; other < AssemblyDrive.RECIPE_SLOTS.length; other++) {
            if (other != index && token.equals(cache.tokens[other])) {
                cache.tokens[index] = token;
                cache.recipes[index] = cache.recipes[other];
                return cache.recipes[other];
            }
        }
        Recipe recipe = parseRecipe(item);
        cache.tokens[index] = token;
        cache.recipes[index] = recipe;
        return recipe;
    }

    @Nullable
    private static String cardToken(@NotNull ItemStack item) {
        if (item.getType() != Material.PAPER || !item.hasItemMeta()) {
            return null;
        }
        var meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String token = PersistentDataAPI.getString(meta, Keys.ASSEMBLY_CARD_HASH);
        return token == null || token.isEmpty() ? null : token;
    }

    @Nullable
    private static Recipe parseRecipe(@NotNull ItemStack item) {
        AssemblyCard.RecipeEntry entry = AssemblyCard.readCard(item);
        return entry == null ? null : toRecipe(entry.ingredients(), entry.output());
    }

    @Nullable
    private static Recipe toRecipe(@NotNull List<ItemStack> rawIngredients, @NotNull ItemStack rawOutput) {
        if (rawIngredients.isEmpty() || rawOutput.getType().isAir()) {
            return null;
        }
        List<Ingredient> ingredients = new ArrayList<>(rawIngredients.size());
        for (ItemStack requested : rawIngredients) {
            if (StackUtils.isBlacklisted(requested)
                || StorageCell.isStorageCell(requested)
                || VoidCellSupport.isVoidCell(requested)) {
                return null;
            }
            ingredients.add(new Ingredient(requested.clone(), Math.max(1, requested.getAmount()),
                new ItemKey(requested), ItemKey.exact(requested)));
        }
        ItemStack output = rawOutput.clone();
        output.setAmount(Math.max(1, rawOutput.getAmount()));
        return new Recipe(output, List.copyOf(ingredients), new ItemKey(output), ItemKey.exact(output));
    }

    static boolean isAcceptableRecipeItem(@Nullable ItemStack item) {
        return AssemblyCard.isCard(item);
    }

    static boolean isSlotMarker(@Nullable ItemStack item) {
        if (item == null || item.getType() != Material.LIME_STAINED_GLASS_PANE || !item.hasItemMeta()) {
            return false;
        }
        var meta = item.getItemMeta();
        return meta != null
            && PersistentDataAPI.getByte(meta, Keys.ASSEMBLY_SLOT_MARKER) == 1;
    }
}
