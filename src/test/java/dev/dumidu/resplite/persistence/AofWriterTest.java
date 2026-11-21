package dev.dumidu.resplite.persistence;

import dev.dumidu.resplite.config.AofFsyncPolicy;
import dev.dumidu.resplite.store.Keyspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AofWriterTest {

    @TempDir
    Path dir;

    @Test
    void appendAndReplay() throws Exception {
        Path aof = dir.resolve("test.aof");
        try (AofWriter writer = new AofWriter(aof, AofFsyncPolicy.ALWAYS)) {
            writer.append(List.of("SET", "a", "1"));
            writer.append(List.of("SET", "b", "2"));
        }
        Keyspace ks = new Keyspace(Clock.systemUTC());
        AofReplayer.replay(aof, ks);
        assertThat(ks.get("a")).contains("1".getBytes());
        assertThat(ks.get("b")).contains("2".getBytes());
    }
}