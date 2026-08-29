package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.util;

import com.balugaq.netex.utils.Debug;
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
        } catch (RuntimeException e) {
            Debug.trace(e, "反序列化物品 base64 失败");
            return null;
        }
    }
}