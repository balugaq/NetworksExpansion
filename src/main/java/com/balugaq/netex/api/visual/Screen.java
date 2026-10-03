package com.balugaq.netex.api.visual;

import com.balugaq.bim.grid.ActiveGrid;
import com.ytdd9527.networksexpansion.implementation.machines.networks.grids.visual.ActiveGridScreen;
import com.ytdd9527.networksexpansion.implementation.machines.networks.grids.visual.BlockMenuScreen;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
public interface Screen {
    Map<Location, Screen> activeScreens = new ConcurrentHashMap<>();
    Location getLocation();
    boolean hasViewer();
    boolean isAlive();
    void setItem(int slot, ItemStack stack);
    void setItem(int slot, ItemStack stack, ChestMenu.MenuClickHandler clickHandler);
    SlimefunItem source();
    void close();
    void open(Player... players);

    static Screen of(BlockMenu blockMenu) {
        var cached = activeScreens.get(blockMenu.getLocation());
        if (cached != null && cached.isAlive()) return cached;
        var screen = new BlockMenuScreen(blockMenu);
        activeScreens.put(blockMenu.getLocation(), screen);
        return screen;
    }

    static Screen of(ActiveGrid active) {
        var cached = activeScreens.get(active.getLocation());
        if (cached != null && cached.isAlive()) return cached;
        var screen = new ActiveGridScreen(active);
        activeScreens.put(active.getLocation(), screen);
        return screen;
    }

    static Screen delete(Location location) {
        return activeScreens.remove(location);
    }

    static @Nullable Screen of(Location location) {
        return activeScreens.get(location);
    }
}
