package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.protocol.RespValue;

import java.util.List;

public final class DbsizeCommand implements CommandHandler {
    @Override
    public String name() {
        return "DBSIZE";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        return RespValue.integer(ctx.keyspace().size());
    }
}