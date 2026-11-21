package dev.dumidu.resplite.store;

public final class ExpirySupport {
    private ExpirySupport() {
    }

    public static long ttlSeconds(StoredEntry entry, long nowMs) {
        if (!entry.hasExpiry()) {
            return -1;
        }
        long remainingMs = entry.expireAtEpochMs() - nowMs;
        if (remainingMs <= 0) {
            return -2;
        }
        return (remainingMs + 999) / 1000;
    }

    public static long pttlMillis(StoredEntry entry, long nowMs) {
        if (!entry.hasExpiry()) {
            return -1;
        }
        long remainingMs = entry.expireAtEpochMs() - nowMs;
        if (remainingMs <= 0) {
            return -2;
        }
        return remainingMs;
    }
}