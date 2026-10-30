package com.ytdd9527.networksexpansion.api.assembly;

import com.ytdd9527.networksexpansion.utils.ItemKey;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

record SharedSnapshot(
    @NotNull Map<ItemKey, Long> keyed,
    long expiresAtMs,
    long lastAccessAtMs) {
}
