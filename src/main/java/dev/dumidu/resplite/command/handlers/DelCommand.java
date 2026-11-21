package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.command.RedisMessages;
import dev.dumidu.resplite.protocol.RespValue;

import java.util.ArrayList;
import java.util.List;

public final class DelCommand implements CommandHandler {
    @Override
    public String name() {
        return "DEL";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        if (args.size() < 2) {
            return RespValue.err(RedisMessages.WRONG_ARGS);
        }
        long removed = 0;
        for (int i = 1; i < args.size(); i++) {
            if (ctx.keyspace().remove(args.get(i))) {
                removed++;
                ctx.onWrite(List.of("DEL", args.get(i)));
            }
        }
        return RespValue.integer(removed);
    }
}