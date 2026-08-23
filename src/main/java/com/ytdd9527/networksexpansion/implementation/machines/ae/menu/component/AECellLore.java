package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.ae.utils.AENumberFormat;
import com.ytdd9527.networksexpansion.implementation.machines.ae.constants.AECellUpgradeMaterialRegistry;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AEStorageCellCache;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellPersistence;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import io.github.sefiraat.networks.utils.Theme;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AECellLore {

    private AECellLore() {
    }

    public static void applySpecLore(@NotNull ItemStack itemStack, long perTypeLimit, long currentPerTypeLimit) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        List<String> newLore = new ArrayList<>();

        List<String> baseLore = meta.getLore();
        if (baseLore != null && !baseLore.isEmpty()) {
            newLore.add(baseLore.get(0));
        }

        final ChatColor P = Theme.PASSIVE.getColor();
        final ChatColor FULL = Theme.SUCCESS.getColor();
        final ChatColor PROG = Theme.MACHINE.getColor();

        final String line = P + ChatColor.STRIKETHROUGH.toString() + "                                 ";

        newLore.add(line);

        UUID cellUuid = AECellPersistence.getCellUUID(itemStack);

        AEStorageCellCache cache = cellUuid == null ? null : AEStorageCellCache.getActiveCaches().get(cellUuid);
        String customName = cache != null ? cache.getCustomName() : null;
        if (customName != null && !customName.isEmpty()) {
            newLore.add(Lang.getString("messages.ae.cell.lore.name_line", customName));
        }

        long maxUnits = AECellPersistence.getMaxUnitsFor(perTypeLimit);
        long safeCurrent = Math.max(1L, Math.min(currentPerTypeLimit, maxUnits));

        newLore.add(Lang.getString("messages.ae.cell.lore.capacity_line", AENumberFormat.formatCellShort(perTypeLimit)));

        int totalBars = 20;
        long filled = (maxUnits > 0) ? (safeCurrent * totalBars + maxUnits - 1) / maxUnits : 0L;
        filled = Math.min(totalBars, Math.max(0L, filled));
        StringBuilder bar = new StringBuilder();
        ChatColor fillColor = (safeCurrent >= maxUnits) ? FULL : PROG;
        for (int i = 0; i < totalBars; i++) {
            if (i < filled) {
                bar.append(fillColor).append("■");
            } else {
                bar.append(P).append("·");
            }
        }
        newLore.add(Lang.getString("messages.ae.cell.lore.units_line", fillColor.toString() + AENumberFormat.formatCellNumber(safeCurrent), AENumberFormat.formatCellNumber(maxUnits), bar.toString()));

        newLore.add("");

        List<AEStorageCellCache.CellEntry> storedItems = cache == null ? List.of() : cache.getStoredItems();
        if (!storedItems.isEmpty()) {
            int shown = Math.min(9, storedItems.size());
            for (int i = 0; i < shown; i++) {
                AEStorageCellCache.CellEntry entry = storedItems.get(i);
                newLore.add(Lang.getString("messages.ae.cell.lore.item_entry", ItemStackHelper.getDisplayName(entry.sample), AENumberFormat.formatNumber(entry.amount)));
            }
            if (storedItems.size() > shown) {
                newLore.add(Lang.getString("messages.ae.cell.lore.and_more", storedItems.size() - shown));
            }
            newLore.add("");
        }

        long remaining = Math.max(0L, maxUnits - safeCurrent);
        if (remaining > 0) {
            NetworkQuantumStorage mat = AECellUpgradeMaterialRegistry.getUpgradeMaterial(perTypeLimit);
            String matName = mat == null ? Lang.getString("messages.ae.cell.lore.not_configured") : ItemStackHelper.getDisplayName(mat.getItem());
            newLore.add(Lang.getString("messages.ae.cell.lore.upgrade_line", matName));
            newLore.add("");
        }

        newLore.add(line);

        newLore.add(Lang.getString("messages.ae.cell.lore.hint_open"));
        newLore.add(Lang.getString("messages.ae.cell.lore.hint_drive"));

        meta.setLore(newLore);
        itemStack.setItemMeta(meta);
    }
}