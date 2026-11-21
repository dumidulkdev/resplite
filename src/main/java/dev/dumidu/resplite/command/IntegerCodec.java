package dev.dumidu.resplite.command;

import java.nio.charset.StandardCharsets;

public final class IntegerCodec {
    private IntegerCodec() {
    }

    public static Long parseLongOrNull(byte[] value) {
        if (value == null || value.length == 0) {
            return null;
        }
        String s = new String(value, StandardCharsets.UTF_8).trim();
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isValidIncrRange(long v) {
        return v >= Long.MIN_VALUE && v <= Long.MAX_VALUE;
    }
}