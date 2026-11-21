package dev.dumidu.resplite.command;

import dev.dumidu.resplite.server.PersistenceContext;
import dev.dumidu.resplite.server.ServerMetrics;
import dev.dumidu.resplite.store.Keyspace;

import java.util.List;
import java.util.function.Consumer;

public record CommandContext(
        Keyspace keyspace,
        ServerMetrics metrics,
        Consumer<List<String>> writeListener,
        PersistenceContext persistence
) {
    public CommandContext(Keyspace keyspace, ServerMetrics metrics, Consumer<List<String>> writeListener) {
        this(keyspace, metrics, writeListener, null);
    }
    public void onWrite(List<String> command) {
        if (writeListener != null) {
            writeListener.accept(command);
        }
    }
}