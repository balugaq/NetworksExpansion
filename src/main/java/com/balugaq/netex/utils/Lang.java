package com.balugaq.netex.utils;

import com.ytdd9527.networksexpansion.core.services.LocalizationService;
import io.github.sefiraat.networks.Networks;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.text.MessageFormat;
import java.util.List;

public class Lang {
    public static LocalizationService get() {
        return Networks.getLocalizationService();
    }

    @NotNull
    public static ItemStack getMechanism(@NotNull String key) {
        return get().getMechanism(key);
    }

    @NotNull
    public static SlimefunItemStack getItem(@NotNull String key, @NotNull Material material) {
        return get().getItem(key, material);
    }

    @NotNull
    public static SlimefunItemStack getItem(@NotNull String id, @NotNull Material material, Object... args) {
        SlimefunItemStack stack = get().getItem(id, material);
        if (args.length > 0) {
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(MessageFormat.format(meta.getDisplayName(), args));
            List<String> lore = meta.getLore();
            if (lore != null) {
                lore.replaceAll(line -> MessageFormat.format(line, args));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @NotNull
    @ParametersAreNonnullByDefault
    public static SlimefunItemStack getItem(String id, String texture, String... extraLore) {
        return get().getItem(id, texture, extraLore);
    }

    @NotNull
    @ParametersAreNonnullByDefault
    public static SlimefunItemStack getItem(String id, ItemStack itemStack, String... extraLore) {
        return get().getItem(id, itemStack, extraLore);
    }

    @NotNull
    public static ItemStack getIcon(@NotNull String key, @NotNull Material material) {
        return get().getIcon(key, material);
    }

    @NotNull
    public static String getString(@NotNull String key) {
        return get().getString(key);
    }

    @ParametersAreNonnullByDefault
    @NotNull
    public static String getString(String key, Object... args) {
        return get().getString(key, args);
    }

    @NotNull
    public static List<String> getStringList(@NotNull String key) {
        return get().getStringList(key);
    }

    @NotNull
    public static String @NotNull [] getStringArray(@NotNull String key) {
        return get().getStringArray(key);
    }
}
