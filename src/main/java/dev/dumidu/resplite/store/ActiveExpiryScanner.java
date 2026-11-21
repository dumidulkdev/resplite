package dev.dumidu.resplite.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ActiveExpiryScanner implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ActiveExpiryScanner.class);

    private final Keyspace keyspace;
    private final int sampleCount;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public ActiveExpiryScanner(Keyspace keyspace, long intervalMs, int sampleCount) {
        this.keyspace = keyspace;
        this.sampleCount = sampleCount;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "resplite-expiry");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::sampleExpire, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    void sampleExpire() {
        if (!running.get()) {
            return;
        }
        Map<String, StoredEntry> snap = keyspace.snapshot();
        if (snap.isEmpty()) {
            return;
        }
        List<String> keys = new ArrayList<>(snap.keySet());
        int n = Math.min(sampleCount, keys.size());
        long now = keyspace.nowMs();
        int removed = 0;
        for (int i = 0; i < n; i++) {
            int idx = ThreadLocalRandom.current().nextInt(keys.size());
            String key = keys.get(idx);
            StoredEntry entry = snap.get(key);
            if (entry != null && entry.isExpiredAt(now)) {
                if (keyspace.remove(key)) {
                    removed++;
                }
            }
        }
        if (removed > 0) {
            log.debug("Active expiry removed {} keys", removed);
        }
    }

    @Override
    public void close() {
        running.set(false);
        scheduler.shutdownNow();
    }
}