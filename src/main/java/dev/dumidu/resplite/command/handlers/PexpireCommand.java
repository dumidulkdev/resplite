package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.command.RedisMessages;
import dev.dumidu.resplite.protocol.RespValue;

import java.util.List;

public final class PexpireCommand implements CommandHandler {
    @Override
    public String name() {
        return "PEXPIRE";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        if (args.size() != 3) {
            return RespValue.err(RedisMessages.WRONG_ARGS);
        }
        long ms;
        try {
            ms = Long.parseLong(args.get(2));
        } catch (NumberFormatException e) {
            return RespValue.err(RedisMessages.SYNTAX);
        }
        long expireAt = ctx.keyspace().nowMs() + ms;
        boolean ok = ctx.keyspace().expireAt(args.get(1), expireAt);
        if (ok) {
            ctx.onWrite(List.of("EXPIREAT", args.get(1), Long.toString(expireAt)));
        }
        return RespValue.integer(ok ? 1 : 0);
    }
}