package com.balugaq.netex.core.guide;

import com.balugaq.jeg.api.patches.JEGGuideSettings;
import com.balugaq.netex.api.algorithm.Calculator;
import com.balugaq.netex.utils.Lang;
import io.github.sefiraat.networks.Networks;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.core.guide.options.SlimefunGuideOption;
import io.github.thebusybiscuit.slimefun4.core.guide.options.SlimefunGuideSettings;
import io.github.thebusybiscuit.slimefun4.libraries.dough.chat.ChatInput;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author balugaq
 */
@SuppressWarnings({"UnnecessaryUnicodeEscape", "SameReturnValue"})
public class GridNewStyleCustomAmountGuideOption implements SlimefunGuideOption<Integer> {
    public static final int GRID_NEW_STYLE_MAX_CUSTOM_AMOUNT = Networks.getConfigManager().getInt("grid-new-style-max-custom-amount");
    private static final @NotNull GridNewStyleCustomAmountGuideOption instance = new GridNewStyleCustomAmountGuideOption();

    public static @NotNull GridNewStyleCustomAmountGuideOption instance() {
        return instance;
    }

    public static @NotNull NamespacedKey key0() {
        return new NamespacedKey(Networks.getInstance(), "grid_new_style_custom_amount");
    }

    public static int get(@NotNull Player p) {
        return PersistentDataAPI.getInt(p, key0(), GRID_NEW_STYLE_MAX_CUSTOM_AMOUNT);
    }

    @Override
    public @NotNull SlimefunAddon getAddon() {
        return Networks.getInstance();
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return key0();
    }

    @Override
    public @NotNull Optional<ItemStack> getDisplayItem(@NotNull Player p, ItemStack guide) {
        int value = getSelectedOption(p, guide).orElse(GRID_NEW_STYLE_MAX_CUSTOM_AMOUNT);
        if (value > GRID_NEW_STYLE_MAX_CUSTOM_AMOUNT) {
            value = GRID_NEW_STYLE_MAX_CUSTOM_AMOUNT;
            PersistentDataAPI.setInt(p, key0(), value);
        }

        ItemStack item = new CustomItemStack(
            Material.FURNACE,
            Lang.getString("messages.guide.grid-new-style-custom-amount.display-name"),
            "",
            String.format(
                Lang.getString("messages.guide.grid-new-style-custom-amount.current-amount"),
                value,
                GRID_NEW_STYLE_MAX_CUSTOM_AMOUNT),
            Lang.getString("messages.guide.grid-new-style-custom-amount.click-tip")
        );
        return Optional.of(item);
    }

    @Override
    public void onClick(@NotNull Player p, @NotNull ItemStack guide) {
        p.closeInventory();
        p.sendMessage(Lang.getString("messages.guide.grid-new-style-custom-amount.input-prompt"));
        ChatInput.waitForPlayer(Networks.getInstance(), p, s -> {
            try {
                int value = Calculator.calculate(s).intValue();
                if (value < 1 || value > GRID_NEW_STYLE_MAX_CUSTOM_AMOUNT) {
                    p.sendMessage(String.format(
                        Lang.getString("messages.guide.invalid-amount-input"), GRID_NEW_STYLE_MAX_CUSTOM_AMOUNT));
                    return;
                }

                setSelectedOption(p, guide, value);
                try {
                    JEGGuideSettings.openSettings(p, guide);
                } catch (Exception ignored) {
                    SlimefunGuideSettings.openSettings(p, guide);
                }
            } catch (NumberFormatException e) {
                p.sendMessage(String.format(
                        Lang.getString("messages.guide.invalid-amount-input"), GRID_NEW_STYLE_MAX_CUSTOM_AMOUNT)
                    + e.getMessage());
            }
        });
    }

    @Override
    public @NotNull Optional<Integer> getSelectedOption(@NotNull Player p, ItemStack guide) {
        return Optional.of(get(p));
    }

    @Override
    public void setSelectedOption(@NotNull Player p, ItemStack guide, Integer value) {
        PersistentDataAPI.setInt(p, getKey(), value);
    }
}
