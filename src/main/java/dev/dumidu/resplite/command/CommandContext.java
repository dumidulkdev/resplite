package dev.dumidu.resplite.command;

import dev.dumidu.resplite.server.ServerMetrics;
import dev.dumidu.resplite.store.Keyspace;

import java.util.List;
import java.util.function.Consumer;

public record CommandContext(
        Keyspace keyspace,
        ServerMetrics metrics,
        Consumer<List<String>> writeListener
) {
    public void onWrite(List<String> command) {
        if (writeListener != null) {
            writeListener.accept(command);
        }
    }
}