package com.ytdd9527.networksexpansion.implementation.machines.ae.constants;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 元件容量等级，与量子存储的 {@code SIZES} 对齐，升级材料表以本枚举为 key.
 */
public enum AEStorageCellType {

    LEVEL_0(64L),
    LEVEL_1(256L),
    LEVEL_2(1024L),
    LEVEL_3(4096L),
    LEVEL_4(32768L),
    LEVEL_5(262144L),
    LEVEL_6(2097152L),
    LEVEL_7(16777216L),
    LEVEL_8(134217728L),
    LEVEL_9(1073741824L),
    LEVEL_10(2147483647L),
    LEVEL_11(34359738352L),
    LEVEL_12(549755813888L),
    LEVEL_13(8796093022208L),
    LEVEL_14(140737488355328L);

    @Getter
    private final long maxAmount;

    AEStorageCellType(long maxAmount) {
        this.maxAmount = maxAmount;
    }

    @Nullable
    public static AEStorageCellType fromAmount(long maxAmount) {
        for (AEStorageCellType type : values()) {
            if (type.maxAmount == maxAmount) {
                return type;
            }
        }
        return null;
    }
}
