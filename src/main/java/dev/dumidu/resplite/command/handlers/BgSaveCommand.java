package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.protocol.RespValue;

import java.util.List;

public final class BgSaveCommand implements CommandHandler {
    @Override
    public String name() {
        return "BGSAVE";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        if (ctx.persistence() == null || ctx.persistence().snapshotScheduler() == null) {
            return RespValue.err("ERR persistence disabled");
        }
        ctx.persistence().snapshotScheduler().save(false);
        return RespValue.ok("Background saving started");
    }
}