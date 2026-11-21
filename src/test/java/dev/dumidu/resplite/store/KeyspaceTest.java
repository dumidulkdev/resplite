package dev.dumidu.resplite.store;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class KeyspaceTest {

    @Test
    void globPattern() {
        assertThat(Keyspace.globToRegex("user:*")).isEqualTo("^user:.*$");
        assertThat(Keyspace.globToRegex("a?c")).isEqualTo("^a.c$");
    }

    @Test
    void lazyExpiryOnGet() {
        AtomicLong now = new AtomicLong(1000);
        Clock clock = Clock.fixed(Instant.ofEpochMilli(now.get()), ZoneId.of("UTC"));
        Keyspace ks = new Keyspace(clock);
        ks.put("k", new StoredEntry("v".getBytes(), 500));
        assertThat(ks.get("k")).isEmpty();
        now.set(400);
        Clock earlier = Clock.fixed(Instant.ofEpochMilli(now.get()), ZoneId.of("UTC"));
        Keyspace ks2 = new Keyspace(earlier);
        ks2.put("k2", new StoredEntry("v".getBytes(), 1000));
        assertThat(ks2.get("k2")).contains("v".getBytes());
    }
}