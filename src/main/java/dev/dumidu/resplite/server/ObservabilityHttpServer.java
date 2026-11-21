package dev.dumidu.resplite.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.dumidu.resplite.config.RespLiteConfig;
import dev.dumidu.resplite.store.Keyspace;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class ObservabilityHttpServer implements AutoCloseable {
    private final HttpServer server;

    public ObservabilityHttpServer(RespLiteConfig config, Keyspace keyspace, ServerMetrics metrics) throws IOException {
        server = HttpServer.create(new InetSocketAddress(config.bindHost(), config.httpPort()), 0);
        server.createContext("/health", exchange -> respond(exchange, 200, "{\"status\":\"ok\"}\n"));
        server.createContext("/metrics", exchange -> {
            String body = "{\n"
                    + "  \"keys\": " + keyspace.size() + ",\n"
                    + "  \"connected_clients\": " + metrics.connectedClients() + ",\n"
                    + "  \"commands_processed\": " + metrics.commandsProcessed() + ",\n"
                    + "  \"ops_per_sec\": " + String.format("%.2f", metrics.opsPerSecond()) + "\n"
                    + "}\n";
            respond(exchange, 200, body);
        });
        server.start();
    }

    private static void respond(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }

    public int port() {
        return server.getAddress().getPort();
    }
}