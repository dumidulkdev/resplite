package dev.dumidu.resplite.integration;

import dev.dumidu.resplite.RespLiteServer;
import dev.dumidu.resplite.config.RespLiteConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dev.dumidu.resplite.protocol.RespValue;
import redis.clients.jedis.Jedis;

import java.util.List;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityIntegrationTest {
    private RespLiteServer server;
    private int port;
    private int httpPort;

    @BeforeEach
    void start() throws Exception {
        port = freePort();
        httpPort = freePort();
        Properties p = new Properties();
        p.setProperty("server.port", Integer.toString(port));
        p.setProperty("http.port", Integer.toString(httpPort));
        p.setProperty("data.dir", Files.createTempDirectory("obs").toString());
        p.setProperty("aof.enabled", "false");
        server = new RespLiteServer(RespLiteConfig.fromProperties(p));
        server.start();
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    void infoAndHttpEndpoints() throws Exception {
        try (Jedis j = new Jedis("127.0.0.1", port)) {
            j.set("k", "v");
            RespValue infoReply = server.dispatcher().dispatch(List.of("INFO"));
            String info = ((RespValue.BulkString) infoReply).asUtf8();
            assertThat(info).contains("connected_clients");
            assertThat(info).contains("db0:keys=");
        }
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> health = client.send(HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + httpPort + "/health"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(health.body()).contains("ok");
        HttpResponse<String> metrics = client.send(HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + httpPort + "/metrics"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(metrics.body()).contains("commands_processed");
    }

    private static int freePort() throws Exception {
        try (var s = new java.net.ServerSocket(0)) {
            s.setReuseAddress(true);
            return s.getLocalPort();
        }
    }
}