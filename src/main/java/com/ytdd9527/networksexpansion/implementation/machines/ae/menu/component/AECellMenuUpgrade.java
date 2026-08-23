package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.ae.utils.AENumberFormat;
import com.ytdd9527.networksexpansion.implementation.machines.ae.constants.AECellUpgradeMaterialRegistry;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AEStorageCellCache;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellPersistence;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import io.github.sefiraat.networks.utils.Keys;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.slot.AECellMenuSlots;

public final class AECellMenuUpgrade {

    private AECellMenuUpgrade() {
    }

    public static void handleClick(@NotNull ChestMenu menu, @NotNull Player player, @NotNull UUID uuid) {
        AEStorageCellCache cache = cache(uuid);
        if (cache == null) {
            return;
        }
        long maxUnits = cache.getMaxUnits();
        if (cache.getCurrentPerTypeLimit() >= maxUnits) {
            player.sendMessage(Lang.getString("messages.ae.cell.upgrade_maxed", AENumberFormat.formatNumber(maxUnits)));
            return;
        }

        NetworkQuantumStorage required = AECellUpgradeMaterialRegistry.getUpgradeMaterial(cache.getPerTypeLimit());
        if (required == null) {
            player.sendMessage(Lang.getString("messages.ae.cell.upgrade_no_material"));
            return;
        }

        if (!consumeUpgradeMaterial(player, required)) {
            player.sendMessage(Lang.getString("messages.ae.cell.upgrade_missing", ItemStackHelper.getDisplayName(required.getItem())));
            return;
        }

        long newCurrent = Math.min(maxUnits, cache.getCurrentPerTypeLimit() + 1);
        cache.setCurrentPerTypeLimit(newCurrent);

        applyCurrentCapacity(player, uuid, newCurrent);
        menu.replaceExistingItem(AECellMenuSlots.UPGRADE, AECellMenuButtons.upgradeButton(cache));
        player.sendMessage(Lang.getString("messages.ae.cell.upgrade_success", AENumberFormat.formatNumber(newCurrent), AENumberFormat.formatNumber(maxUnits)));
    }

    private static boolean consumeUpgradeMaterial(@NotNull Player player, @NotNull NetworkQuantumStorage required) {
        final String requiredId = required.getId();
        final ItemStack requiredStack = required.getItem();
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            boolean matched = false;
            SlimefunItem sfItem = SlimefunItem.getByItem(stack);
            if (sfItem == required || (sfItem != null && sfItem.getId().equals(requiredId))) {
                matched = true;
            } else if (SlimefunUtils.isItemSimilar(stack, requiredStack, true, false, false)) {
                matched = true;
            }
            if (matched) {
                if (!isEmptyQuantumStorage(stack)) {
                    continue;
                }
                if (stack.getAmount() <= 1) {
                    player.getInventory().setItem(i, null);
                } else {
                    stack.setAmount(stack.getAmount() - 1);
                    player.getInventory().setItem(i, stack);
                }
                return true;
            }
        }
        return false;
    }

    private static boolean isEmptyQuantumStorage(@NotNull ItemStack stack) {
        if (!stack.hasItemMeta()) {
            return true;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return true;
        }
        QuantumCache quantumCache = Keys.getQuantumCache(meta);
        return quantumCache == null
            || quantumCache.getItemStack() == null
            || quantumCache.getAmountLong() <= 0;
    }

    private static void applyCurrentCapacity(@NotNull Player player, @NotNull UUID uuid, long current) {
        ItemStack cell = AECellMenuPlayerState.findCellInPlayer(player, uuid);
        if (cell != null) {
            AECellPersistence.setCurrentPerTypeLimit(cell, current);
            AECellLore.applySpecLore(cell, AECellPersistence.getPerTypeLimit(cell), AECellPersistence.getCurrentPerTypeLimit(cell));
            AECellMenuPlayerState.updateHeldCell(player, cell);
        }
    }

    @Nullable
    private static AEStorageCellCache cache(@NotNull UUID uuid) {
        return AEStorageCellCache.getActiveCaches().get(uuid);
    }
}