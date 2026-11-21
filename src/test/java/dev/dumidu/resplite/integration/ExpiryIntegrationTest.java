package dev.dumidu.resplite.integration;

import dev.dumidu.resplite.RespLiteServer;
import dev.dumidu.resplite.config.RespLiteConfig;
import dev.dumidu.resplite.support.MutableClock;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ExpiryIntegrationTest {
    private MutableClock clock;
    private RespLiteServer server;
    private int port;

    @BeforeEach
    void start() throws Exception {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));
        port = findFreePort();
        Properties props = new Properties();
        props.setProperty("server.port", Integer.toString(port));
        props.setProperty("data.dir", Files.createTempDirectory("exp-test").toString());
        props.setProperty("expiry.active.interval.ms", "50");
        props.setProperty("expiry.sample.count", "50");
        RespLiteConfig cfg = RespLiteConfig.fromProperties(props);
        server = new RespLiteServer(cfg, clock);
        server.start();
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    void lazyExpiryOnGet() {
        try (Jedis j = jedis()) {
            j.set("temp", "v", SetParams.setParams().ex(10));
            clock.advanceMillis(10_000);
            assertThat(j.get("temp")).isNull();
        }
    }

    @Test
    void ttlAndPersist() {
        try (Jedis j = jedis()) {
            j.set("k", "v");
            j.expire("k", 60);
            assertThat(j.ttl("k")).isBetween(55L, 60L);
            j.persist("k");
            assertThat(j.ttl("k")).isEqualTo(-1L);
        }
    }

    @Test
    void activeExpiryRemovesKey() {
        try (Jedis j = jedis()) {
            j.set("active", "x", SetParams.setParams().px(100));
            clock.advanceMillis(200);
            Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> j.get("active") == null);
        }
    }

    private Jedis jedis() {
        return new Jedis("127.0.0.1", port);
    }

    private static int findFreePort() throws Exception {
        try (var s = new java.net.ServerSocket(0)) {
            s.setReuseAddress(true);
            return s.getLocalPort();
        }
    }
}