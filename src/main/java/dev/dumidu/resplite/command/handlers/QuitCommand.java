package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.protocol.RespValue;

import java.util.List;

public final class QuitCommand implements CommandHandler {
    @Override
    public String name() {
        return "QUIT";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        return RespValue.ok("OK");
    }

    public boolean shouldClose() {
        return true;
    }
}