package dev.dumidu.resplite;

import dev.dumidu.resplite.config.RespLiteConfig;

/**
 * Placeholder until server module is wired in subsequent commits.
 */
public class RespLiteServer {
    private final RespLiteConfig config;

    public RespLiteServer(RespLiteConfig config) {
        this.config = config;
    }

    public void start() {
        throw new UnsupportedOperationException("Server bootstrap not yet initialized");
    }

    public void shutdown() {
        // graceful shutdown wired in observability commit
    }

    public RespLiteConfig config() {
        return config;
    }
}