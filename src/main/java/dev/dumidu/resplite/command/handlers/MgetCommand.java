package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.command.RedisMessages;
import dev.dumidu.resplite.protocol.RespValue;

import java.util.ArrayList;
import java.util.List;

public final class MgetCommand implements CommandHandler {
    @Override
    public String name() {
        return "MGET";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        if (args.size() < 2) {
            return RespValue.err(RedisMessages.WRONG_ARGS);
        }
        List<RespValue> values = new ArrayList<>();
        for (int i = 1; i < args.size(); i++) {
            values.add(ctx.keyspace().get(args.get(i))
                    .map(RespValue.BulkString::new)
                    .orElse(RespValue.BulkString.nullBulk()));
        }
        return new RespValue.ArrayValue(values);
    }
}