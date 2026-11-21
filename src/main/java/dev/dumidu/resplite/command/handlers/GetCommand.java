package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.command.RedisMessages;
import dev.dumidu.resplite.protocol.RespValue;

import java.util.List;

public final class GetCommand implements CommandHandler {
    @Override
    public String name() {
        return "GET";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        if (args.size() != 2) {
            return RespValue.err(RedisMessages.WRONG_ARGS);
        }
        return ctx.keyspace().get(args.get(1))
                .map(RespValue.BulkString::new)
                .orElse(RespValue.BulkString.nullBulk());
    }
}