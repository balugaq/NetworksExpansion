package com.ytdd9527.networksexpansion.implementation.machines.cellnet.listener;

import com.balugaq.netex.utils.Debug;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.converter.CellCleaner;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.drive.CellDrive;
import io.github.thebusybiscuit.slimefun4.api.events.ExplosiveToolBreakBlocksEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
public class CellnetExplosiveToolListener implements Listener {

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onExplosiveBlockBreak(@NotNull ExplosiveToolBreakBlocksEvent event) {
        final List<Block> blocksToRemove = new ArrayList<>();
        for (Block block : event.getAdditionalBlocks()) {
            final Location location = block.getLocation();
            final SlimefunItem item = StorageCacheUtils.getSfItem(location);
            if (item instanceof CellDrive || item instanceof CellCleaner) {
                blocksToRemove.add(block);
                Debug.debug("已阻止爆破工具破坏元件网络方块: " + location);
            }
        }
        event.getAdditionalBlocks().removeAll(blocksToRemove);
    }
}