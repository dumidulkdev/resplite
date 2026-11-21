package dev.dumidu.resplite.persistence;

import dev.dumidu.resplite.RespLiteServer;
import dev.dumidu.resplite.config.RespLiteConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class AofPersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    void restartRecoversData() throws Exception {
        int port = freePort();
        Path dataDir = tempDir.resolve("data1");
        Properties props = baseProps(port, dataDir);
        RespLiteServer server = new RespLiteServer(RespLiteConfig.fromProperties(props));
        server.start();
        try (Jedis j = new Jedis("127.0.0.1", port)) {
            j.set("persisted", "value");
            j.set("ttlkey", "x", SetParams.setParams().ex(3600));
        }
        server.shutdown();

        props = baseProps(port + 1, dataDir);
        RespLiteServer server2 = new RespLiteServer(RespLiteConfig.fromProperties(props));
        server2.start();
        try (Jedis j = new Jedis("127.0.0.1", port + 1)) {
            assertThat(j.get("persisted")).isEqualTo("value");
            assertThat(j.get("ttlkey")).isEqualTo("x");
            assertThat(j.ttl("ttlkey")).isGreaterThan(0);
        }
        server2.shutdown();
    }

    @Test
    void truncatedTailIsTolerated() throws Exception {
        Path dataDir = tempDir.resolve("data2");
        Files.createDirectories(dataDir);
        Path aof = dataDir.resolve("appendonly.aof");
        Files.writeString(aof, "*3\r\n$3\r\nSET\r\n$1\r\nk\r\n$1\r\nv\r\n*2\r\n$3\r\n");
        int port = freePort();
        Properties props = baseProps(port, dataDir);
        RespLiteServer server = new RespLiteServer(RespLiteConfig.fromProperties(props));
        server.start();
        try (Jedis j = new Jedis("127.0.0.1", port)) {
            assertThat(j.get("k")).isEqualTo("v");
        }
        server.shutdown();
    }

    private static Properties baseProps(int port, Path dataDir) {
        Properties p = new Properties();
        p.setProperty("server.port", Integer.toString(port));
        p.setProperty("data.dir", dataDir.toString());
        p.setProperty("aof.enabled", "true");
        p.setProperty("snapshot.enabled", "false");
        p.setProperty("aof.fsync", "always");
        return p;
    }

    private static int freePort() throws Exception {
        try (var s = new java.net.ServerSocket(0)) {
            s.setReuseAddress(true);
            return s.getLocalPort();
        }
    }
}