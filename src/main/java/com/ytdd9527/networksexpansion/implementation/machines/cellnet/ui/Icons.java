package com.ytdd9527.networksexpansion.implementation.machines.cellnet.ui;

import com.balugaq.netex.utils.Lang;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.assembly.AssemblyCard;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.Cell;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.GhostItems;
public final class Icons {

    public static final ItemStack BACK = of("cellnet-back-button", Material.BARRIER);
    public static final ItemStack DRIVE_BROWSE = of("cellnet-drive-browse", Material.CHEST);
    public static final ItemStack DRIVE_WHITELIST_ADD = of("cellnet-drive-whitelist-add", Material.NAME_TAG);
    public static final ItemStack ASSEMBLY_SLOT_MARKER = of("cellnet-assembly-slot-marker", Material.LIME_STAINED_GLASS_PANE);
    public static final ItemStack ASSEMBLY_UPGRADE_SLOT = of("cellnet-assembly-upgrade-slot", Material.REDSTONE);
    public static final ItemStack ASSEMBLY_SMART_SLOT = of("cellnet-assembly-smart-slot", Material.AMETHYST_SHARD);
    public static final ItemStack ASSEMBLY_SWITCH_ON = of("cellnet-assembly-switch-on", Material.LIME_STAINED_GLASS_PANE);
    public static final ItemStack ASSEMBLY_SWITCH_OFF = of("cellnet-assembly-switch-off", Material.GRAY_STAINED_GLASS_PANE);
    public static final ItemStack ASSEMBLY_TASK_EMPTY = glint(of("cellnet-assembly-task-empty", Material.BLUE_STAINED_GLASS_PANE));
    public static final ItemStack CARD_SAVE = of("cellnet-assemblycard-save", Material.LIME_DYE);
    public static final ItemStack CARD_CLEAR = of("cellnet-assemblycard-clear", Material.GRAY_DYE);
    public static final ItemStack CARD_ARROW = of("cellnet-assemblycard-arrow", Material.ARROW);
    public static final ItemStack CARD_HINT = of("cellnet-assemblycard-hint", Material.BOOK);
    public static final ItemStack VOID_INFO = of("cellnet-void-info", Material.BOOK);
    public static final ItemStack VOID_CLOSE = of("cellnet-void-close", Material.BARRIER);
    public static final ItemStack CLEANER_CELL_SLOT = of("cellnet-cleaner-cell-slot", Material.LIGHT_GRAY_STAINED_GLASS_PANE);
    public static final ItemStack CLEANER_DISPLAY = of("cellnet-display-placeholder", Material.GREEN_STAINED_GLASS_PANE);
    public static final ItemStack CONVERTER_BORDER = of("cellnet-converter-border", Material.BLUE_STAINED_GLASS_PANE);
    public static final ItemStack CONVERTER_CELL_SLOT = of("cellnet-converter-cell-slot", Material.LIGHT_GRAY_STAINED_GLASS_PANE);
    public static final ItemStack CONVERTER_CLOSE = of("cellnet-converter-close", Material.BARRIER);
    public static final ItemStack CELL_SETTING_SLOT = of("cellnet-setting-slot", Material.GREEN_STAINED_GLASS_PANE);
    public static final ItemStack SEARCH_EMPTY = of("cellnet-search-empty", Material.BARRIER);
    public static final ItemStack ASSEMBLER_TEMPLATE_SLOT = of("cellnet-template-slot-marker", Material.LIGHT_BLUE_STAINED_GLASS_PANE);
    public static final ItemStack ASSEMBLER_STATUS = of("cellnet-assembler-status", Material.OBSERVER);
    public static final ItemStack ASSEMBLER_PAUSE = of("cellnet-assembler-pause", Material.GRAY_DYE);
    public static final ItemStack ASSEMBLER_RESUME = of("cellnet-assembler-resume", Material.LIME_DYE);
    public static final ItemStack ASSEMBLER_BOX_OFF = of("cellnet-assembler-box-off", Material.GRAY_DYE);
    public static final ItemStack ASSEMBLER_BOX_ON = of("cellnet-assembler-box-on", Material.LIME_DYE);
    public static final ItemStack ASSEMBLY_DRIVE_CLEAR_SLOTS = of("cellnet-assembly-drive-clear-slots", Material.LAVA_BUCKET);
    public static final ItemStack ASSEMBLY_WORKSHOP_ENCODE = of("cellnet-workshop-encode", Material.ANVIL);
    public static final ItemStack ASSEMBLY_WORKSHOP_CLEAR = of("cellnet-workshop-clear", Material.LAVA_BUCKET);
    public static final ItemStack CONVERTER_MODE_EXPORT = of("cellnet-converter-mode-export", Material.REPEATER);
    public static final ItemStack CONVERTER_MODE_IMPORT = of("cellnet-converter-mode-import", Material.REPEATER);
    public static final ItemStack CELL_TOGGLE_ON = of("cellnet-cell-toggle-on", Material.GREEN_DYE);
    public static final ItemStack CELL_TOGGLE_OFF = of("cellnet-cell-toggle-off", Material.RED_DYE);
    public static final ItemStack CELL_UPGRADE_MAX = of("cellnet-cell-upgrade-max", Material.BARRIER);
    public static final ItemStack PAGE_ARROW = of("cellnet-page-arrow", Material.ARROW);
    public static final ItemStack SEARCH_ICON = of("cellnet-search-icon", Material.SPYGLASS);
    public static final ItemStack PREVIEW_FILL = of("cellnet-preview-fill", Material.WHITE_STAINED_GLASS_PANE);
    public static final ItemStack LINE_MODULE_SLOT = of("cellnet-line-module-slot", Material.LIME_STAINED_GLASS_PANE);
    public static final ItemStack LINE_TEMPLATE_LOCKED = of("cellnet-line-template-locked", Material.RED_STAINED_GLASS_PANE);
    public static final ItemStack LINE_DISTANCE = of("cellnet-line-distance", Material.RECOVERY_COMPASS);
    public static final ItemStack LINE_MODE_ON = of("cellnet-line-mode-on", Material.REPEATER);
    public static final ItemStack LINE_MODE_OFF = of("cellnet-line-mode-off", Material.GRAY_DYE);
    public static final ItemStack LINE_DIRECTION_ON = of("cellnet-line-direction-on", Material.GREEN_STAINED_GLASS_PANE);
    public static final ItemStack LINE_DIRECTION_OFF = of("cellnet-line-direction-off", Material.BLUE_STAINED_GLASS_PANE);
    public static final ItemStack LINE_DIRECTION_FALLBACK = of("cellnet-line-direction-fallback", Material.COMPASS);
    public static final ItemStack CHAIN_BINDING_ENTRY = of("cellnet-chain-binding-entry", Material.LEAD);
    public static final ItemStack CHAIN_BINDING_ENTRY_LOCKED = of("cellnet-chain-binding-entry-locked", Material.GRAY_DYE);
    public static final ItemStack CHAIN_BINDING_INFO = of("cellnet-chain-binding-info", Material.BOOK);
    public static final ItemStack CHAIN_BINDING_MODE_ON = glint(of("cellnet-chain-binding-mode-on", Material.REPEATER));

    private Icons() {
    }

    @NotNull
    private static ItemStack of(@NotNull String key, @NotNull Material fallback) {
        ItemStack icon = Lang.getIcon(key, fallback);
        return icon != null ? icon : new ItemStack(fallback);
    }

    @NotNull
    private static ItemStack glint(@NotNull ItemStack icon) {
        ItemStack glinted = icon.clone();
        glinted.editMeta(meta -> {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        });
        return glinted;
    }
    static {
        GhostItems.mark(BACK);
        GhostItems.mark(CLEANER_DISPLAY);
        GhostItems.mark(SEARCH_EMPTY);
        GhostItems.mark(PREVIEW_FILL);
    }
}