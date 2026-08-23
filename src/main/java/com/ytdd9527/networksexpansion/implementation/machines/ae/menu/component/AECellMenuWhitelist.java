package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.component;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.ae.utils.AENumberFormat;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AEStorageCellCache;
import io.github.sefiraat.networks.utils.StackUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.slot.AECellMenuSlots;

public final class AECellMenuWhitelist {

    private AECellMenuWhitelist() {
    }

    public static void render(@NotNull ChestMenu menu, @NotNull UUID uuid) {
        AEStorageCellCache cache = cache(uuid);
        if (cache == null) {
            return;
        }
        List<ItemStack> whitelist = cache.getWhitelist();
        for (int i = 0; i < AECellMenuSlots.LIST_SLOTS.length; i++) {
            int slot = AECellMenuSlots.LIST_SLOTS[i];
            if (i < whitelist.size()) {
                menu.replaceExistingItem(slot, AECellMenuButtons.whitelistSlotItem(whitelist.get(i)));
            } else {
                menu.replaceExistingItem(slot, AECellMenuButtons.settingSlotItem());
            }
        }
        menu.replaceExistingItem(AECellMenuSlots.WHITELIST_TOGGLE, AECellMenuButtons.toggleButton(cache));
        menu.replaceExistingItem(AECellMenuSlots.WHITELIST_BACK, AECellMenuButtons.backButton());
    }

    public static void handleSlotClick(@NotNull ChestMenu menu, @NotNull Player player, int slot, @NotNull UUID uuid) {
        AEStorageCellCache cache = cache(uuid);
        if (cache == null) {
            return;
        }
        int index = AECellMenuSlots.whitelistIndexForSlot(slot);
        if (index < 0) {
            return;
        }
        List<ItemStack> whitelist = cache.getWhitelist();
        ItemStack cursor = player.getItemOnCursor();
        boolean cursorAir = cursor == null || cursor.getType().isAir();

        if (index < whitelist.size()) {
            whitelist.remove(index);
            apply(player, uuid, cache.isWhitelistEnabled(), whitelist);
        } else if (!cursorAir) {
            long limit = Math.min(cache.getCurrentPerTypeLimit(), AECellMenuSlots.LIST_SLOTS.length);
            if (whitelist.size() >= limit) {
                player.sendMessage(Lang.getString("messages.ae.cell.whitelist_limit", AENumberFormat.formatNumber(limit)));
            } else if (!inList(whitelist, cursor)) {
                ItemStack template = cursor.clone();
                template.setAmount(1);
                whitelist.add(template);
                apply(player, uuid, cache.isWhitelistEnabled(), whitelist);
            }
        }

        render(menu, uuid);
    }

    public static void apply(@NotNull Player player, @NotNull UUID uuid, boolean enabled, @NotNull List<ItemStack> whitelist) {
        AEStorageCellCache cache = cache(uuid);
        if (cache != null) {
            cache.setWhitelistEnabled(enabled);
            cache.setWhitelist(whitelist);
        }

        ItemStack cell = AECellMenuPlayerState.findCellInPlayer(player, uuid);
        if (cell != null) {
            AECellMenuPlayerState.updateHeldCell(player, cell);
        }
    }

    private static boolean inList(@NotNull List<ItemStack> list, @NotNull ItemStack sample) {
        for (ItemStack item : list) {
            if (StackUtils.itemsMatch(item, sample)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static AEStorageCellCache cache(@NotNull UUID uuid) {
        return AEStorageCellCache.getActiveCaches().get(uuid);
    }
}