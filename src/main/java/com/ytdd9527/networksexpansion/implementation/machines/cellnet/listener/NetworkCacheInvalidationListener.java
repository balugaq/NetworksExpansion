package com.ytdd9527.networksexpansion.implementation.machines.cellnet.listener;

import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.NetworkUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

public class NetworkCacheInvalidationListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        NetworkUtil.invalidate(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        NetworkUtil.invalidate(event.getBlock().getLocation());
    }
}
