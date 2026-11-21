package dev.dumidu.resplite;

import dev.dumidu.resplite.config.RespLiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RespLiteMain {
    private static final Logger log = LoggerFactory.getLogger(RespLiteMain.class);

    private RespLiteMain() {
    }

    public static void main(String[] args) throws Exception {
        RespLiteConfig config = RespLiteConfig.load(args);
        log.info("RespLite starting on port {}", config.serverPort());
        RespLiteServer server = new RespLiteServer(config);
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown, "resplite-shutdown"));
        server.start();
    }
}