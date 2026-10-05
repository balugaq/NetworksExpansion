package com.ytdd9527.networksexpansion.implementation.machines.cellnet.storage.journal;

import org.jetbrains.annotations.Nullable;

public enum JournalOp {
    PUT('P'),
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
