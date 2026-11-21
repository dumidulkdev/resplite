package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.config.RespLiteConfig;
import dev.dumidu.resplite.protocol.RespValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class InfoCommand implements CommandHandler {
    private final RespLiteConfig config;

    public InfoCommand(RespLiteConfig config) {
        this.config = config;
    }

    @Override
    public String name() {
        return "INFO";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        String section = (args.size() > 1 ? args.get(1) : "default").toUpperCase(Locale.ROOT);
        String info = buildInfo(ctx, section);
        return RespValue.BulkString.ofUtf8(info);
    }

    private String buildInfo(CommandContext ctx, String section) {
        List<String> lines = new ArrayList<>();
        if (section.equals("DEFAULT") || section.equals("SERVER")) {
            lines.add("# Server");
            lines.add("redis_version:7.0.0-resplite");
            lines.add("redis_mode:standalone");
            lines.add("resplite_version:1.0.0");
            long uptime = (System.currentTimeMillis() - ctx.metrics().startedAtMs()) / 1000;
            lines.add("uptime_in_seconds:" + uptime);
            lines.add("tcp_port:" + config.serverPort());
        }
        if (section.equals("DEFAULT") || section.equals("CLIENTS")) {
            lines.add("# Clients");
            lines.add("connected_clients:" + ctx.metrics().connectedClients());
        }
        if (section.equals("DEFAULT") || section.equals("MEMORY")) {
            lines.add("# Memory");
            lines.add("used_memory:" + ctx.keyspace().memoryEstimateBytes());
        }
        if (section.equals("DEFAULT") || section.equals("KEYSPACE")) {
            lines.add("# Keyspace");
            lines.add("db0:keys=" + ctx.keyspace().size());
        }
        if (section.equals("DEFAULT") || section.equals("PERSISTENCE")) {
            lines.add("# Persistence");
            lines.add("aof_enabled:" + (config.aofEnabled() ? 1 : 0));
            lines.add("aof_fsync:" + config.aofFsync().wireName());
            lines.add("rdb_enabled:" + (config.snapshotEnabled() ? 1 : 0));
        }
        if (section.equals("DEFAULT") || section.equals("STATS")) {
            lines.add("# Stats");
            lines.add("total_commands_processed:" + ctx.metrics().commandsProcessed());
            lines.add("instantaneous_ops_per_sec:" + (long) ctx.metrics().opsPerSecond());
        }
        return String.join("\r\n", lines) + "\r\n";
    }
}