package dev.dumidu.resplite.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record RespLiteConfig(
        String bindHost,
        int serverPort,
        int httpPort,
        Path dataDir,
        boolean aofEnabled,
        String aofFilename,
        AofFsyncPolicy aofFsync,
        boolean snapshotEnabled,
        String snapshotFilename,
        int snapshotIntervalSeconds,
        long expiryActiveIntervalMs,
        int expirySampleCount
) {
    public static final int DEFAULT_PORT = 6380;
    public static final int DEFAULT_HTTP_PORT = 6381;

    public static RespLiteConfig load(String[] args) throws IOException {
        Properties props = new Properties();
        try (InputStream in = RespLiteConfig.class.getClassLoader().getResourceAsStream("resplite.properties")) {
            if (in != null) {
                props.load(in);
            }
        }
        applyEnv(props);
        applyArgs(props, args);
        return fromProperties(props);
    }

    static void applyEnv(Properties props) {
        envInt(props, "server.port", "RESPLITE_PORT");
        envStr(props, "server.bind", "RESPLITE_BIND");
        envInt(props, "http.port", "RESPLITE_HTTP_PORT");
        envStr(props, "data.dir", "RESPLITE_DATA_DIR");
        envStr(props, "aof.enabled", "RESPLITE_AOF_ENABLED");
        envStr(props, "aof.filename", "RESPLITE_AOF_FILENAME");
        envStr(props, "aof.fsync", "RESPLITE_AOF_FSYNC");
        envStr(props, "snapshot.enabled", "RESPLITE_SNAPSHOT_ENABLED");
        envStr(props, "snapshot.filename", "RESPLITE_SNAPSHOT_FILENAME");
        envInt(props, "snapshot.interval.seconds", "RESPLITE_SNAPSHOT_INTERVAL");
        envInt(props, "expiry.active.interval.ms", "RESPLITE_EXPIRY_INTERVAL_MS");
        envInt(props, "expiry.sample.count", "RESPLITE_EXPIRY_SAMPLE_COUNT");
    }

    private static void envStr(Properties props, String key, String env) {
        String v = System.getenv(env);
        if (v != null && !v.isBlank()) {
            props.setProperty(key, v);
        }
    }

    private static void envInt(Properties props, String key, String env) {
        String v = System.getenv(env);
        if (v != null && !v.isBlank()) {
            props.setProperty(key, v);
        }
    }

    static void applyArgs(Properties props, String[] args) {
        if (args == null) {
            return;
        }
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port", "-p" -> {
                    if (i + 1 < args.length) {
                        props.setProperty("server.port", args[++i]);
                    }
                }
                case "--data-dir" -> {
                    if (i + 1 < args.length) {
                        props.setProperty("data.dir", args[++i]);
                    }
                }
                case "--aof-fsync" -> {
                    if (i + 1 < args.length) {
                        props.setProperty("aof.fsync", args[++i]);
                    }
                }
                case "--snapshot-interval" -> {
                    if (i + 1 < args.length) {
                        props.setProperty("snapshot.interval.seconds", args[++i]);
                    }
                }
                default -> { }
            }
        }
    }

    static RespLiteConfig fromProperties(Properties props) throws IOException {
        String bind = props.getProperty("server.bind", "0.0.0.0");
        int port = parseInt(props.getProperty("server.port"), DEFAULT_PORT);
        int httpPort = parseInt(props.getProperty("http.port"), DEFAULT_HTTP_PORT);
        Path dataDir = Path.of(props.getProperty("data.dir", "./data")).toAbsolutePath().normalize();
        Files.createDirectories(dataDir);
        boolean aofEnabled = Boolean.parseBoolean(props.getProperty("aof.enabled", "true"));
        String aofFile = props.getProperty("aof.filename", "appendonly.aof");
        AofFsyncPolicy fsync = AofFsyncPolicy.fromString(props.getProperty("aof.fsync", "everysec"));
        boolean snapEnabled = Boolean.parseBoolean(props.getProperty("snapshot.enabled", "true"));
        String snapFile = props.getProperty("snapshot.filename", "dump.rdb");
        int snapInterval = parseInt(props.getProperty("snapshot.interval.seconds"), 300);
        long expiryInterval = parseLong(props.getProperty("expiry.active.interval.ms"), 100L);
        int expirySample = parseInt(props.getProperty("expiry.sample.count"), 20);
        return new RespLiteConfig(
                bind, port, httpPort, dataDir, aofEnabled, aofFile, fsync,
                snapEnabled, snapFile, snapInterval, expiryInterval, expirySample
        );
    }

    private static int parseInt(String s, int def) {
        if (s == null || s.isBlank()) {
            return def;
        }
        return Integer.parseInt(s.trim());
    }

    private static long parseLong(String s, long def) {
        if (s == null || s.isBlank()) {
            return def;
        }
        return Long.parseLong(s.trim());
    }

    public Path aofPath() {
        return dataDir.resolve(aofFilename);
    }

    public Path snapshotPath() {
        return dataDir.resolve(snapshotFilename);
    }
}