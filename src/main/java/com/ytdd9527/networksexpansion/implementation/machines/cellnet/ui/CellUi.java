package com.ytdd9527.networksexpansion.implementation.machines.cellnet.ui;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.ledger.CellLedger;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.ledger.CellPersistence;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.CellTier;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.GhostItems;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.ui.Icons;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.ItemKey;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.support.NumberFormat;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import net.guizhanss.minecraft.guizhanlib.gugu.minecraft.helpers.inventory.ItemStackHelper;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import com.ytdd9527.networksexpansion.implementation.machines.cellnet.cell.Cell;

public final class CellUi {

    private static final int BAR_TOTAL = 20;

    private CellUi() {
    }

    @NotNull
    public static String progressBar(long current, long max) {
        ChatColor fillColor = barColor(current, max);
        return fillColor + barBody(barFill(current, max), fillColor);
    }

    @NotNull
    public static ChatColor barColor(long current, long max) {
        if (max == Long.MAX_VALUE) {
            return ChatColor.YELLOW;
        }
        return current >= max ? ChatColor.GREEN : ChatColor.YELLOW;
    }

    public static long barFill(long current, long max) {
        if (max == Long.MAX_VALUE || max <= 0) {
            return 0L;
        }
        long filled = (current * BAR_TOTAL + max - 1) / max;
        return Math.min(BAR_TOTAL, Math.max(0L, filled));
    }

    @NotNull
    public static String barBody(long filled, @NotNull ChatColor fillColor) {
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < BAR_TOTAL; i++) {
            if (i < filled) {
                bar.append(fillColor).append("■");
            } else {
                bar.append(ChatColor.GRAY).append("·");
            }
        }
        return bar.toString();
    }

    @NotNull
    public static ItemStack displayItem(@NotNull CellLedger ledger, @NotNull ItemStack sample, long amount) {
        ItemKey key = new ItemKey(sample);
        return browseEntry(sample, amount,
            Lang.getString(ledger.isReserved(key)
                ? CellnetText.CELL_BUTTON_RESERVED_ON_LORE
                : CellnetText.CELL_BUTTON_RESERVED_OFF_LORE),
            Lang.getString(ledger.isVoidExcessUnit(key)
                ? CellnetText.CELL_BUTTON_VOID_EXCESS_ON_LORE
                : CellnetText.CELL_BUTTON_VOID_EXCESS_OFF_LORE));
    }

    @NotNull
    public static ItemStack browseEntry(@NotNull ItemStack sample, long amount, @NotNull String... extraLore) {
        ItemStack item = sample.clone();
        item.setAmount(1);
        item.editMeta(meta -> {
            meta.setDisplayName(ItemStackHelper.getDisplayName(sample));
            List<String> lore = new ArrayList<>();
            lore.add(Lang.getString(CellnetText.CELL_BUTTON_ITEM_COUNT, NumberFormat.formatNumber(amount)));
            for (String line : extraLore) {
                lore.add(line);
            }
            meta.setLore(lore);
        });
        GhostItems.mark(item);
        return item;
    }

    @NotNull
    public static ItemStack settingSlotItem() {
        return Icons.CELL_SETTING_SLOT;
    }

    @NotNull
    public static ItemStack whitelistButton(@NotNull CellLedger ledger) {
        ItemStack item = new ItemStack(ledger.isWhitelistEnabled() ? Material.LIME_DYE : Material.GRAY_DYE);
        item.editMeta(meta -> {
            meta.setDisplayName(Lang.getString(CellnetText.CELL_BUTTON_WHITELIST));
            List<String> lore = new ArrayList<>();
            lore.add(Lang.getString(ledger.isWhitelistEnabled()
                ? CellnetText.CELL_BUTTON_WHITELIST_ENABLED
                : CellnetText.CELL_BUTTON_WHITELIST_DISABLED));
            boolean unlimited = ledger.getMaxUnits() == Long.MAX_VALUE;
            lore.add(Lang.getString(CellnetText.CELL_BUTTON_WHITELIST_COUNT,
                ledger.getWhitelist().size(), unlimited ? "∞" : NumberFormat.formatNumber(ledger.getCurrentPerTypeLimit())));
            lore.add(Lang.getString(CellnetText.CELL_BUTTON_PER_UNIT_CAPACITY,
                unlimited ? "∞" : NumberFormat.formatNumber(ledger.getPerTypeLimit())));
            lore.add("");
            lore.add(Lang.getString(CellnetText.CELL_BUTTON_OPEN_WHITELIST));
            meta.setLore(lore);
        });
        return item;
    }

    @NotNull
    public static ItemStack toggleButton(@NotNull CellLedger ledger) {
        return ledger.isWhitelistEnabled() ? Icons.CELL_TOGGLE_ON : Icons.CELL_TOGGLE_OFF;
    }

    @NotNull
    public static ItemStack whitelistSlotItem(@NotNull ItemStack sample) {
        ItemStack item = sample.clone();
        item.setAmount(1);
        item.editMeta(meta -> {
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add(Lang.getString(CellnetText.CELL_BUTTON_REMOVE_ITEM));
            meta.setLore(lore);
        });
        GhostItems.mark(item);
        return item;
    }

    @NotNull
    public static ItemStack upgradeButton(@NotNull CellLedger ledger) {
        NetworkQuantumStorage required = CellTier.upgradeMaterialOf(ledger.getPerTypeLimit());
        ItemStack item;
        if (required != null) {
            item = required.getItem().clone();
            item.setAmount(1);
        } else {
            item = Icons.CELL_UPGRADE_MAX.clone();
        }
        item.editMeta(meta -> {
            boolean unlimited = ledger.getMaxUnits() == Long.MAX_VALUE;
            meta.setDisplayName(Lang.getString(unlimited
                ? CellnetText.CELL_BUTTON_NO_UPGRADE_NEEDED
                : CellnetText.CELL_BUTTON_UPGRADE));
            List<String> lore = new ArrayList<>();
            long maxUnits = ledger.getMaxUnits();
            long currentUnits = Math.min(ledger.getCurrentPerTypeLimit(), maxUnits);
            String unitText = unlimited ? "∞" : NumberFormat.formatNumber(currentUnits);
            lore.add(Lang.getString(CellnetText.CELL_BUTTON_PER_UNIT_CAPACITY, unlimited ? "∞" : NumberFormat.formatNumber(ledger.getPerTypeLimit())));
            lore.add(Lang.getString(CellnetText.CELL_BUTTON_CURRENT_UNITS, unitText, unlimited ? "∞" : NumberFormat.formatNumber(maxUnits)));
            if (!unlimited) {
                lore.add(progressBar(currentUnits, maxUnits));
            }
            if (unlimited) {
                lore.add(Lang.getString(CellnetText.CELL_BUTTON_UNLIMITED_HINT));
            } else if (required != null) {
                lore.add(Lang.getString(CellnetText.CELL_BUTTON_UPGRADE_MATERIAL, ItemStackHelper.getDisplayName(required.getItem())));
                lore.add(Lang.getString(CellnetText.CELL_BUTTON_UPGRADE_CONSUME));
            } else {
                lore.add(Lang.getString(CellnetText.CELL_BUTTON_NO_UPGRADE_MATERIAL));
                lore.add(Lang.getString(CellnetText.CELL_BUTTON_RAW_CAPACITY, ledger.getPerTypeLimit()));
            }
            meta.setLore(lore);
        });
        return item;
    }

    @NotNull
    public static ItemStack renameButton(@NotNull CellLedger ledger) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        item.editMeta(meta -> {
            meta.setDisplayName(Lang.getString(CellnetText.CELL_BUTTON_RENAME));
            List<String> lore = new ArrayList<>();
            String customName = CellPersistence.getCustomName(ledger.getUuid());
            lore.add(Lang.getString(CellnetText.CELL_BUTTON_RENAME_CURRENT, customName != null ? customName : Lang.getString(CellnetText.CELL_BUTTON_RENAME_DEFAULT)));
            lore.add("");
            lore.add(Lang.getString(CellnetText.CELL_BUTTON_RENAME_HINT));
            meta.setLore(lore);
        });
        return item;
    }
}
