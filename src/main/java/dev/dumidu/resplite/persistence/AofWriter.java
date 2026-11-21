package dev.dumidu.resplite.persistence;

import dev.dumidu.resplite.config.AofFsyncPolicy;
import dev.dumidu.resplite.protocol.RespEncode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AofWriter implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AofWriter.class);

    private final Path path;
    private final AofFsyncPolicy fsyncPolicy;
    private final Object lock = new Object();
    private final OutputStream out;
    private final ScheduledExecutorService fsyncScheduler;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public AofWriter(Path path, AofFsyncPolicy fsyncPolicy) throws IOException {
        this.path = path;
        this.fsyncPolicy = fsyncPolicy;
        Files.createDirectories(path.getParent());
        this.out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if (fsyncPolicy == AofFsyncPolicy.EVERYSEC) {
            fsyncScheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "aof-fsync"));
            fsyncScheduler.scheduleAtFixedRate(this::fsyncIfDirty, 1, 1, TimeUnit.SECONDS);
        } else {
            fsyncScheduler = null;
        }
    }

    public void append(List<String> command) throws IOException {
        ByteBuf buf = Unpooled.buffer();
        RespEncode.writeCommand(buf, command);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        synchronized (lock) {
            out.write(bytes);
            out.flush();
            dirty.set(true);
            if (fsyncPolicy == AofFsyncPolicy.ALWAYS) {
                fsync();
            }
        }
    }

    public void fsyncIfDirty() {
        if (dirty.compareAndSet(true, false)) {
            try {
                fsync();
            } catch (IOException e) {
                log.warn("AOF fsync failed: {}", e.getMessage());
            }
        }
    }

    public void fsync() throws IOException {
        synchronized (lock) {
            out.flush();
            try (var channel = java.nio.channels.FileChannel.open(path, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (fsyncScheduler != null) {
            fsyncScheduler.shutdownNow();
        }
        fsync();
        synchronized (lock) {
            out.close();
        }
    }

    public Path path() {
        return path;
    }
}