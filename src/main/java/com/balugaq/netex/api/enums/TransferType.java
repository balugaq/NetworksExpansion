package com.balugaq.netex.api.enums;

import com.balugaq.netex.api.gui.GUI;
import com.balugaq.netex.utils.GUIs;
import io.github.sefiraat.networks.Networks;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum TransferType {
    ADVANCED_LINE_TRANSFER(GUIs.ALT),
    ADVANCED_LINE_TRANSFER_BEST_PUSHER(GUIs.ALTBP),
    ADVANCED_LINE_TRANSFER_GRABBER(GUIs.ALTG),
    ADVANCED_LINE_TRANSFER_MORE_PUSHER(GUIs.ALTMP),
    ADVANCED_LINE_TRANSFER_PUSHER(GUIs.ALTP),
    LINE_TRANSFER(GUIs.LT),
    LINE_TRANSFER_BEST_PUSHER(GUIs.LTBP),
    LINE_TRANSFER_GRABBER(GUIs.LTG),
    LINE_TRANSFER_MORE_PUSHER(GUIs.LTMP),
    LINE_TRANSFER_PUSHER(GUIs.LTP),
    ADVANCED_TRANSFER(GUIs.AT),
    ADVANCED_TRANSFER_BEST_PUSHER(GUIs.ATBP),
    ADVANCED_TRANSFER_GRABBER(GUIs.ATG),
    ADVANCED_TRANSFER_MORE_PUSHER(GUIs.ATMP),
    ADVANCED_TRANSFER_PUSHER(GUIs.ATP),
    TRANSFER(GUIs.T),
    TRANSFER_BEST_PUSHER(GUIs.TBP),
    TRANSFER_GRABBER(GUIs.TG),
    TRANSFER_MORE_PUSHER(GUIs.TMP),
    TRANSFER_PUSHER(GUIs.TP),
    LINE_TRANSFER_VANILLA_GRABBER(GUIs.LTVG),
    LINE_TRANSFER_VANILLA_PUSHER(GUIs.LTVP),
    WHITELISTED_TRANSFER_GRABBER(GUIs.WTG),
    WHITELISTED_LINE_TRANSFER_GRABBER(GUIs.WLTG),
    WHITELISTED_TRANSFER_VANILLA_GRABBER(GUIs.WTVG),
    WHITELISTED_LINE_TRANSFER_VANILLA_GRABBER(GUIs.WLTVG),
    ADVANCED_LINE_TRANSFER_VANILLA_GRABBER(GUIs.ALTVG),
    ADVANCED_LINE_TRANSFER_VANILLA_PUSHER(GUIs.ALTVP),
    CHAIN_GRABBER(GUIs.CG),
    CHAIN_PUSHER(GUIs.CP),
    CHAIN_TRANSCEIVER(GUIs.CGP),
    ;

    private final @NotNull GUI gui;
    private final Map<String, Integer> config = new HashMap<>();

    TransferType(@NotNull GUI gui) {
        this.gui = gui;
    }

    public int config(@NotNull String key) {
        return config(key, 0);
    }

    public int config(@NotNull String key, int defaultValue) {
        if (config.containsKey(key)) {
            return config.get(key);
        }

        int v = Networks.getConfigManager().getInt("items.NTW_EXPANSION_" + name() + "." + key, defaultValue);
        config.put(key, v);
        return v;
    }

    public int config(@Nullable String id, @NotNull String key, int defaultValue) {
        if (id == null) {
            return config(key, defaultValue);
        } else {
            return fixedConfig(id, key, defaultValue);
        }
    }

    public int fixedConfig(@NotNull String id, @NotNull String key, int defaultValue) {
        String fkey = id + "." + key;
        if (config.containsKey(fkey)) {
            return config.get(fkey);
        }

        int v = Networks.getConfigManager().getInt("items." + fkey, defaultValue);
        config.put(fkey, v);
        return v;
    }

    public boolean isTransfer() {
        return name().endsWith("TRANSFER");
    }

    public boolean isGrabber() {
        return isTransfer() || name().endsWith("GRABBER");
    }

    public boolean isPusher() {
        return isTransfer() || name().endsWith("PUSHER");
    }

    public boolean isVanilla() {
        return name().contains("VANILLA");
    }

    public boolean isAdvanced() {
        return name().contains("ADVANCED");
    }

    public boolean isWhitelisted() {
        return name().startsWith("WHITELISTED");
    }
}
