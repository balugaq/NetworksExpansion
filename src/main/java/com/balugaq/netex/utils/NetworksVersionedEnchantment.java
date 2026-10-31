package com.balugaq.netex.utils;

import com.balugaq.netex.api.enums.MinecraftVersion;
import io.github.sefiraat.networks.Networks;
import lombok.experimental.UtilityClass;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

@UtilityClass
public class NetworksVersionedEnchantment {
    public static final @NotNull Enchantment GLOW;
    public static final @NotNull Enchantment LUCK_OF_THE_SEA;

    static {
        MinecraftVersion version = Networks.getInstance().getMinecraftVersion();
        GLOW = version.isAtLeast(MinecraftVersion.V1_20_5) ? Enchantment.POWER : getKey("ARROW_DAMAGE");
        LUCK_OF_THE_SEA = version.isAtLeast(MinecraftVersion.V1_20_5) ? Enchantment.LUCK_OF_THE_SEA : getKey("LUCK");
    }

    @NotNull
    private static Enchantment getKey(@NotNull String key) {
        try {
            Field field = Enchantment.class.getDeclaredField(key);
            return (Enchantment) field.get(null);
        } catch (Exception e) {
            // Shouldn't be null
            Debug.trace(e);
            //noinspection DataFlowIssue
            return null;
        }
    }
}
