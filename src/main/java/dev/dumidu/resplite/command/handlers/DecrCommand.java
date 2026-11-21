package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.protocol.RespValue;

import java.util.List;

public final class DecrCommand extends CounterCommand implements CommandHandler {
    @Override
    public String name() {
        return "DECR";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        return adjust(args, ctx, -1);
    }
}