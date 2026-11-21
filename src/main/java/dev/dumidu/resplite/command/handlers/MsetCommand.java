package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.command.RedisMessages;
import dev.dumidu.resplite.protocol.RespValue;
import dev.dumidu.resplite.store.StoredEntry;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class MsetCommand implements CommandHandler {
    @Override
    public String name() {
        return "MSET";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        if (args.size() < 3 || (args.size() - 1) % 2 != 0) {
            return RespValue.err(RedisMessages.WRONG_ARGS);
        }
        for (int i = 1; i < args.size(); i += 2) {
            String key = args.get(i);
            byte[] value = args.get(i + 1).getBytes(StandardCharsets.UTF_8);
            ctx.keyspace().put(key, new StoredEntry(value, StoredEntry.NO_EXPIRY));
            ctx.onWrite(List.of("SET", key, args.get(i + 1)));
        }
        return RespValue.ok("OK");
    }
}