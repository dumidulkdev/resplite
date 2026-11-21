package dev.dumidu.resplite.persistence;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandDispatcher;
import dev.dumidu.resplite.command.CommandRegistry;
import dev.dumidu.resplite.command.handlers.ExpireAtCommand;
import dev.dumidu.resplite.protocol.RespDecoder;
import dev.dumidu.resplite.protocol.RespDecodeException;
import dev.dumidu.resplite.protocol.RespValue;
import dev.dumidu.resplite.server.ServerMetrics;
import dev.dumidu.resplite.store.Keyspace;
import dev.dumidu.resplite.store.StoredEntry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AofReplayer {
    private static final Logger log = LoggerFactory.getLogger(AofReplayer.class);

    private AofReplayer() {
    }

    public static void replay(Path aof, Keyspace keyspace) throws IOException {
        if (!Files.exists(aof) || Files.size(aof) == 0) {
            return;
        }
        byte[] bytes = Files.readAllBytes(aof);
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        RespDecoder decoder = new RespDecoder();
        CommandContext ctx = new CommandContext(keyspace, new ServerMetrics(), null);
        CommandDispatcher dispatcher = new CommandDispatcher(ctx, withExpireAt(CommandRegistry.createDefaults()));
        int applied = 0;
        while (buf.isReadable()) {
            try {
                RespValue value = decoder.readValue(buf);
                if (value == null) {
                    log.warn("Skipping truncated AOF tail ({} bytes remaining)", buf.readableBytes());
                    break;
                }
                if (!(value instanceof RespValue.ArrayValue arr)) {
                    continue;
                }
                List<String> args = CommandDispatcher.toArgs(arr);
                if (args.isEmpty()) {
                    continue;
                }
                dispatcher.dispatch(args);
                applied++;
            } catch (RespDecodeException e) {
                log.warn("Skipping corrupt AOF entry: {}", e.getMessage());
                break;
            }
        }
        log.info("Replayed {} commands from AOF", applied);
        pruneExpired(keyspace);
    }

    private static List<dev.dumidu.resplite.command.CommandHandler> withExpireAt(List<dev.dumidu.resplite.command.CommandHandler> base) {
        List<dev.dumidu.resplite.command.CommandHandler> all = new ArrayList<>(base);
        all.add(new ExpireAtCommand());
        return all;
    }

    private static void pruneExpired(Keyspace keyspace) {
        long now = keyspace.nowMs();
        keyspace.snapshot().forEach((k, v) -> {
            if (v.isExpiredAt(now)) {
                keyspace.remove(k);
            }
        });
    }

    public static List<String> encodeSetWithExpiry(String key, String value, long expireAtMs) {
        List<String> cmds = new ArrayList<>();
        cmds.add("SET");
        cmds.add(key);
        cmds.add(value);
        if (expireAtMs > 0) {
            // represented separately in logical log
        }
        return cmds;
    }
}