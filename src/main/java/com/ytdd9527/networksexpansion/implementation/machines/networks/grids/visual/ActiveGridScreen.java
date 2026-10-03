package com.ytdd9527.networksexpansion.implementation.machines.networks.grids.visual;

import com.balugaq.bim.grid.ActiveGrid;
import com.balugaq.netex.api.visual.Screen;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.implementation.ExpansionItems;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@RequiredArgsConstructor
@Data
@NullMarked
public class ActiveGridScreen implements Screen {
    final ActiveGrid active;
    @Override
    public Location getLocation() {
        return active.getLocation();
    }

    @Override
    public boolean hasViewer() {
        return active.isDisplaying();
    }

    @Override
    public boolean isAlive() {
        return StorageCacheUtils.getSfItem(getLocation()) instanceof VisualGrid;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        active.setItem(slot, stack);
    }

    @Override
    public void setItem(int slot, ItemStack stack, ChestMenu.MenuClickHandler clickHandler) {
        setItem(slot, stack);
        active.setClickHandler(slot, (dto) -> clickHandler.onClick(dto.player(), dto.slot(), dto.clicked(), new ClickAction(dto.isRightClick(), dto.isShiftClick())));
    }

    @Override
    public SlimefunItem source() {
        return ExpansionItems.VISUAL_GRID;
    }

    @Override
    public void close() {
        // 不需要关闭
    }

    @Override
    public void open(Player... players) {
    }
}
