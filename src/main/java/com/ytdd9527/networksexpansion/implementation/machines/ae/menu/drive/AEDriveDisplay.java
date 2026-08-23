package com.ytdd9527.networksexpansion.implementation.machines.ae.menu.drive;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.cell.AECellHandle;
import com.ytdd9527.networksexpansion.implementation.machines.ae.core.drive.AEDriveStorage;
import com.ytdd9527.networksexpansion.implementation.machines.ae.menu.slot.AEDriveMenuSlots;
import com.ytdd9527.networksexpansion.implementation.machines.ae.utils.AENumberFormat;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AEDriveDisplay {

    private AEDriveDisplay() {
    }

    public static void updateMainDisplay(@NotNull BlockMenu menu, @NotNull AEDriveStorage storage) {
        long totalStored = 0;
        int cellCount = 0;

        List<AECellHandle> handles = storage.getCells(menu);
        for (AECellHandle cell : handles) {
            totalStored += cell.getStoredCount();
            cellCount++;
        }

        Map<ItemStack, Long> aggregatedItems = storage.getAllCellItems(handles);

        menu.replaceExistingItem(AEDriveMenuSlots.DISPLAY_SLOT, buildDisplayItem(cellCount, totalStored, aggregatedItems));
        menu.replaceExistingItem(AEDriveMenuSlots.ACCESS_SLOT, buildAccessButton());
    }

    @NotNull
    private static ItemStack buildDisplayItem(int cellCount, long totalStored, Map<ItemStack, Long> aggregatedItems) {
        ItemStack display = new ItemStack(Material.PAPER);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(Lang.getString("messages.ae.drive.cell_count", cellCount, AEDriveMenuSlots.CELL_SLOT_COUNT));
            lore.add(Lang.getString("messages.ae.drive.total_items", AENumberFormat.formatNumber(totalStored)));
            lore.add(Lang.getString("messages.ae.drive.item_types", aggregatedItems.size()));
            lore.add("");

            int count = 0;
            for (Map.Entry<ItemStack, Long> entry : aggregatedItems.entrySet()) {
                if (count >= 8) {
                    lore.add(Lang.getString("messages.ae.drive.more_items"));
                    break;
                }
                lore.add(Lang.getString("messages.ae.drive.item_entry", ItemStackHelper.getDisplayName(entry.getKey()), AENumberFormat.formatNumber(entry.getValue())));
                count++;
            }

            meta.setLore(lore);
            meta.setDisplayName(Lang.getString("messages.ae.drive.display_name"));
            display.setItemMeta(meta);
        }
        return display;
    }

    @NotNull
    private static ItemStack buildAccessButton() {
        ItemStack accessButton = new ItemStack(Material.CHEST);
        ItemMeta accessMeta = accessButton.getItemMeta();
        if (accessMeta != null) {
            List<String> accessLore = new ArrayList<>();
            accessLore.add(Lang.getString("messages.ae.drive.access_hint"));
            accessMeta.setDisplayName(Lang.getString("messages.ae.drive.browse"));
            accessMeta.setLore(accessLore);
            accessButton.setItemMeta(accessMeta);
        }
        return accessButton;
    }
}