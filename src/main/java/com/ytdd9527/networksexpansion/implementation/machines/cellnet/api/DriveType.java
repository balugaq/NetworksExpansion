package com.ytdd9527.networksexpansion.implementation.machines.cellnet.api;

import com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive.CellDrive;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive.EnderDrive;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.jetbrains.annotations.Nullable;

public enum DriveType {
    STANDARD,
    ENDER;

    @Nullable
    public static DriveType of(@Nullable SlimefunItem item) {
        if (item instanceof EnderDrive) {
            return ENDER;
        }
        if (item instanceof CellDrive) {
            return STANDARD;
        }
        return null;
    }
}
