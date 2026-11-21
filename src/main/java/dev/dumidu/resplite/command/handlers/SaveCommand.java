package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.protocol.RespValue;

import java.util.List;

public final class SaveCommand implements CommandHandler {
    @Override
    public String name() {
        return "SAVE";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        if (ctx.persistence() == null || ctx.persistence().snapshotScheduler() == null) {
            return RespValue.err("ERR persistence disabled");
        }
        ctx.persistence().snapshotScheduler().save(true);
        return RespValue.ok("OK");
    }
}