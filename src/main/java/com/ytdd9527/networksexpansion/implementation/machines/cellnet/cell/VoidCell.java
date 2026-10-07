package com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.menu.VoidCellMenu;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import net.guizhanss.minecraft.guizhanlib.gugu.minecraft.helpers.inventory.ItemStackHelper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.ui.CellnetText;
public class VoidCell extends SpecialSlimefunItem implements NotPlaceable {

    public VoidCell(
            @NotNull ItemGroup itemGroup,
            @NotNull SlimefunItemStack item,
            @NotNull RecipeType recipeType,
            ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void preRegister() {
        addItemHandler(CellUseHandler.create(new CellUseHandler.CellUse() {
            @Override
            public boolean prepare(@NotNull Player player, @NotNull ItemStack cell) {
                VoidCellSupport.ensureInitialized(cell);
                renderLore(cell);
                return true;
            }

            @Override
            public void open(@NotNull Player player, @NotNull ItemStack cell) {
                openMenu(player, cell);
            }
        }));
    }

    protected void openMenu(@NotNull Player player, @NotNull ItemStack cellItem) {
        VoidCellMenu.openFromHand(player, cellItem);
    }

    public void renderLore(@NotNull ItemStack cellItem) {
        var meta = cellItem.getItemMeta();
        if (meta == null) {
            return;
        }
        List<String> lore = new ArrayList<>(Lang.getStringList(CellnetText.VOID_CELL_LORE));
        lore.add(Lang.getString(CellnetText.VOID_EXTRA_WHITELIST));
        appendFilterSummary(lore, cellItem);
        meta.setLore(lore);
        cellItem.setItemMeta(meta);
    }

    private static void appendFilterSummary(@NotNull List<String> lore, @NotNull ItemStack cellItem) {
        List<ItemStack> filters = VoidCellSupport.filtersView(cellItem);
        if (filters.isEmpty()) {
            lore.add(Lang.getString(CellnetText.VOID_LORE_FILTER_EMPTY));
            return;
        }
        lore.add(Lang.getString(CellnetText.VOID_LORE_FILTER_HEADER, filters.size()));
        for (ItemStack filter : filters) {
            lore.add(Lang.getString(CellnetText.VOID_LORE_FILTER_ENTRY, ItemStackHelper.getDisplayName(filter)));
        }
    }
}
