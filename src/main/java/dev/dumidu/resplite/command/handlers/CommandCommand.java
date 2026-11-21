package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.protocol.RespValue;

import java.util.List;

public final class CommandCommand implements CommandHandler {
    private static final List<String> KNOWN = List.of(
            "PING", "ECHO", "GET", "SET", "DEL", "EXISTS", "INCR", "DECR", "INCRBY",
            "MGET", "MSET", "KEYS", "DBSIZE", "FLUSHALL", "QUIT", "COMMAND"
    );

    @Override
    public String name() {
        return "COMMAND";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        if (args.size() == 1) {
            List<RespValue> defs = KNOWN.stream()
                    .map(name -> new RespValue.ArrayValue(List.of(
                            RespValue.BulkString.ofUtf8(name),
                            RespValue.integer(-1),
                            RespValue.BulkString.ofUtf8(""))
                    ))
                    .map(RespValue.class::cast)
                    .toList();
            return new RespValue.ArrayValue(defs);
        }
        return new RespValue.ArrayValue(List.of());
    }
}