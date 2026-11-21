package dev.dumidu.resplite.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class RespLiteConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultsFromProperties() throws IOException {
        Properties p = new Properties();
        p.setProperty("server.port", "6380");
        p.setProperty("data.dir", tempDir.toString());
        RespLiteConfig cfg = RespLiteConfig.fromProperties(p);
        assertThat(cfg.serverPort()).isEqualTo(6380);
        assertThat(cfg.bindHost()).isEqualTo("0.0.0.0");
        assertThat(cfg.aofFsync()).isEqualTo(AofFsyncPolicy.EVERYSEC);
        assertThat(cfg.dataDir()).isEqualTo(tempDir.toAbsolutePath().normalize());
    }

    @Test
    void cliArgsOverridePort() {
        Properties p = new Properties();
        p.setProperty("data.dir", tempDir.toString());
        RespLiteConfig.applyArgs(p, new String[]{"--port", "6399"});
        assertThat(p.getProperty("server.port")).isEqualTo("6399");
    }

    @Test
    void aofFsyncParsing() {
        assertThat(AofFsyncPolicy.fromString("always")).isEqualTo(AofFsyncPolicy.ALWAYS);
        assertThat(AofFsyncPolicy.fromString("EVERYSEC")).isEqualTo(AofFsyncPolicy.EVERYSEC);
        assertThat(AofFsyncPolicy.fromString("no")).isEqualTo(AofFsyncPolicy.NO);
    }

    @Test
    void loadResolvesPaths() throws IOException {
        RespLiteConfig cfg = RespLiteConfig.load(new String[]{"--port", "7001", "--data-dir", tempDir.toString()});
        assertThat(cfg.serverPort()).isEqualTo(7001);
        assertThat(cfg.aofPath().getParent()).isEqualTo(tempDir.toAbsolutePath().normalize());
    }
}