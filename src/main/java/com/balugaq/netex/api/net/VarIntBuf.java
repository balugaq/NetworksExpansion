package com.balugaq.netex.api.net;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class VarIntBuf {
    private final ByteArrayOutputStream out;

    public VarIntBuf() {
        this(32);
    }

    public VarIntBuf(int estimatedSize) {
        this.out = new ByteArrayOutputStream(estimatedSize);
    }

    public VarIntBuf u8(int value) {
        out.write(value & 0xFF);
        return this;
    }

    public VarIntBuf varInt(int value) {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
        return this;
    }

    public VarIntBuf varLong(long value) {
        while ((value & ~0x7FL) != 0) {
            out.write((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        out.write((int) value);
        return this;
    }

    public VarIntBuf i64(long value) {
        for (int shift = 56; shift >= 0; shift -= 8) {
            out.write((int) (value >>> shift) & 0xFF);
        }
        return this;
    }

    public VarIntBuf utf(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        varInt(bytes.length);
        out.write(bytes, 0, bytes.length);
        return this;
    }

    public byte[] bytes() {
        return out.toByteArray();
    }
}
