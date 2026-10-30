package com.ytdd9527.networksexpansion.api.assembly;

import com.ytdd9527.networksexpansion.utils.ItemKey;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record CachedSnapshot(long expiresAtMs, @NotNull Map<ItemKey, Long> keyed) {
}
