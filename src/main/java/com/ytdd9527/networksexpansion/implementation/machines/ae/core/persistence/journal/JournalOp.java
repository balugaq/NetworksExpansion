package com.ytdd9527.networksexpansion.implementation.machines.ae.core.persistence.journal;

import org.jetbrains.annotations.Nullable;

// journal 操作类型，替代 'P'/'R' 魔法字符
public enum JournalOp {
    /** 写入/更新某物品数量（可为 0，表示写后清空该条目）。 */
    PUT('P'),
    /** 移除某物品条目。 */
    REMOVE('R');

    private final char code;

    JournalOp(char code) {
        this.code = code;
    }

    public char code() {
        return code;
    }

    @Nullable
    public static JournalOp fromCode(char code) {
        for (JournalOp op : values()) {
            if (op.code == code) {
                return op;
            }
        }
        return null;
    }
}
