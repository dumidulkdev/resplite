package dev.dumidu.resplite;

import dev.dumidu.resplite.command.CommandContext;
import dev.dumidu.resplite.command.CommandDispatcher;
import dev.dumidu.resplite.command.CommandRegistry;
import dev.dumidu.resplite.config.RespLiteConfig;
import dev.dumidu.resplite.protocol.RespDecoder;
import dev.dumidu.resplite.protocol.RespEncoderHandler;
import dev.dumidu.resplite.server.RespCommandHandler;
import dev.dumidu.resplite.server.ServerMetrics;
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

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final AtomicReference<java.util.function.Consumer<java.util.List<String>>> writeHook = new AtomicReference<>();

    public RespLiteServer(RespLiteConfig config) {
        this(config, Clock.systemUTC());
    }

    public RespLiteServer(RespLiteConfig config, Clock clock) {
        this.config = config;
        this.clock = clock;
        this.keyspace = new Keyspace(clock);
        this.metrics = new ServerMetrics();
        CommandContext ctx = new CommandContext(keyspace, metrics, cmd -> {
            java.util.function.Consumer<java.util.List<String>> hook = writeHook.get();
            if (hook != null) {
                hook.accept(cmd);
            }
        });
        this.dispatcher = new CommandDispatcher(ctx, CommandRegistry.createDefaults());
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
        log.info("RespLite listening on {}:{}", config.bindHost(), config.serverPort());
    }

    public void awaitShutdown() throws InterruptedException {
        if (serverChannel != null) {
            serverChannel.closeFuture().sync();
        }
    }

    public void shutdown() {
        log.info("RespLite shutting down");
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