package com.ytdd9527.networksexpansion.implementation.machines.networks.grids.visual;

import com.balugaq.netex.api.visual.Screen;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@RequiredArgsConstructor
@Data
@NullMarked
public class BlockMenuScreen implements Screen {
    final BlockMenu blockMenu;

    @Override
    public Location getLocation() {
        return blockMenu.getLocation();
    }

    @Override
    public boolean hasViewer() {
        return blockMenu.hasViewer();
    }

    @Override
    public boolean isAlive() {
        return StorageCacheUtils.getMenu(getLocation()) == blockMenu;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        blockMenu.replaceExistingItem(slot, stack);
    }

    @Override
    public void setItem(int slot, ItemStack stack, ChestMenu.MenuClickHandler clickHandler) {
        setItem(slot, stack);
        blockMenu.addMenuClickHandler(slot, clickHandler);
    }

    @Override
    public SlimefunItem source() {
        return blockMenu.getPreset().getSlimefunItem();
    }

    @Override
    public void close() {
        blockMenu.close();
    }

    @Override
    public void open(Player... players) {
        blockMenu.open(players);
    }
}
