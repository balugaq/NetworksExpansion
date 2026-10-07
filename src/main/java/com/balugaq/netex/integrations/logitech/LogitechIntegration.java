package com.balugaq.netex.integrations.logitech;

import com.ytdd9527.networksexpansion.implementation.ExpansionItemStacks;
import com.ytdd9527.networksexpansion.implementation.ExpansionItemsMenus;
import com.ytdd9527.networksexpansion.implementation.machines.manual.ExpansionWorkbench;
import com.ytdd9527.networksexpansion.utils.itemstacks.ItemStackUtil;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.inventory.ItemStack;

public class LogitechIntegration {
    private final ItemStack hyp = SlimefunItem.getById("LOGITECH_HYPER_LINK").getItem();
    private final ItemStack qtl  = SlimefunItem.getById("LOGITECH_QUANTUM_LINK").getItem();
    private final ItemStack tin = ItemStackUtil.getCleanItem(ExpansionItemStacks.AUTHOR_TINALNESS);
    private final ItemStack gns = ItemStackUtil.getCleanItem(ExpansionItemStacks.HANGING_GRID_NEW_STYLE);

    public final ItemStack[] RECIPE_LINKER_GRID = new ItemStack[] {
        hyp, qtl, hyp,
        tin, gns, tin,
        hyp, qtl, hyp
    };

    public final LinkerGrid LINKER_GRID = new LinkerGrid(
        ExpansionItemsMenus.MENU_FUNCTIONAL_MACHINE,
        ExpansionItemStacks.LINKER_GRID,
        ExpansionWorkbench.TYPE,
        RECIPE_LINKER_GRID
    );

    public LogitechIntegration() {
        ExpansionItemsMenus.SUB_MENU_ADVANCED_NETWORKS.addTo(LINKER_GRID.registerThis());
    }
}
