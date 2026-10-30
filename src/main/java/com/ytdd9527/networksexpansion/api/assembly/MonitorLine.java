package com.ytdd9527.networksexpansion.api.assembly;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public record MonitorLine(@NotNull ItemStack output, long mode, int gear, long remaining, long keep, boolean blocked,
                          boolean planArmed) {
}
