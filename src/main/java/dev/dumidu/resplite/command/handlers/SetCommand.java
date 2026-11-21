package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.command.RedisMessages;
import dev.dumidu.resplite.protocol.RespValue;
import dev.dumidu.resplite.store.StoredEntry;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class SetCommand implements CommandHandler {
    @Override
    public String name() {
        return "SET";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        if (args.size() < 3) {
            return RespValue.err(RedisMessages.WRONG_ARGS);
        }
        String key = args.get(1);
        byte[] value = args.get(2).getBytes(StandardCharsets.UTF_8);
        long expireAt = StoredEntry.NO_EXPIRY;
        boolean nx = false;
        boolean xx = false;
        for (int i = 3; i < args.size(); i++) {
            String opt = args.get(i).toUpperCase(Locale.ROOT);
            switch (opt) {
                case "EX" -> {
                    if (++i >= args.size()) {
                        return RespValue.err(RedisMessages.SYNTAX);
                    }
                    long sec = parseDuration(args.get(i));
                    if (sec < 0) {
                        return RespValue.err(RedisMessages.SYNTAX);
                    }
                    expireAt = ctx.keyspace().nowMs() + sec * 1000L;
                }
                case "PX" -> {
                    if (++i >= args.size()) {
                        return RespValue.err(RedisMessages.SYNTAX);
                    }
                    long ms = parseDuration(args.get(i));
                    if (ms < 0) {
                        return RespValue.err(RedisMessages.SYNTAX);
                    }
                    expireAt = ctx.keyspace().nowMs() + ms;
                }
                case "NX" -> nx = true;
                case "XX" -> xx = true;
                default -> {
                    return RespValue.err(RedisMessages.SYNTAX);
                }
            }
        }
        boolean exists = ctx.keyspace().getEntry(key).isPresent();
        if (nx && exists) {
            return RespValue.BulkString.nullBulk();
        }
        if (xx && !exists) {
            return RespValue.BulkString.nullBulk();
        }
        StoredEntry entry = new StoredEntry(value, expireAt);
        ctx.keyspace().put(key, entry);
        List<String> log = new ArrayList<>(args.subList(0, 3));
        ctx.onWrite(log);
        if (expireAt != StoredEntry.NO_EXPIRY) {
            ctx.onWrite(List.of("EXPIREAT", key, Long.toString(expireAt)));
        }
        return RespValue.ok("OK");
    }

    private static long parseDuration(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}