package dev.dumidu.resplite.command;

import dev.dumidu.resplite.protocol.RespValue;
import dev.dumidu.resplite.protocol.RespValue.BulkString;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CommandDispatcher {
    private final Map<String, CommandHandler> handlers = new HashMap<>();
    private final CommandContext context;

    public CommandDispatcher(CommandContext context, List<CommandHandler> handlers) {
        this.context = context;
        for (CommandHandler h : handlers) {
            this.handlers.put(h.name().toUpperCase(Locale.ROOT), h);
        }
    }

    public CommandContext context() {
        return context;
    }

    public RespValue dispatch(List<String> args) {
        if (args.isEmpty()) {
            return RespValue.err("ERR empty command");
        }
        String name = args.getFirst().toUpperCase(Locale.ROOT);
        CommandHandler handler = handlers.get(name);
        if (handler == null) {
            return RespValue.err("ERR unknown command `" + args.getFirst() + "`");
        }
        return handler.execute(args, context);
    }

    public static List<String> toArgs(RespValue.ArrayValue command) {
        return command.elements().stream()
                .map(e -> {
                    if (e instanceof BulkString b) {
                        return b.asUtf8();
                    }
                    return e.toString();
                })
                .toList();
    }
}