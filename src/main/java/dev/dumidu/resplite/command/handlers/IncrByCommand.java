package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.command.IntegerCodec;
import dev.dumidu.resplite.command.RedisMessages;
import dev.dumidu.resplite.protocol.RespValue;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class IncrByCommand extends CounterCommand implements CommandHandler {
    @Override
    public String name() {
        return "INCRBY";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        if (args.size() != 3) {
            return RespValue.err(RedisMessages.WRONG_ARGS);
        }
        Long delta = IntegerCodec.parseLongOrNull(args.get(2).getBytes(StandardCharsets.UTF_8));
        if (delta == null) {
            return RespValue.err(RedisMessages.NOT_INTEGER);
        }
        return adjust(List.of(args.get(0), args.get(1)), ctx, delta);
    }
}