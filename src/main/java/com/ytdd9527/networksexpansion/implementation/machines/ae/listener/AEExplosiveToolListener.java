package com.ytdd9527.networksexpansion.implementation.machines.ae.listener;

import com.balugaq.netex.utils.Debug;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.implementation.machines.ae.blockentity.AECellCleaner;
import com.ytdd9527.networksexpansion.implementation.machines.ae.blockentity.AEDrive;
import io.github.thebusybiscuit.slimefun4.api.events.ExplosiveToolBreakBlocksEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AEExplosiveToolListener implements Listener {

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onExplosiveBlockBreak(@NotNull ExplosiveToolBreakBlocksEvent event) {
        final List<Block> blocksToRemove = new ArrayList<>();
        for (Block block : event.getAdditionalBlocks()) {
            final Location location = block.getLocation();
            final SlimefunItem item = StorageCacheUtils.getSfItem(location);
            if (item instanceof AEDrive || item instanceof AECellCleaner) {
                blocksToRemove.add(block);
                Debug.debug("Prevented explosive tool from breaking an AE block at " + location);
            }
        }
        event.getAdditionalBlocks().removeAll(blocksToRemove);
    }
}