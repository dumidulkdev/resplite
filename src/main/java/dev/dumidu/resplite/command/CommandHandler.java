package dev.dumidu.resplite.command;

import dev.dumidu.resplite.protocol.RespValue;

import java.util.List;

public interface CommandHandler {
    String name();

    RespValue execute(List<String> args, CommandContext ctx);
}