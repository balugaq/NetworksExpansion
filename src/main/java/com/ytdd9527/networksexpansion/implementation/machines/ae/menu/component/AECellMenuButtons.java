package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.ae.utils.AENumberFormat;
import com.ytdd9527.networksexpansion.implementation.machines.ae.constants.AECellUpgradeMaterialRegistry;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AEStorageCellCache;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellPersistence;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class AECellMenuButtons {

    private AECellMenuButtons() {
    }

    @NotNull
    public static ItemStack displayItem(@NotNull ItemStack sample, long amount) {
        ItemStack item = new ItemStack(sample.getType());
        item.setAmount(1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ItemStackHelper.getDisplayName(sample));
            meta.setLore(List.of(Lang.getString("messages.ae.cell.button.item_count", AENumberFormat.formatNumber(amount))));
            item.setItemMeta(meta);
        }
        return item;
    }

    @NotNull
    public static ItemStack border() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    @NotNull
    public static ItemStack pageButton(@NotNull String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @NotNull
    public static ItemStack whitelistButton(@NotNull AEStorageCellCache cache) {
        ItemStack item = new ItemStack(cache.isWhitelistEnabled() ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Lang.getString("messages.ae.cell.button.whitelist"));
            List<String> lore = new ArrayList<>();
            lore.add(Lang.getString(cache.isWhitelistEnabled()
                ? "messages.ae.cell.button.whitelist_enabled"
                : "messages.ae.cell.button.whitelist_disabled"));
            lore.add(Lang.getString("messages.ae.cell.button.whitelist_count",
                cache.getWhitelist().size(), AENumberFormat.formatNumber(cache.getCurrentPerTypeLimit())));
            lore.add(Lang.getString("messages.ae.cell.button.per_unit_capacity",
                AENumberFormat.formatNumber(cache.getPerTypeLimit())));
            lore.add("");
            lore.add(Lang.getString("messages.ae.cell.button.open_whitelist"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @NotNull
    public static ItemStack toggleButton(@NotNull AEStorageCellCache cache) {
        ItemStack item = new ItemStack(cache.isWhitelistEnabled() ? Material.GREEN_DYE : Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(cache.isWhitelistEnabled()
                ? Lang.getString("messages.ae.cell.button.toggle_on")
                : Lang.getString("messages.ae.cell.button.toggle_off"));
            meta.setLore(List.of(Lang.getString("messages.ae.cell.button.toggle_hint")));
            item.setItemMeta(meta);
        }
        return item;
    }

    @NotNull
    public static ItemStack settingSlotItem() {
        ItemStack item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Lang.getString("messages.ae.cell.button.setting_slot"));
            meta.setLore(List.of(Lang.getString("messages.ae.cell.button.setting_slot_hint")));
            item.setItemMeta(meta);
        }
        return item;
    }

    @NotNull
    public static ItemStack whitelistSlotItem(@NotNull ItemStack sample) {
        ItemStack item = sample.clone();
        item.setAmount(1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add(Lang.getString("messages.ae.cell.button.remove_item"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @NotNull
    public static ItemStack backButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Lang.getString("messages.ae.cell.button.back"));
            item.setItemMeta(meta);
        }
        return item;
    }

    @NotNull
    public static ItemStack upgradeButton(@NotNull AEStorageCellCache cache) {
        NetworkQuantumStorage required = AECellUpgradeMaterialRegistry.getUpgradeMaterial(cache.getPerTypeLimit());
        ItemStack item;
        if (required != null) {
            item = required.getItem().clone();
            item.setAmount(1);
        } else {
            item = new ItemStack(Material.BARRIER);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Lang.getString("messages.ae.cell.button.upgrade"));
            List<String> lore = new ArrayList<>();
            long maxUnits = cache.getMaxUnits();
            long currentUnits = Math.min(cache.getCurrentPerTypeLimit(), maxUnits);
            lore.add(Lang.getString("messages.ae.cell.button.per_unit_capacity", AENumberFormat.formatNumber(cache.getPerTypeLimit())));
            lore.add(Lang.getString("messages.ae.cell.button.current_units", AENumberFormat.formatNumber(currentUnits), AENumberFormat.formatNumber(maxUnits)));
            if (required != null) {
                lore.add(Lang.getString("messages.ae.cell.button.upgrade_material_id", required.getId()));
                lore.add(Lang.getString("messages.ae.cell.button.upgrade_material", ItemStackHelper.getDisplayName(required.getItem())));
                lore.add(Lang.getString("messages.ae.cell.button.upgrade_consume"));
            } else {
                lore.add(Lang.getString("messages.ae.cell.button.no_upgrade_material"));
                lore.add(Lang.getString("messages.ae.cell.button.raw_capacity", cache.getPerTypeLimit()));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @NotNull
    public static ItemStack renameButton(@NotNull AEStorageCellCache cache) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Lang.getString("messages.ae.cell.button.rename"));
            List<String> lore = new ArrayList<>();
            String customName = AECellPersistence.getCustomName(cache.getUuid());
            lore.add(Lang.getString("messages.ae.cell.button.rename_current", customName != null ? customName : Lang.getString("messages.ae.cell.button.rename_default")));
            lore.add("");
            lore.add(Lang.getString("messages.ae.cell.button.rename_hint"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}