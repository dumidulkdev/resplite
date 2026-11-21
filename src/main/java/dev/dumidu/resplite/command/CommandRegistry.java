package dev.dumidu.resplite.command;

import dev.dumidu.resplite.command.handlers.*;

import java.util.List;

public final class CommandRegistry {
    private CommandRegistry() {
    }

    public static List<CommandHandler> createDefaults() {
        return List.of(
                new PingCommand(),
                new EchoCommand(),
                new GetCommand(),
                new SetCommand(),
                new DelCommand(),
                new ExistsCommand(),
                new IncrCommand(),
                new DecrCommand(),
                new IncrByCommand(),
                new MgetCommand(),
                new MsetCommand(),
                new KeysCommand(),
                new DbsizeCommand(),
                new FlushAllCommand(),
                new CommandCommand(),
                new QuitCommand(),
                new ExpireCommand(),
                new PexpireCommand(),
                new TtlCommand(),
                new PttlCommand(),
                new PersistCommand(),
                new ExpireAtCommand(),
                new SaveCommand(),
                new BgSaveCommand(),
                new BgRewriteAofCommand()
        );
    }
}