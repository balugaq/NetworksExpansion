package com.ytdd9527.networksexpansion.api.assembly;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record DriveOverview(@NotNull Location location, @Nullable String name, boolean enabled, boolean overclock,
                            boolean smart,
                            @NotNull List<MonitorLine> lines) {
}
