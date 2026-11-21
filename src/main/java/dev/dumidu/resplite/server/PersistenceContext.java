package dev.dumidu.resplite.server;

import dev.dumidu.resplite.persistence.AofWriter;
import dev.dumidu.resplite.persistence.SnapshotScheduler;

public final class PersistenceContext {
    private final AofWriter aofWriter;
    private final SnapshotScheduler snapshotScheduler;

    public PersistenceContext(AofWriter aofWriter, SnapshotScheduler snapshotScheduler) {
        this.aofWriter = aofWriter;
        this.snapshotScheduler = snapshotScheduler;
    }

    public AofWriter aofWriter() {
        return aofWriter;
    }

    public SnapshotScheduler snapshotScheduler() {
        return snapshotScheduler;
    }
}