package dev.dumidu.resplite.persistence;

import dev.dumidu.resplite.store.Keyspace;
import dev.dumidu.resplite.store.StoredEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnapshotFormatTest {

    @TempDir
    Path dir;

    @Test
    void roundTrip() throws Exception {
        Keyspace ks = new Keyspace(Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")));
        ks.put("a", new StoredEntry("1".getBytes(), StoredEntry.NO_EXPIRY));
        ks.put("b", new StoredEntry("2".getBytes(), 5_000));
        Path file = dir.resolve("dump.rdb");
        SnapshotFormat.write(file, ks);
        Keyspace loaded = new Keyspace(Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")));
        SnapshotFormat.load(file, loaded);
        assertThat(loaded.get("a")).contains("1".getBytes());
        assertThat(loaded.get("b")).contains("2".getBytes());
    }

    @Test
    void checksumFailure() throws Exception {
        Keyspace ks = new Keyspace(Clock.systemUTC());
        ks.put("k", new StoredEntry("v".getBytes(), StoredEntry.NO_EXPIRY));
        Path file = dir.resolve("dump.rdb");
        SnapshotFormat.write(file, ks);
        byte[] bytes = Files.readAllBytes(file);
        bytes[bytes.length - 1] ^= 0xFF;
        Files.write(file, bytes);
        Keyspace loaded = new Keyspace(Clock.systemUTC());
        assertThatThrownBy(() -> SnapshotFormat.load(file, loaded))
                .hasMessageContaining("checksum");
    }
}