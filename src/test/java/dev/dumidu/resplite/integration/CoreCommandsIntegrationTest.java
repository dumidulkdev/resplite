package dev.dumidu.resplite.integration;

import dev.dumidu.resplite.support.RespLiteServerRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoreCommandsIntegrationTest {

    @RegisterExtension
    static final RespLiteServerRule rule = new RespLiteServerRule();

  private Jedis jedis() {
        return new Jedis("127.0.0.1", rule.port());
    }

    @Test
    void pingAndEcho() {
        try (Jedis j = jedis()) {
            assertThat(j.ping()).isEqualTo("PONG");
            assertThat(j.echo("hello")).isEqualTo("hello");
        }
    }

    @Test
    void setGetWithOptions() {
        try (Jedis j = jedis()) {
            assertThat(j.set("k", "v")).isEqualTo("OK");
            assertThat(j.get("k")).isEqualTo("v");
            assertThat(j.set("nx", "1", SetParams.setParams().nx())).isEqualTo("OK");
            assertThat(j.set("nx", "2", SetParams.setParams().nx())).isNull();
            assertThat(j.get("nx")).isEqualTo("1");
        }
    }

    @Test
    void delExistsMgetMset() {
        try (Jedis j = jedis()) {
            j.mset("a", "1", "b", "2");
            assertThat(j.exists("a", "b", "missing")).isEqualTo(2L);
            assertThat(j.mget("a", "b")).isEqualTo(List.of("1", "2"));
            assertThat(j.del("a", "b")).isEqualTo(2L);
        }
    }

    @Test
    void incrConcurrency() throws Exception {
        try (Jedis j = jedis()) {
            j.set("counter", "0");
        }
        int threads = 32;
        int perThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger errors = new AtomicInteger();
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try (Jedis j = jedis()) {
                    for (int i = 0; i < perThread; i++) {
                        j.incr("counter");
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();
        assertThat(errors.get()).isZero();
        try (Jedis j = jedis()) {
            assertThat(j.get("counter")).isEqualTo(Integer.toString(threads * perThread));
        }
    }

    @Test
    void keysAndDbsize() {
        try (Jedis j = jedis()) {
            j.set("user:1", "a");
            j.set("user:2", "b");
            j.set("other", "c");
            assertThat(j.keys("user:*")).containsExactlyInAnyOrder("user:1", "user:2");
            assertThat(j.dbSize()).isGreaterThanOrEqualTo(3);
            j.flushAll();
            assertThat(j.dbSize()).isZero();
        }
    }

    @Test
    void incrByRejectNonIntegerValue() {
        try (Jedis j = jedis()) {
            j.set("x", "notanumber");
            assertThatThrownBy(() -> j.incr("x"))
                    .hasMessageContaining("not an integer");
        }
    }
}