package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.ae.constants.AECellUpgradeMaterialRegistry;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellPersistence;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AEStorageCellCache;
import com.ytdd9527.networksexpansion.implementation.machines.ae.utils.AENumberFormat;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AECellLore {

    private AECellLore() {
    }

    // perTypeLimit=单格容量, currentPerTypeLimit=单元数(种类上限), maxUnits=min(perTypeLimit, 全局上限)
    public static void applySpecLore(@NotNull ItemStack itemStack, long perTypeLimit, long currentPerTypeLimit) {
        UUID cellUuid = AECellPersistence.getCellUUID(itemStack);
        // 未初始化 UUID 的元件不重写 lore
        if (cellUuid == null) {
            return;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        List<String> newLore = new ArrayList<>();
        appendBaseLore(meta, newLore);

        AEStorageCellCache cache = AEStorageCellCache.getActiveCaches().get(cellUuid);
        int storedTypes = cache == null ? 0 : cache.getStoredItems().size();
        appendNameLine(newLore, cache);
        appendCapacitySection(newLore, perTypeLimit, currentPerTypeLimit, storedTypes);
        appendStoredItems(newLore, cache);
        appendUpgradeLine(newLore, perTypeLimit, currentPerTypeLimit);
        appendHints(newLore);

        meta.setLore(newLore);
        itemStack.setItemMeta(meta);
    }

    // 元件自带一行基础 lore，只留第一行
    private static void appendBaseLore(@NotNull ItemMeta meta, @NotNull List<String> newLore) {
        List<String> baseLore = meta.getLore();
        if (baseLore != null && !baseLore.isEmpty()) {
            newLore.add(baseLore.get(0));
        }
    }

    private static void appendNameLine(@NotNull List<String> newLore, @Nullable AEStorageCellCache cache) {
        String customName = cache != null ? cache.getCustomName() : null;
        if (customName != null && !customName.isEmpty()) {
            newLore.add(Lang.getString("messages.ae.cell.lore.name_line", customName));
        }
    }

    private static void appendCapacitySection(@NotNull List<String> newLore, long perTypeLimit, long currentPerTypeLimit, int storedTypes) {
        long maxUnits = AECellPersistence.getMaxUnitsFor(perTypeLimit);
        boolean unlimited = maxUnits == Long.MAX_VALUE;
        long slots = unlimited ? Long.MAX_VALUE : Math.max(1L, Math.min(currentPerTypeLimit, maxUnits));
        long filled = Math.min(slots, storedTypes);

        newLore.add(Lang.getString("messages.ae.cell.lore.capacity_line", unlimited ? "∞" : AENumberFormat.formatCellShort(perTypeLimit)));

        int totalBars = 20;
        long barFill = (!unlimited && slots > 0) ? (filled * totalBars + slots - 1) / slots : 0L;
        barFill = Math.min(totalBars, Math.max(0L, barFill));
        StringBuilder bar = new StringBuilder();
        ChatColor fillColor = (unlimited) ? ChatColor.YELLOW : (filled >= slots ? ChatColor.GREEN : ChatColor.YELLOW);
        for (int i = 0; i < totalBars; i++) {
            if (i < barFill) {
                bar.append(fillColor).append("■");
            } else {
                bar.append(ChatColor.GRAY).append("·");
            }
        }
        // 显示"已存种类数 / 当前等级可存种类数"，剩余按当前等级算，而不是按升级上限算
        newLore.add(Lang.getString("messages.ae.cell.lore.units_line",
            fillColor.toString() + AENumberFormat.formatCellNumber(filled),
            unlimited ? "∞" : AENumberFormat.formatCellNumber(slots), bar.toString()));
        long remainingUnits = unlimited ? 0L : Math.max(0L, slots - filled);
        newLore.add(Lang.getString("messages.ae.cell.lore.remaining_units",
            unlimited ? "∞" : AENumberFormat.formatCellNumber(remainingUnits)));
    }

    private static void appendStoredItems(@NotNull List<String> newLore, @Nullable AEStorageCellCache cache) {
        List<AEStorageCellCache.CellEntry> storedItems = cache == null ? List.of() : cache.getStoredItems();
        if (storedItems.isEmpty()) {
            return;
        }
        int shown = Math.min(9, storedItems.size());
        for (int i = 0; i < shown; i++) {
            AEStorageCellCache.CellEntry entry = storedItems.get(i);
            newLore.add(Lang.getString("messages.ae.cell.lore.item_entry", ItemStackHelper.getDisplayName(entry.sample), AENumberFormat.formatNumber(entry.amount)));
        }
        if (storedItems.size() > shown) {
            newLore.add(Lang.getString("messages.ae.cell.lore.and_more", storedItems.size() - shown));
        }
    }

    private static void appendUpgradeLine(@NotNull List<String> newLore, long perTypeLimit, long currentPerTypeLimit) {
        long maxUnits = AECellPersistence.getMaxUnitsFor(perTypeLimit);
        // 无限元件无需升级，不显示升级行
        if (maxUnits == Long.MAX_VALUE) {
            return;
        }
        long remaining = Math.max(0L, maxUnits - Math.max(1L, Math.min(currentPerTypeLimit, maxUnits)));
        if (remaining <= 0) {
            return;
        }
        NetworkQuantumStorage mat = AECellUpgradeMaterialRegistry.getUpgradeMaterial(perTypeLimit);
        String matName = mat == null ? Lang.getString("messages.ae.cell.lore.not_configured") : ItemStackHelper.getDisplayName(mat.getItem());
        newLore.add(Lang.getString("messages.ae.cell.lore.upgrade_line", matName));
    }

    private static void appendHints(@NotNull List<String> newLore) {
        newLore.add(Lang.getString("messages.ae.cell.lore.hint_open"));
        newLore.add(Lang.getString("messages.ae.cell.lore.hint_drive"));
    }
}
