package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.protocol.RespValue;

import java.util.List;

public final class FlushAllCommand implements CommandHandler {
    @Override
    public String name() {
        return "FLUSHALL";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        ctx.keyspace().clear();
        ctx.onWrite(List.of("FLUSHALL"));
        return RespValue.ok("OK");
    }
}