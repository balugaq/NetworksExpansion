package com.ytdd9527.networksexpansion.implementation.machines.ae.utils;

import org.jetbrains.annotations.NotNull;

public final class AENumberFormat {

    private static final long K = 1_000L;
    private static final long M = 1_000_000L;
    private static final long B = 1_000_000_000L;
    private static final long T = 1_000_000_000_000L;

    private AENumberFormat() {
    }

    @NotNull
    public static String formatNumber(long value) {
        if (value >= T) {
            return fixed(value, T, 2) + "T";
        }
        if (value >= B) {
            return fixed(value, B, 2) + "B";
        }
        if (value >= M) {
            return fixed(value, M, 2) + "M";
        }
        if (value >= K) {
            return fixed(value, K, 2) + "K";
        }
        return Long.toString(value);
    }

    @NotNull
    public static String formatCellShort(long value) {
        if (value >= T) {
            return fixed(value, T, 1) + "T";
        }
        if (value >= B) {
            return fixed(value, B, 1) + "B";
        }
        if (value >= M) {
            return fixed(value, M, 0) + "M";
        }
        if (value >= 10_000L) {
            return fixed(value, K, 0) + "K";
        }
        if (value >= K) {
            return fixed(value, K, 1) + "K";
        }
        return Long.toString(value);
    }

    @NotNull
    public static String formatCellNumber(long value) {
        if (value >= T) {
            return fixed(value, T, 1) + "T";
        }
        if (value >= B) {
            return fixed(value, B, 1) + "B";
        }
        if (value >= M) {
            return fixed(value, M, 0) + "M";
        }
        if (value >= K) {
            return grouped(value);
        }
        return Long.toString(value);
    }

    @NotNull
    private static String fixed(long value, long divisor, int decimals) {
        long whole = value / divisor;
        long rem = value % divisor;
        if (decimals == 0) {
            return Long.toString(whole + ((rem * 2 >= divisor) ? 1L : 0L));
        }
        long scale = decimals == 1 ? 10L : 100L;
        long scaled = (rem * scale * 2 + divisor) / (divisor * 2);
        if (scaled == scale) {
            whole++;
            scaled = 0;
        }
        String frac = Long.toString(scaled);
        if (decimals == 2 && frac.length() == 1) {
            return whole + ".0" + frac;
        }
        return whole + "." + frac;
    }

    @NotNull
    private static String grouped(long value) {
        String s = Long.toString(value);
        int first = s.length() % 3;
        StringBuilder sb = new StringBuilder(s.length() + s.length() / 3);
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (i - first) % 3 == 0) {
                sb.append(',');
            }
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }
}