package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.command.RedisMessages;
import dev.dumidu.resplite.protocol.RespValue;

import java.util.List;

public final class KeysCommand implements CommandHandler {
    @Override
    public String name() {
        return "KEYS";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        if (args.size() != 2) {
            return RespValue.err(RedisMessages.WRONG_ARGS);
        }
        List<RespValue> keys = ctx.keyspace().keysMatching(args.get(1)).stream()
                .map(RespValue.BulkString::ofUtf8)
                .map(RespValue.class::cast)
                .toList();
        return new RespValue.ArrayValue(keys);
    }
}