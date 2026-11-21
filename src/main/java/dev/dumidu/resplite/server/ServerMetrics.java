package dev.dumidu.resplite.server;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class ServerMetrics {
    private final AtomicInteger connectedClients = new AtomicInteger();
    private final AtomicLong commandsProcessed = new AtomicLong();
    private final AtomicLong totalNanos = new AtomicLong();
    private final long startedAtMs = System.currentTimeMillis();

    public void clientConnected() {
        connectedClients.incrementAndGet();
    }

    public void clientDisconnected() {
        connectedClients.decrementAndGet();
    }

    public int connectedClients() {
        return connectedClients.get();
    }

    public long commandsProcessed() {
        return commandsProcessed.get();
    }

    public long startedAtMs() {
        return startedAtMs;
    }

    public void recordCommand(long nanos) {
        commandsProcessed.incrementAndGet();
        totalNanos.addAndGet(nanos);
    }

    public double opsPerSecond() {
        long elapsedMs = Math.max(1, System.currentTimeMillis() - startedAtMs);
        return commandsProcessed.get() * 1000.0 / elapsedMs;
    }
}