package dev.dumidu.resplite;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandDispatcher;
import dev.dumidu.resplite.command.CommandRegistry;
import dev.dumidu.resplite.config.RespLiteConfig;
import dev.dumidu.resplite.persistence.AofReplayer;
import dev.dumidu.resplite.persistence.AofWriter;
import dev.dumidu.resplite.persistence.SnapshotFormat;
import dev.dumidu.resplite.persistence.SnapshotScheduler;
import dev.dumidu.resplite.protocol.RespDecoder;
import dev.dumidu.resplite.protocol.RespEncoderHandler;
import dev.dumidu.resplite.server.ObservabilityHttpServer;
import dev.dumidu.resplite.server.PersistenceContext;
import dev.dumidu.resplite.server.RespCommandHandler;
import dev.dumidu.resplite.server.ServerMetrics;
import dev.dumidu.resplite.store.ActiveExpiryScanner;
import dev.dumidu.resplite.store.Keyspace;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;

public class RespLiteServer {
    private static final Logger log = LoggerFactory.getLogger(RespLiteServer.class);

    private final RespLiteConfig config;
    private final Keyspace keyspace;
    private final ServerMetrics metrics;
    private final CommandDispatcher dispatcher;
    private final Clock clock;
    private final PersistenceContext persistenceContext;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final AtomicReference<java.util.function.Consumer<java.util.List<String>>> writeHook = new AtomicReference<>();
    private ActiveExpiryScanner expiryScanner;
    private SnapshotScheduler snapshotScheduler;
    private ObservabilityHttpServer httpServer;

    public RespLiteServer(RespLiteConfig config) {
        this(config, Clock.systemUTC());
    }

    public RespLiteServer(RespLiteConfig config, Clock clock) {
        this.config = config;
        this.clock = clock;
        this.keyspace = new Keyspace(clock);
        this.metrics = new ServerMetrics();
        AofWriter aofWriter = null;
        SnapshotScheduler sched = null;
        try {
            if (config.snapshotEnabled()) {
                SnapshotFormat.load(config.snapshotPath(), keyspace);
            }
            if (config.aofEnabled()) {
                AofReplayer.replay(config.aofPath(), keyspace);
                aofWriter = new AofWriter(config.aofPath(), config.aofFsync());
                final AofWriter writer = aofWriter;
                setWriteHook(cmd -> {
                    try {
                        writer.append(cmd);
                    } catch (Exception e) {
                        log.error("AOF append failed: {}", e.getMessage());
                    }
                });
            }
            if (config.snapshotEnabled()) {
                sched = new SnapshotScheduler(keyspace, config.snapshotPath(), config.snapshotIntervalSeconds());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize persistence", e);
        }
        this.snapshotScheduler = sched;
        this.persistenceContext = new PersistenceContext(aofWriter, sched);
        CommandContext ctx = new CommandContext(keyspace, metrics, cmd -> {
            java.util.function.Consumer<java.util.List<String>> hook = writeHook.get();
            if (hook != null) {
                hook.accept(cmd);
            }
        }, persistenceContext);
        this.dispatcher = new CommandDispatcher(ctx, CommandRegistry.createDefaults(config));
    }

    public void setWriteHook(java.util.function.Consumer<java.util.List<String>> hook) {
        writeHook.set(hook);
    }

    public Keyspace keyspace() {
        return keyspace;
    }

    public ServerMetrics metrics() {
        return metrics;
    }

    public RespLiteConfig config() {
        return config;
    }

    public CommandDispatcher dispatcher() {
        return dispatcher;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new RespDecoder());
                        ch.pipeline().addLast(new RespEncoderHandler());
                        ch.pipeline().addLast(new RespCommandHandler(dispatcher, metrics));
                    }
                });
        serverChannel = b.bind(config.bindHost(), config.serverPort()).sync().channel();
        expiryScanner = new ActiveExpiryScanner(keyspace, config.expiryActiveIntervalMs(), config.expirySampleCount());
        try {
            httpServer = new ObservabilityHttpServer(config, keyspace, metrics);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to start HTTP observability server", e);
        }
        log.info("RespLite listening on {}:{} (HTTP {}:{})", config.bindHost(), config.serverPort(),
                config.bindHost(), config.httpPort());
    }

    public void awaitShutdown() throws InterruptedException {
        if (serverChannel != null) {
            serverChannel.closeFuture().sync();
        }
    }

    public void shutdown() {
        log.info("RespLite shutting down");
        if (expiryScanner != null) {
            expiryScanner.close();
        }
        if (snapshotScheduler != null) {
            snapshotScheduler.close();
        }
        if (httpServer != null) {
            httpServer.close();
        }
        if (persistenceContext != null && persistenceContext.aofWriter() != null) {
            try {
                persistenceContext.aofWriter().close();
            } catch (Exception e) {
                log.warn("AOF close error: {}", e.getMessage());
            }
        }
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }
}