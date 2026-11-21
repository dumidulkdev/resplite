package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.persistence.AofRewriter;
import dev.dumidu.resplite.protocol.RespValue;

import java.util.List;

public final class BgRewriteAofCommand implements CommandHandler {
    @Override
    public String name() {
        return "BGREWRITEAOF";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        if (ctx.persistence() == null || ctx.persistence().aofWriter() == null) {
            return RespValue.err("ERR persistence disabled");
        }
        try {
            AofRewriter.rewrite(ctx.persistence().aofWriter().path(), ctx.keyspace());
            return RespValue.ok("OK");
        } catch (Exception e) {
            return RespValue.err("ERR rewrite failed");
        }
    }
}