package com.ytdd9527.networksexpansion.api.assembly;

import com.ytdd9527.networksexpansion.implementation.machines.assembly.AssemblyDrive;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class MachineState {
    public final boolean[] blocked = new boolean[AssemblyDrive.RECIPE_SLOTS.length];
    public final long[] craftedSince = new long[AssemblyDrive.RECIPE_SLOTS.length];
    public final long[] nextProbeAt = new long[AssemblyDrive.RECIPE_SLOTS.length];
    public final long[] cachedMode = new long[AssemblyDrive.RECIPE_SLOTS.length];
    public final long[] cachedTarget = new long[AssemblyDrive.RECIPE_SLOTS.length];
    public final long[] cachedKeep = new long[AssemblyDrive.RECIPE_SLOTS.length];
    public final long[] cachedRemaining = new long[AssemblyDrive.RECIPE_SLOTS.length];
    public final int[] cachedGear = new int[AssemblyDrive.RECIPE_SLOTS.length];
    public final ItemStack[] slotItemRefs = new ItemStack[AssemblyDrive.RECIPE_SLOTS.length];
    public final Recipe[] slotRecipes = new Recipe[AssemblyDrive.RECIPE_SLOTS.length];
    public final boolean[] lineNeedsStock = new boolean[AssemblyDrive.RECIPE_SLOTS.length];
    public boolean enabled = true;
    public boolean needsStockSnapshot;
    public int maxGear = AssemblyRound.GEAR_MAX - 1;
    public volatile long nextRun;
    @Nullable
    public Boolean lastStatusEnabled;
    @Nullable
    public Boolean lastStatusHadRoot;
    @Nullable
    public Boolean lastToggleEnabled;
    public final RowDisplay[] rowDisplays = new RowDisplay[AssemblyDrive.RECIPE_SLOTS.length];
    @Nullable
    public CachedSnapshot snapshot;
}
