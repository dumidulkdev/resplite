package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.IntegerCodec;
import dev.dumidu.resplite.command.RedisMessages;
import dev.dumidu.resplite.protocol.RespValue;
import dev.dumidu.resplite.store.StoredEntry;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

abstract class CounterCommand {
    protected RespValue adjust(List<String> args, CommandContext ctx, long delta) {
        if (args.size() != 2) {
            return RespValue.err(RedisMessages.WRONG_ARGS);
        }
        String key = args.get(1);
        try {
            long next = ctx.keyspace().increment(key, delta);
            ctx.onWrite(List.of("SET", key, Long.toString(next)));
            return RespValue.integer(next);
        } catch (IllegalStateException e) {
            return RespValue.err(RedisMessages.NOT_INTEGER);
        }
    }
}