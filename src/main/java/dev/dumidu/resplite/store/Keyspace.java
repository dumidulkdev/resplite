package dev.dumidu.resplite.store;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

public final class Keyspace {
    private final ConcurrentHashMap<String, StoredEntry> data = new ConcurrentHashMap<>();
    private final Clock clock;
    private final AtomicLong memoryEstimate = new AtomicLong();

    public Keyspace(Clock clock) {
        this.clock = clock;
    }

    public Clock clock() {
        return clock;
    }

    public long nowMs() {
        return clock.millis();
    }

    public int size() {
        return data.size();
    }

    public Map<String, StoredEntry> snapshot() {
        return Map.copyOf(data);
    }

    public void clear() {
        data.clear();
        memoryEstimate.set(0);
    }

    public void restore(Map<String, StoredEntry> entries) {
        data.clear();
        data.putAll(entries);
        recalcMemory();
    }

    private void recalcMemory() {
        long sum = 0;
        for (var e : data.entrySet()) {
            sum += estimateEntry(e.getKey(), e.getValue());
        }
        memoryEstimate.set(sum);
    }

    public long memoryEstimateBytes() {
        return memoryEstimate.get();
    }

    private static long estimateEntry(String key, StoredEntry entry) {
        return key.length() * 2L + entry.value().length + 64L;
    }

    public Optional<StoredEntry> getEntry(String key) {
        StoredEntry entry = data.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpiredAt(nowMs())) {
            data.remove(key, entry);
            adjustMemory(key, entry, null);
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    public Optional<byte[]> get(String key) {
        return getEntry(key).map(StoredEntry::value);
    }

    public void put(String key, StoredEntry entry) {
        StoredEntry prev = data.put(key, entry);
        adjustMemory(key, prev, entry);
    }

    public boolean remove(String key) {
        StoredEntry prev = data.remove(key);
        if (prev != null) {
            adjustMemory(key, prev, null);
            return true;
        }
        return false;
    }

    public boolean exists(String key) {
        return getEntry(key).isPresent();
    }

    private void adjustMemory(String key, StoredEntry prev, StoredEntry next) {
        long delta = 0;
        if (prev != null) {
            delta -= estimateEntry(key, prev);
        }
        if (next != null) {
            delta += estimateEntry(key, next);
        }
        if (delta != 0) {
            memoryEstimate.addAndGet(delta);
        }
    }

    public List<String> keysMatching(String pattern) {
        String regex = globToRegex(pattern);
        List<String> keys = new ArrayList<>();
        long now = nowMs();
        data.forEach((k, v) -> {
            if (v.isExpiredAt(now)) {
                data.remove(k, v);
                return;
            }
            if (k.matches(regex)) {
                keys.add(k);
            }
        });
        return keys;
    }

    static String globToRegex(String pattern) {
        StringBuilder sb = new StringBuilder("^");
        for (char c : pattern.toCharArray()) {
            switch (c) {
                case '*' -> sb.append(".*");
                case '?' -> sb.append('.');
                case '.' -> sb.append("\\.");
                case '\\' -> sb.append("\\\\");
                default -> sb.append(c);
            }
        }
        sb.append('$');
        return sb.toString();
    }

    public boolean expireAt(String key, long expireAtEpochMs) {
        StoredEntry current = data.get(key);
        if (current == null || current.isExpiredAt(nowMs())) {
            if (current != null) {
                data.remove(key, current);
            }
            return false;
        }
        StoredEntry updated = current.withExpiry(expireAtEpochMs);
        data.put(key, updated);
        adjustMemory(key, current, updated);
        return true;
    }

    public boolean persist(String key) {
        StoredEntry current = data.get(key);
        if (current == null || current.isExpiredAt(nowMs())) {
            if (current != null) {
                data.remove(key, current);
            }
            return false;
        }
        if (!current.hasExpiry()) {
            return true;
        }
        StoredEntry updated = current.withExpiry(StoredEntry.NO_EXPIRY);
        data.put(key, updated);
        adjustMemory(key, current, updated);
        return true;
    }

    public long ttlSeconds(String key) {
        StoredEntry entry = data.get(key);
        if (entry == null || entry.isExpiredAt(nowMs())) {
            if (entry != null) {
                data.remove(key, entry);
            }
            return -2;
        }
        return ExpirySupport.ttlSeconds(entry, nowMs());
    }

    public long pttlMillis(String key) {
        StoredEntry entry = data.get(key);
        if (entry == null || entry.isExpiredAt(nowMs())) {
            if (entry != null) {
                data.remove(key, entry);
            }
            return -2;
        }
        return ExpirySupport.pttlMillis(entry, nowMs());
    }

    public long increment(String key, long delta) {
        final long now = nowMs();
        final long[] result = new long[1];
        data.compute(key, (k, existing) -> {
            long current = 0L;
            long expireAt = StoredEntry.NO_EXPIRY;
            if (existing != null) {
                if (existing.isExpiredAt(now)) {
                    existing = null;
                } else {
                    Long parsed = dev.dumidu.resplite.command.IntegerCodec.parseLongOrNull(existing.value());
                    if (parsed == null) {
                        throw new IllegalStateException("not integer");
                    }
                    current = parsed;
                    expireAt = existing.expireAtEpochMs();
                }
            }
            long next = current + delta;
            if (!dev.dumidu.resplite.command.IntegerCodec.isValidIncrRange(next)) {
                throw new IllegalStateException("overflow");
            }
            result[0] = next;
            byte[] bytes = Long.toString(next).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            StoredEntry updated = new StoredEntry(bytes, expireAt);
            if (existing == null) {
                memoryEstimate.addAndGet(estimateEntry(k, updated));
            } else {
                memoryEstimate.addAndGet(estimateEntry(k, updated) - estimateEntry(k, existing));
            }
            return updated;
        });
        return result[0];
    }

    public void forEachEntry(BiConsumer<String, StoredEntry> consumer) {
        long now = nowMs();
        data.forEach((k, v) -> {
            if (!v.isExpiredAt(now)) {
                consumer.accept(k, v);
            }
        });
    }
}