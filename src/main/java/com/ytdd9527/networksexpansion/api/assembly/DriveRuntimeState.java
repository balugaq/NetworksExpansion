package com.ytdd9527.networksexpansion.api.assembly;

import com.ytdd9527.networksexpansion.utils.ItemKey;

import java.util.Map;

public final class DriveRuntimeState {
    public MachineState machineState;
    SlotCache recipeCache;
    public Map<ItemKey, Long> pendingOutputs;
}
