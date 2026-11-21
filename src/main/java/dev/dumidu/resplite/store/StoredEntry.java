package dev.dumidu.resplite.store;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record StoredEntry(byte[] value, long expireAtEpochMs) {
    public static final long NO_EXPIRY = 0L;

    public StoredEntry {
        value = value == null ? new byte[0] : value.clone();
    }

    public boolean hasExpiry() {
        return expireAtEpochMs > 0;
    }

    public boolean isExpiredAt(long nowMs) {
        return hasExpiry() && nowMs >= expireAtEpochMs;
    }

    public String asUtf8() {
        return new String(value, StandardCharsets.UTF_8);
    }

    public StoredEntry withExpiry(long expireAtEpochMs) {
        return new StoredEntry(value, expireAtEpochMs);
    }

    public StoredEntry withValue(byte[] newValue) {
        return new StoredEntry(newValue, expireAtEpochMs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StoredEntry(byte[] v, long e))) return false;
        return expireAtEpochMs == e && Arrays.equals(value, v);
    }

    @Override
    public int hashCode() {
        int r = Arrays.hashCode(value);
        return 31 * r + Long.hashCode(expireAtEpochMs);
    }
}