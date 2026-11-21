package dev.dumidu.resplite.command;

import dev.dumidu.resplite.protocol.RespValue;
import dev.dumidu.resplite.server.ServerMetrics;
import dev.dumidu.resplite.store.Keyspace;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandDispatcherTest {

    @Test
    void unknownCommand() {
        Keyspace ks = new Keyspace(Clock.systemUTC());
        CommandContext ctx = new CommandContext(ks, new ServerMetrics(), c -> {});
        CommandDispatcher d = new CommandDispatcher(ctx, CommandRegistry.createDefaults());
        RespValue r = d.dispatch(List.of("NOTACMD"));
        assertThat(r).isInstanceOf(RespValue.ErrorReply.class);
        assertThat(((RespValue.ErrorReply) r).message()).contains("unknown command");
    }
}