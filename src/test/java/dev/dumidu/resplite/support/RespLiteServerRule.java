package dev.dumidu.resplite.support;

import dev.dumidu.resplite.RespLiteServer;
import dev.dumidu.resplite.config.RespLiteConfig;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class RespLiteServerRule implements BeforeEachCallback, AfterEachCallback {
    private RespLiteServer server;
    private int port;
    private Path dataDir;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        dataDir = Files.createTempDirectory("resplite-test");
        port = findFreePort();
        Properties props = new Properties();
        props.setProperty("server.port", Integer.toString(port));
        props.setProperty("data.dir", dataDir.toString());
        props.setProperty("aof.enabled", "false");
        props.setProperty("snapshot.enabled", "false");
        RespLiteConfig cfg = RespLiteConfig.fromProperties(props);
        server = new RespLiteServer(cfg);
        server.start();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (server != null) {
            server.shutdown();
        }
    }

    public int port() {
        return port;
    }

    public RespLiteServer server() {
        return server;
    }

    private static int findFreePort() throws IOException {
        try (var socket = new java.net.ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}