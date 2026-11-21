package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.command.RedisMessages;
import dev.dumidu.resplite.protocol.RespValue;

import java.util.List;

public final class PersistCommand implements CommandHandler {
    @Override
    public String name() {
        return "PERSIST";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        if (args.size() != 2) {
            return RespValue.err(RedisMessages.WRONG_ARGS);
        }
        boolean ok = ctx.keyspace().persist(args.get(1));
        if (ok) {
            ctx.onWrite(List.of("PERSIST", args.get(1)));
        }
        return RespValue.integer(ok ? 1 : 0);
    }
}