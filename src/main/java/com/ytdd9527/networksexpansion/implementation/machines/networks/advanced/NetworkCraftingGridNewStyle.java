package com.ytdd9527.networksexpansion.implementation.machines.networks.advanced;

import com.balugaq.netex.api.helpers.Icon;
import com.balugaq.netex.api.helpers.SupportedCraftingTableRecipes;
import com.balugaq.netex.api.interfaces.RecipeCompletableWithGuide;
import com.balugaq.netex.api.keybind.Keybinds;
import com.balugaq.netex.utils.BlockMenuUtil;
import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.core.items.machines.AbstractGridNewStyle;
import com.ytdd9527.networksexpansion.implementation.ExpansionItems;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.events.NetworkCraftEvent;
import io.github.sefiraat.networks.network.GridItemRequest;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.slimefun.network.grid.AbstractGrid;
import io.github.sefiraat.networks.slimefun.network.grid.GridCache;
import io.github.sefiraat.networks.slimefun.network.grid.GridCache.DisplayMode;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NullMarked
@SuppressWarnings("DuplicatedCode")
public class NetworkCraftingGridNewStyle extends AbstractGridNewStyle implements RecipeCompletableWithGuide {

    private static final int[] BACKGROUND_SLOTS = {5, 14, 23, 32, 41, 43, 50, 51};

    private static final int[] DISPLAY_SLOTS = {
        0, 1, 2, 3, 4,
        9, 10, 11, 12, 13,
        18, 19, 20, 21, 22,
        27, 28, 29, 30, 31,
        36, 37, 38, 39, 40,
        45, 46, 47, 48, 49
    };

    private static final int KEYBIND_BUTTON_SLOT = 43;

    private static final int CHANGE_SORT = 35;
    private static final int FILTER = 42;
    private static final int PAGE_PREVIOUS = 44;
    private static final int PAGE_NEXT = 53;
    private static final int TOGGLE_MODE_SLOT = 52;
    private static final int CRAFT_BUTTON_SLOT = 33;
    private static final int OUTPUT_SLOT = 34;
    private static final int[] INGREDIENT_SLOTS = {6, 7, 8, 15, 16, 17, 24, 25, 26};
    private static final Map<Location, GridCache> CACHE_MAP = new HashMap<>();

    public NetworkCraftingGridNewStyle(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    @NotNull
    protected BlockMenuPreset getPreset() {
        return new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                // drawBackground(getBackgroundSlots());
                drawBackground(getDisplaySlots());
                setSize(54);
            }

            @Override
            public boolean canOpen(@NotNull Block block, @NotNull Player player) {
                return player.hasPermission("slimefun.inventory.bypass")
                    || (ExpansionItems.NETWORK_CRAFTING_GRID_NEW_STYLE.canUse(player, false)
                    && Slimefun.getProtectionManager()
                    .hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK));
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }

            @Override
            public void newInstance(@NotNull BlockMenu menu, @NotNull Block b) {
                getCacheMap().put(menu.getLocation(), new GridCache(0, 0, GridCache.SortOrder.ALPHABETICAL));

                menu.replaceExistingItem(getPagePrevious(), getPagePreviousStack());
                menu.addMenuClickHandler(getPagePrevious(), (p, slot, item, action) -> {
                    GridCache gridCache = getCacheMap().get(menu.getLocation());
                    gridCache.setPage(gridCache.getPage() <= 0 ? 0 : gridCache.getPage() - 1);
                    getCacheMap().put(menu.getLocation(), gridCache);
                    updateDisplay(menu);
                    return false;
                });

                menu.replaceExistingItem(getPageNext(), getPageNextStack());
                menu.addMenuClickHandler(getPageNext(), (p, slot, item, action) -> {
                    GridCache gridCache = getCacheMap().get(menu.getLocation());
                    gridCache.setPage(
                        gridCache.getPage() >= gridCache.getMaxPages()
                            ? gridCache.getMaxPages()
                            : gridCache.getPage() + 1);
                    getCacheMap().put(menu.getLocation(), gridCache);
                    updateDisplay(menu);
                    return false;
                });

                menu.replaceExistingItem(getChangeSort(), getChangeSortStack());
                menu.addMenuClickHandler(getChangeSort(), (p, slot, item, action) -> {
                    GridCache gridCache = getCacheMap().get(menu.getLocation());
                    AbstractGrid.updateSortOrder(gridCache, action, 4);
                    getCacheMap().put(menu.getLocation(), gridCache);
                    updateDisplay(menu);
                    return false;
                });

                menu.replaceExistingItem(getFilterSlot(), getFilterStack());
                menu.addMenuClickHandler(getFilterSlot(), (p, slot, item, action) -> {
                    GridCache gridCache = getCacheMap().get(menu.getLocation());
                    setFilter(p, menu, gridCache, action);
                    return false;
                });

                menu.replaceExistingItem(getToggleModeSlot(), getModeStack(DisplayMode.DISPLAY));
                menu.addMenuClickHandler(getToggleModeSlot(), (p, slot, item, action) -> {
                    if (!action.isRightClicked()) {
                        GridCache gridCache = getCacheMap().get(menu.getLocation());
                        gridCache.toggleDisplayMode();
                        menu.replaceExistingItem(getToggleModeSlot(), getModeStack(gridCache));
                        updateDisplay(menu);
                    }
                    return false;
                });

                menu.replaceExistingItem(CRAFT_BUTTON_SLOT, Icon.CRAFT_BUTTON_NEW_STYLE);
                menu.addMenuClickHandler(CRAFT_BUTTON_SLOT, (p, slot, item, action) -> {
                    tryCraft(menu, p, action);
                    return false;
                });

                for (int displaySlot : getDisplaySlots()) {
                    menu.replaceExistingItem(displaySlot, ChestMenuUtils.getBackground());
                    menu.addMenuClickHandler(displaySlot, (p, slot, item, action) -> false);
                }

                for (int backgroundSlot : getBackgroundSlots()) {
                    menu.replaceExistingItem(backgroundSlot, ChestMenuUtils.getBackground());
                    menu.addMenuClickHandler(backgroundSlot, (p, slot, item, action) -> false);
                }

                menu.addPlayerInventoryClickHandler(outsideKeybinds());
                addKeybindSettingsButton(menu, getKeybindButtonSlot());
            }
        };
    }

    @NotNull
    public Map<Location, GridCache> getCacheMap() {
        return CACHE_MAP;
    }

    public int[] getBackgroundSlots() {
        return BACKGROUND_SLOTS;
    }

    public int[] getDisplaySlots() {
        return DISPLAY_SLOTS;
    }

    public int getChangeSort() {
        return CHANGE_SORT;
    }

    public int getPagePrevious() {
        return PAGE_PREVIOUS;
    }

    public int getPageNext() {
        return PAGE_NEXT;
    }

    @Override
    public int getKeybindButtonSlot() {
        return KEYBIND_BUTTON_SLOT;
    }

    public int getToggleModeSlot() {
        return TOGGLE_MODE_SLOT;
    }

    @Override
    protected int getFilterSlot() {
        return FILTER;
    }

    @SuppressWarnings("deprecation")
    private synchronized void tryCraft(@NotNull BlockMenu menu, @NotNull Player player, @NotNull ClickAction action) {
        int times = 1;
        if (action.isRightClicked()) {
            times = 64;
        }

        // Get node and, if it doesn't exist - escape
        final NodeDefinition definition = NetworkStorage.getNode(menu.getLocation());
        if (definition.getNode() == null) {
            return;
        }

        NetworkRoot root = definition.getNode().getRoot();
        root.refreshRootItems();

        for (int k = 0; k < times; k++) {
            // Get the recipe input
            final ItemStack[] inputs = new ItemStack[INGREDIENT_SLOTS.length];
            int i = 0;
            for (int recipeSlot : INGREDIENT_SLOTS) {
                ItemStack stack = menu.getItemInSlot(recipeSlot);
                inputs[i] = stack;
                i++;
            }

            ItemStack crafted = null;

            // Go through each slimefun recipe, trigger and set the ItemStack if found
            for (Map.Entry<ItemStack[], ItemStack> entry :
                SupportedCraftingTableRecipes.getRecipes().entrySet()) {
                if (SupportedCraftingTableRecipes.testRecipe(inputs, entry.getKey())) {
                    crafted = entry.getValue();
                    break;
                }
            }

            if (crafted != null) {
                final SlimefunItem sfi2 = SlimefunItem.getByItem(crafted);
                if (sfi2 != null && sfi2.isDisabled()) {
                    player.sendMessage(Lang.getString("messages.unsupported-operation.encoder.disabled_output"));
                    return;
                }
            }

            // If no slimefun recipe found, try a vanilla one
            if (crafted == null) {
                crafted = Bukkit.craftItem(inputs, player.getWorld(), player);
            }

            // If no item crafted OR result doesn't fit, escape
            if (crafted.getType() == Material.AIR || !BlockMenuUtil.fits(menu, crafted, OUTPUT_SLOT)) {
                return;
            }

            // fire craft event
            NetworkCraftEvent event = new NetworkCraftEvent(player, this, inputs, crafted);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            crafted = event.getOutput();

            // Push item
            if (crafted != null) {
                BlockMenuUtil.pushItem(menu, crafted.clone(), OUTPUT_SLOT);
            }

            // Let's clear down all the items
            for (int recipeSlot : INGREDIENT_SLOTS) {
                final ItemStack itemInSlot = menu.getItemInSlot(recipeSlot);
                if (itemInSlot != null) {
                    // Grab a clone for potential retrieval
                    final ItemStack itemInSlotClone = itemInSlot.clone();
                    itemInSlotClone.setAmount(1);
                    BlockMenuUtil.consumeItem(menu, recipeSlot, 1, true);
                    // We have consumed a slot item and now the slot is empty - try to refill
                    if (menu.getItemInSlot(recipeSlot) == null) {
                        // Process item request
                        final GridItemRequest request = new GridItemRequest(itemInSlotClone, 1, player);
                        final ItemStack requestingStack = root.getItemStack(request);
                        if (requestingStack != null) {
                            menu.replaceExistingItem(recipeSlot, requestingStack);
                        }
                    }
                }
            }
        }
    }

    @Override
    public @NotNull List<Keybinds> keybinds() {
        return List.of(displayKeybinds(), outsideKeybinds());
    }

    @Override
    public @NotNull SlimefunItem getSlimefunItem() {
        return this;
    }
}
