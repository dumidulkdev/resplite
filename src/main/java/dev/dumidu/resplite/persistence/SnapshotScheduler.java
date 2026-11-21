package dev.dumidu.resplite.persistence;

import dev.dumidu.resplite.store.Keyspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SnapshotScheduler implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(SnapshotScheduler.class);

    private final Keyspace keyspace;
    private final Path snapshotPath;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean saving = new AtomicBoolean(false);

    public SnapshotScheduler(Keyspace keyspace, Path snapshotPath, int intervalSeconds) {
        this.keyspace = keyspace;
        this.snapshotPath = snapshotPath;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "snapshot-scheduler"));
        if (intervalSeconds > 0) {
            scheduler.scheduleAtFixedRate(() -> save(false), intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        }
    }

    public boolean save(boolean blocking) {
        if (!saving.compareAndSet(false, true)) {
            return false;
        }
        Runnable task = () -> {
            try {
                SnapshotFormat.write(snapshotPath, keyspace);
                log.info("Snapshot saved to {}", snapshotPath);
            } catch (Exception e) {
                log.warn("Snapshot failed: {}", e.getMessage());
            } finally {
                saving.set(false);
            }
        };
        if (blocking) {
            task.run();
            return true;
        }
        scheduler.execute(task);
        return true;
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}