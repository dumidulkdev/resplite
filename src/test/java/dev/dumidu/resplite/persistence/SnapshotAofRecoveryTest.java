package dev.dumidu.resplite.persistence;

import dev.dumidu.resplite.RespLiteServer;
import dev.dumidu.resplite.config.RespLiteConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import redis.clients.jedis.Jedis;

import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotAofRecoveryTest {

    @TempDir
    Path tempDir;

    @Test
    void snapshotPlusAofTail() throws Exception {
        int port = freePort();
        Properties props = props(port, tempDir);
        RespLiteServer server = new RespLiteServer(RespLiteConfig.fromProperties(props));
        server.start();
        try (Jedis j = new Jedis("127.0.0.1", port)) {
            j.set("fromAof", "1");
        }
        server.dispatcher().context().persistence().snapshotScheduler().save(true);
        try (Jedis j = new Jedis("127.0.0.1", port)) {
            j.set("afterSnap", "2");
        }
        server.shutdown();

        RespLiteServer server2 = new RespLiteServer(RespLiteConfig.fromProperties(props));
        server2.start();
        try (Jedis j = new Jedis("127.0.0.1", port)) {
            assertThat(j.get("fromAof")).isEqualTo("1");
            assertThat(j.get("afterSnap")).isEqualTo("2");
        }
        server2.shutdown();
    }

    private static Properties props(int port, Path dataDir) {
        Properties p = new Properties();
        p.setProperty("server.port", Integer.toString(port));
        p.setProperty("data.dir", dataDir.toString());
        p.setProperty("aof.enabled", "true");
        p.setProperty("snapshot.enabled", "true");
        p.setProperty("aof.fsync", "always");
        p.setProperty("snapshot.interval.seconds", "3600");
        return p;
    }

    private static int freePort() throws Exception {
        try (var s = new java.net.ServerSocket(0)) {
            s.setReuseAddress(true);
            return s.getLocalPort();
        }
    }
}