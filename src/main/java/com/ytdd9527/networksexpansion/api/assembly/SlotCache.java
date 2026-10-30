package com.ytdd9527.networksexpansion.api.assembly;

import com.ytdd9527.networksexpansion.implementation.machines.assembly.AssemblyDrive;

public final class SlotCache {
    public final String[] tokens = new String[AssemblyDrive.RECIPE_SLOTS.length];
    public final Recipe[] recipes = new Recipe[AssemblyDrive.RECIPE_SLOTS.length];
}
