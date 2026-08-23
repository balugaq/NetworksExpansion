package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.util;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;

public final class AESerializeUtils {

    private AESerializeUtils() {
    }

    @NotNull
    public static String object2String(@NotNull ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    @Nullable
    public static ItemStack string2Object(@NotNull String base64) {
        try {
            ItemStack item = ItemStack.deserializeBytes(Base64.getDecoder().decode(base64));
            if (item == null || item.getType().isAir()) {
                return null;
            }
            return item;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public static String getId(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "VANILLA_AIR";
        }
        SlimefunItem sfItem = SlimefunItem.getByItem(item);
        if (sfItem != null) {
            return "SLIMEFUN_" + sfItem.getId();
        }
        Material material = item.getType();
        if (new ItemStack(material).equals(item)) {
            return "VANILLA_" + material.name();
        }
        return null;
    }
}