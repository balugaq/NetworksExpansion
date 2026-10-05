package com.balugaq.netex.integrations.logitech;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 量子纠缠的绑定目标。AUTO 沿 量子储存 → 元件 → 抽屉 依次回退；
 * 其余取值只扫描对应存储，目标里没有该物品时绑定失败。
 */
public enum LinkerTarget {
    AUTO,
    QUANTUM_STORAGE,
    CELL,
    DRAWER;

    @NotNull
    public static LinkerTarget of(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            return AUTO;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return AUTO;
        }
    }

    @NotNull
    public LinkerTarget next() {
        LinkerTarget[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
