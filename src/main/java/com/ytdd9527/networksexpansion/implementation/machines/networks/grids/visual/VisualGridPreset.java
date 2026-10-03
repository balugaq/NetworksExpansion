package com.ytdd9527.networksexpansion.implementation.machines.networks.grids.visual;

import com.balugaq.bim.grid.ActiveGrid;
import com.balugaq.bim.grid.ActiveGridPreset;
import com.balugaq.bim.grid.InteractUnit;
import com.balugaq.jeg.utils.KeyUtil;
import com.balugaq.netex.api.helpers.Icon;
import com.balugaq.netex.api.visual.Screen;
import com.ytdd9527.networksexpansion.implementation.ExpansionItems;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.slimefun.network.grid.GridCache;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class VisualGridPreset extends ActiveGridPreset {
    public VisualGridPreset() {
        super(KeyUtil.newKey("visual_grid"), 9, 9);
    }

    @Override
    public void init(ActiveGrid active, int idx, InteractUnit unit) {
        super.init(active, idx, unit);
        var v = ExpansionItems.VISUAL_GRID;
        var screen = Screen.of(active);
        for (var s : VisualGrid.BACKGROUND_SLOTS) {
            screen.setItem(s, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }
        v.addKeybindSettingsButton(screen, v.getKeybindButtonSlot());
        screen.setItem(v.getFilterSlot(), Icon.FILTER_STACK, (p, s, i, a) -> {
            GridCache gridCache = v.getCacheMap().get(screen.getLocation());
            v.setFilter(p, screen, gridCache, a);
            return false;
        });
    }

    @Override
    public void tick(ActiveGrid active) {
        GridCache cache = ExpansionItems.VISUAL_GRID.getCacheMap().get(active.getLocation());
        if (cache != null) {
            cache.setEntriesCache(null);
            ExpansionItems.VISUAL_GRID.updateDisplay(Screen.of(active));
        }
    }

    @Override
    public int tickInterval(ActiveGrid active) {
        return 2;
    }

    @Override
    public int entriesSize(ActiveGrid active) {
        var node = NetworkStorage.getNode(active.getLocation());
        if (node == null || node.getNode() == null) return 0;
        var root = node.getNode().getRoot();
        return root.getAllNetworkItemsLongTypeView().size();
    }
}
