package dev.dumidu.resplite.command.handlers;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandHandler;
import dev.dumidu.resplite.command.RedisMessages;
import dev.dumidu.resplite.protocol.RespValue;
import dev.dumidu.resplite.store.StoredEntry;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Internal command used by AOF to store absolute expiry timestamps. */
public final class ExpireAtCommand implements CommandHandler {
    @Override
    public String name() {
        return "EXPIREAT";
    }

    @Override
    public RespValue execute(List<String> args, CommandContext ctx) {
        if (args.size() != 3) {
            return RespValue.err(RedisMessages.WRONG_ARGS);
        }
        long at;
        try {
            at = Long.parseLong(args.get(2));
        } catch (NumberFormatException e) {
            return RespValue.err(RedisMessages.SYNTAX);
        }
        String key = args.get(1);
        if (at <= ctx.keyspace().nowMs()) {
            ctx.keyspace().remove(key);
            return RespValue.integer(1);
        }
        var opt = ctx.keyspace().getEntry(key);
        if (opt.isEmpty()) {
            return RespValue.integer(0);
        }
        StoredEntry e = opt.get();
        ctx.keyspace().put(key, e.withExpiry(at));
        return RespValue.integer(1);
    }

    public static void restoreSet(CommandContext ctx, String key, byte[] value, long expireAt) {
        if (expireAt > 0 && expireAt <= ctx.keyspace().nowMs()) {
            return;
        }
        ctx.keyspace().put(key, new StoredEntry(value, expireAt > 0 ? expireAt : StoredEntry.NO_EXPIRY));
    }
}