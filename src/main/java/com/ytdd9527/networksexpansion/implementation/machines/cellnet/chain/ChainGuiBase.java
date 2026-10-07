package com.ytdd9527.networksexpansion.implementation.machines.cellnet.chain;

import com.balugaq.netex.api.enums.TransportMode;
import com.balugaq.netex.utils.Debug;
import com.balugaq.netex.utils.Lang;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.GhostItems;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.ui.Icons;
import io.github.sefiraat.networks.slimefun.network.NetworkDirectional;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.ui.CellnetText;
public abstract class ChainGuiBase extends AbstractChainMachine {

    public static final int UP_SLOT = 11;
    public static final int NORTH_SLOT = 12;
    public static final int DOWN_SLOT = 13;
    public static final int WEST_SLOT = 20;
    public static final int EAST_SLOT = 22;
    public static final int SOUTH_SLOT = 30;
    public static final int BINDING_ENTRY_SLOT = 4;

    protected ChainGuiBase(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    protected void wireMenu(@NotNull BlockMenu menu, @NotNull LineState state) {
        loadWorkState(menu.getLocation(), state);
        recountModules(menu, state);
        wireTemplateSlots(menu, state);
        wireModuleSlots(menu, state);
        wireModeSlot(menu, state);
        wireDistanceSlot(menu, state);
        wireDirectionSlots(menu, state);
        wireBindingEntry(menu, state);
        menu.addMenuOpeningHandler(p -> {
            recountModules(menu, state);
            refreshAll(menu, state);
            ChainRangeParticles.show(menu.getLocation(), state.effectiveDirections(),
                effectiveDistance(state));
        });
        menu.addMenuCloseHandler(p -> ChainRangeParticles.fadeLater(menu.getLocation()));
    }

    private void wireBindingEntry(@NotNull BlockMenu menu, @NotNull LineState state) {
        menu.addMenuClickHandler(BINDING_ENTRY_SLOT, (p, s, i, a) -> {
            if (getWorkMode() == WorkMode.GRAB) {
                p.sendMessage(Lang.getString(CellnetText.BINDING_GRABBER_UNSUPPORTED));
                return false;
            }
            if (!state.bindingModule) {
                p.sendMessage(Lang.getString(CellnetText.BINDING_MODULE_REQUIRED));
                return false;
            }
            ChainBindingMenu.openDirections(p, menu.getLocation());
            return false;
        });
    }

    private void refreshBindingEntry(@NotNull BlockMenu menu, @NotNull LineState state) {
        boolean usable = getWorkMode() != WorkMode.GRAB && state.bindingModule;
        ItemStack icon = (usable ? Icons.CHAIN_BINDING_ENTRY : Icons.CHAIN_BINDING_ENTRY_LOCKED).clone();
        icon.editMeta(meta -> {
            meta.setDisplayName(Lang.getString(usable
                ? CellnetText.BINDING_ENTRY_NAME
                : CellnetText.BINDING_LOCKED_NAME));
            meta.setLore(List.of(Lang.getString(usable
                ? CellnetText.BINDING_ENTRY_LORE
                : CellnetText.BINDING_LOCKED_LORE)));
        });
        GhostItems.mark(icon);
        menu.replaceExistingItem(BINDING_ENTRY_SLOT, icon);
    }

    private void wireTemplateSlots(@NotNull BlockMenu menu, @NotNull LineState state) {
        for (int i = 0; i < TEMPLATE_SLOTS.length; i++) {
            int index = i;
            int slot = TEMPLATE_SLOTS[i];
            menu.addMenuClickHandler(slot, (p, s, item, action) -> {
                if (getWorkMode() == WorkMode.GRAB) {
                    return false;
                }
                ItemStack current = menu.getItemInSlot(slot);
                boolean placeholder = isTemplatePlaceholder(current);
                if (index >= templateSlotCount(state)) {
                    ItemStack cursor0 = p.getItemOnCursor();
                    if ((cursor0 == null || cursor0.getType().isAir())
                        && current != null && !current.getType().isAir() && !placeholder) {
                        p.setItemOnCursor(current);
                        menu.replaceExistingItem(slot, lockedTemplatePlaceholder());
                    }
                    return false;
                }
                ItemStack cursor = p.getItemOnCursor();
                if (cursor == null || cursor.getType().isAir()) {
                    return !placeholder;
                }
                if (placeholder) {
                    menu.replaceExistingItem(slot, cursor.clone());
                    p.setItemOnCursor(null);
                    return false;
                }
                return true;
            });
        }
    }

    private void wireModuleSlots(@NotNull BlockMenu menu, @NotNull LineState state) {
        for (int i = 0; i < MODULE_COUNT; i++) {
            int moduleSlot = MODULE_START + i;
            menu.addMenuClickHandler(moduleSlot, (p, slot, item, action) -> {
                handleModuleClick(menu, state, p, moduleSlot);
                return false;
            });
        }
    }

    private void handleModuleClick(
        @NotNull BlockMenu menu, @NotNull LineState state, @NotNull Player player, int slot) {
        ItemStack current = menu.getItemInSlot(slot);
        boolean currentIsModule = current != null && ChainModule.of(current) != null;
        ItemStack cursor = player.getItemOnCursor();
        boolean cursorAir = cursor == null || cursor.getType().isAir();
        ChainModule cursorModule = ChainModule.of(cursor);
        boolean changed = false;
        if (cursorAir) {
            if (currentIsModule) {
                player.setItemOnCursor(current);
                menu.replaceExistingItem(slot, modulePlaceholder());
                changed = true;
            }
        } else if (cursorModule != null) {
            if (getWorkMode() == WorkMode.GRAB && cursorModule == ChainModule.CAPACITY) {
                player.sendMessage(Lang.getString(CellnetText.LINE_MODULE_CAPACITY_UNSUPPORTED));
                return;
            }
            if (getWorkMode() == WorkMode.GRAB && cursorModule == ChainModule.BINDING) {
                player.sendMessage(Lang.getString(CellnetText.LINE_MODULE_BINDING_UNSUPPORTED));
                return;
            }
            if (currentIsModule) {
                player.getWorld().dropItemNaturally(player.getLocation(), current);
            }
            menu.replaceExistingItem(slot, cursor.asOne());
            cursor.setAmount(cursor.getAmount() - 1);
            changed = true;
        } else {
            player.sendMessage(Lang.getString(CellnetText.LINE_MODULE_NOT_MODULE));
        }
        if (changed) {
            recountModules(menu, state);
            refreshAll(menu, state);
            saveWorkState(menu.getLocation(), state);
        }
    }

    private void wireModeSlot(@NotNull BlockMenu menu, @NotNull LineState state) {
        menu.addMenuClickHandler(MODE_SLOT, (p, slot, item, action) -> {
            if (!state.modeModule) {
                p.sendMessage(Lang.getString(CellnetText.LINE_MODE_LOCKED));
                return false;
            }
            state.transportMode = action.isRightClicked()
                ? state.transportMode.previous()
                : state.transportMode.next();
            StorageCacheUtils.setData(menu.getLocation(), KEY_MODE, state.transportMode.name());
            refreshModeIcon(menu, state);
            return false;
        });
    }

    private void wireDistanceSlot(@NotNull BlockMenu menu, @NotNull LineState state) {
        menu.addMenuClickHandler(DISTANCE_SLOT, (p, slot, item, action) -> {
            ItemStack cursor = p.getItemOnCursor();
            if (cursor == null || cursor.getType().isAir()) {
                return false;
            }
            int cap = distanceCap(state);
            int old = state.distance;
            state.distance = Math.max(1, Math.min(cursor.getAmount(), cap));
            if (old != state.distance) {
                StorageCacheUtils.setData(menu.getLocation(), KEY_DISTANCE, String.valueOf(state.distance));
                p.sendMessage(Lang.getString(CellnetText.LINE_DISTANCE_SET, state.distance, cap));
            } else {
                p.sendMessage(Lang.getString(CellnetText.LINE_DISTANCE_LOCKED, state.distance, cap));
            }
            refreshDistanceIcon(menu, state);
            return false;
        });
    }

    private void wireDirectionSlots(@NotNull BlockMenu menu, @NotNull LineState state) {
        for (BlockFace face : NetworkDirectional.VALID_FACES) {
            int slot = directionSlot(face);
            if (slot < 0) {
                continue;
            }
            menu.addMenuClickHandler(slot, (p, s, item, action) -> {
                if (action.isShiftClicked() && !action.isRightClicked()) {
                    ChainBindingMenu.openTarget(p, menu.getBlock().getRelative(face).getLocation());
                    return false;
                }
                if (state.multiDirection) {
                    toggleDirection(state, face);
                } else {
                    state.directions.clear();
                    state.directions.add(face);
                    StorageCacheUtils.setData(menu.getLocation(), KEY_DIRECTION, face.name());
                }
                saveWorkState(menu.getLocation(), state);
                refreshDirectionIcons(menu, state);
                return false;
            });
        }
    }

    private void toggleDirection(@NotNull LineState state, @NotNull BlockFace face) {
        if (!state.directions.remove(face)) {
            state.directions.add(face);
        }
        if (state.directions.isEmpty()) {
            state.directions.add(face);
        }
    }

    private void refreshAll(@NotNull BlockMenu menu, @NotNull LineState state) {
        refreshDistanceIcon(menu, state);
        refreshModeIcon(menu, state);
        refreshDirectionIcons(menu, state);
        refreshModulePlaceholders(menu);
        refreshTemplatePlaceholders(menu, state);
        refreshBindingEntry(menu, state);
    }

    private void refreshDistanceIcon(@NotNull BlockMenu menu, @NotNull LineState state) {
        ItemStack icon = Icons.LINE_DISTANCE.clone();
        icon.setAmount(Math.max(1, Math.min(state.distance, 64)));
        icon.editMeta(meta -> {
            meta.setDisplayName(Lang.getString(CellnetText.LINE_DISTANCE_NAME, state.distance, distanceCap(state)));
            meta.setLore(Lang.getStringList(CellnetText.LINE_DISTANCE_LORE));
        });
        GhostItems.mark(icon);
        menu.replaceExistingItem(DISTANCE_SLOT, icon);
    }

    private void refreshModeIcon(@NotNull BlockMenu menu, @NotNull LineState state) {
        TransportMode current = state.modeModule ? state.transportMode : TransportMode.NONE;
        ItemStack icon = (state.modeModule ? Icons.LINE_MODE_ON : Icons.LINE_MODE_OFF).clone();
        icon.editMeta(meta -> {
            meta.setDisplayName(Lang.getString(CellnetText.LINE_MODE_NAME));
            List<String> lore = new ArrayList<>();
            for (TransportMode mode : TransportMode.values()) {
                String name = Lang.getString(CellnetText.LINE_MODES_PREFIX + mode.name());
                if (mode == current) {
                    lore.add(Lang.getString(CellnetText.LINE_MODE_ROW_CURRENT, name));
                } else {
                    lore.add(Lang.getString(CellnetText.LINE_MODE_ROW_OTHER, name));
                }
            }
            lore.add(Lang.getString(state.modeModule
                ? CellnetText.LINE_MODE_HINT
                : CellnetText.LINE_MODE_LOCKED));
            meta.setLore(lore);
        });
        GhostItems.mark(icon);
        menu.replaceExistingItem(MODE_SLOT, icon);
    }

    private void refreshDirectionIcons(@NotNull BlockMenu menu, @NotNull LineState state) {
        Set<BlockFace> effective = state.effectiveDirections();
        for (BlockFace face : NetworkDirectional.VALID_FACES) {
            int slot = directionSlot(face);
            if (slot < 0) {
                continue;
            }
            try {
                boolean selected = effective.contains(face);
                menu.replaceExistingItem(slot,
                    ChainBindingMenu.directionPane(menu.getLocation(), face, selected));
            } catch (Exception e) {
                Debug.trace(e, "刷新方向面板失败: " + face);
                menu.replaceExistingItem(slot, fallbackDirectionPane(face));
            }
        }
    }

    private @NotNull ItemStack fallbackDirectionPane(@NotNull BlockFace face) {
        ItemStack icon = Icons.LINE_DIRECTION_FALLBACK.clone();
        icon.editMeta(meta -> meta.setDisplayName(Lang.getString(
            CellnetText.LINE_DIRECTION_PREFIX + face.name().toLowerCase(Locale.ROOT))));
        GhostItems.mark(icon);
        return icon;
    }

    private void refreshModulePlaceholders(@NotNull BlockMenu menu) {
        for (int slot = MODULE_START; slot < MODULE_START + MODULE_COUNT; slot++) {
            ItemStack current = menu.getItemInSlot(slot);
            if (current == null || current.getType().isAir()) {
                menu.replaceExistingItem(slot, modulePlaceholder());
            }
        }
    }

    private @NotNull ItemStack modulePlaceholder() {
        ItemStack icon = Icons.LINE_MODULE_SLOT.clone();
        icon.editMeta(meta -> {
            meta.setDisplayName(Lang.getString(CellnetText.LINE_MODULE_SLOT_NAME));
            meta.setLore(List.of(Lang.getString(CellnetText.LINE_MODULE_SLOT_LORE)));
        });
        GhostItems.mark(icon);
        return icon;
    }

    private @NotNull ItemStack lockedTemplatePlaceholder() {
        ItemStack icon = Icons.LINE_TEMPLATE_LOCKED.clone();
        icon.editMeta(meta -> {
            meta.setDisplayName(Lang.getString(CellnetText.LINE_TEMPLATE_LOCKED_NAME));
            meta.setLore(List.of(Lang.getString(CellnetText.LINE_TEMPLATE_LOCKED_LORE)));
            meta.getPersistentDataContainer().set(PLACEHOLDER_KEY,
                PersistentDataType.BYTE, (byte) 1);
        });
        GhostItems.mark(icon);
        return icon;
    }

    private void refreshTemplatePlaceholders(@NotNull BlockMenu menu, @NotNull LineState state) {
        if (getWorkMode() == WorkMode.GRAB) {
            return;
        }
        int unlocked = templateSlotCount(state);
        for (int i = 0; i < TEMPLATE_SLOTS.length; i++) {
            int slot = TEMPLATE_SLOTS[i];
            ItemStack current = menu.getItemInSlot(slot);
            boolean placeholder = isTemplatePlaceholder(current);
            if (i < unlocked) {
                if (placeholder) {
                    menu.replaceExistingItem(slot, null);
                }
            } else if (current == null || current.getType().isAir()) {
                menu.replaceExistingItem(slot, lockedTemplatePlaceholder());
            }
        }
    }

    @Override
    protected void clearDisplayIcons(@NotNull BlockMenu menu) {
        menu.replaceExistingItem(MODE_SLOT, null);
        menu.replaceExistingItem(DISTANCE_SLOT, null);
        menu.replaceExistingItem(BINDING_ENTRY_SLOT, null);
        for (BlockFace face : NetworkDirectional.VALID_FACES) {
            int slot = directionSlot(face);
            if (slot >= 0) {
                menu.replaceExistingItem(slot, null);
            }
        }
        for (int slot = MODULE_START; slot < MODULE_START + MODULE_COUNT; slot++) {
            ItemStack current = menu.getItemInSlot(slot);
            if (current != null && ChainModule.of(current) == null) {
                menu.replaceExistingItem(slot, null);
            }
        }
        for (int slot : TEMPLATE_SLOTS) {
            if (isTemplatePlaceholder(menu.getItemInSlot(slot))) {
                menu.replaceExistingItem(slot, null);
            }
        }
    }

    private static int directionSlot(@NotNull BlockFace face) {
        return switch (face) {
            case NORTH -> NORTH_SLOT;
            case UP -> UP_SLOT;
            case WEST -> WEST_SLOT;
            case EAST -> EAST_SLOT;
            case SOUTH -> SOUTH_SLOT;
            case DOWN -> DOWN_SLOT;
            default -> -1;
        };
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                setSize(54);
                boolean grab = getWorkMode() == WorkMode.GRAB;
                for (int slot = 0; slot < 54; slot++) {
                    if (slot == BINDING_ENTRY_SLOT) {
                        continue;
                    }
                    if (!grab && ChainBindingMenu.in(TEMPLATE_SLOTS, slot)) {
                        continue;
                    }
                    if (slot >= MODULE_START && slot < MODULE_START + MODULE_COUNT) {
                        continue;
                    }
                    addItem(slot, ChestMenuUtils.getBackground(), (p, s, i, a) -> false);
                }
            }

            @Override
            public boolean canOpen(@NotNull Block block, @NotNull Player player) {
                return player.hasPermission("slimefun.inventory.bypass")
                    || (ChainGuiBase.this.canUse(player, false)
                    && Slimefun.getProtectionManager()
                    .hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK));
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }

            @Override
            public void newInstance(@NotNull BlockMenu menu, @NotNull Block block) {
                setupMenu(menu);
            }
        };
    }

    private void setupMenu(@NotNull BlockMenu menu) {
        LineState state = state(menu.getLocation());
        for (int slot = 0; slot < 54; slot++) {
            menu.addMenuClickHandler(slot, (p, s, i, a) -> false);
        }
        wireMenu(menu, state);
        state.wired = true;
    }
}
