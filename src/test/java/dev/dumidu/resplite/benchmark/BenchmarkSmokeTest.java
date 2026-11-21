package dev.dumidu.resplite.benchmark;

import dev.dumidu.resplite.RespLiteServer;
import dev.dumidu.resplite.config.RespLiteConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkSmokeTest {

    @Test
    void benchmarkRuns() throws Exception {
        int port;
        try (var s = new java.net.ServerSocket(0)) {
            s.setReuseAddress(true);
            port = s.getLocalPort();
        }
        Properties p = new Properties();
        p.setProperty("server.port", Integer.toString(port));
        p.setProperty("data.dir", Files.createTempDirectory("bench").toString());
        p.setProperty("aof.enabled", "false");
        RespLiteServer server = new RespLiteServer(RespLiteConfig.fromProperties(p));
        server.start();
        RespLiteBenchmark.Result r = RespLiteBenchmark.run("127.0.0.1", port, 2, 50);
        assertThat(r.opsPerSec()).isGreaterThan(100);
        server.shutdown();
    }
}