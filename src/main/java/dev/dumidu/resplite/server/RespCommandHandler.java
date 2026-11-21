package dev.dumidu.resplite.server;

import dev.dumidu.resplite.command.CommandDispatcher;
import dev.dumidu.resplite.protocol.RespValue;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RespCommandHandler extends SimpleChannelInboundHandler<RespValue> {
    private static final Logger log = LoggerFactory.getLogger(RespCommandHandler.class);
    private final CommandDispatcher dispatcher;
    private final ServerMetrics metrics;

    public RespCommandHandler(CommandDispatcher dispatcher, ServerMetrics metrics) {
        this.dispatcher = dispatcher;
        this.metrics = metrics;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        metrics.clientConnected();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        metrics.clientDisconnected();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RespValue msg) {
        if (!(msg instanceof RespValue.ArrayValue arr)) {
            ctx.writeAndFlush(RespValue.err("ERR protocol error: expected array"));
            return;
        }
        long start = System.nanoTime();
        try {
            List<String> args = CommandDispatcher.toArgs(arr);
            if (args.isEmpty()) {
                ctx.writeAndFlush(RespValue.err("ERR empty command"));
                return;
            }
            RespValue reply = dispatcher.dispatch(args);
            ctx.writeAndFlush(reply);
            if ("QUIT".equalsIgnoreCase(args.getFirst()) && reply instanceof RespValue.SimpleString) {
                ctx.close();
            }
        } finally {
            metrics.recordCommand(System.nanoTime() - start);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("Connection error: {}", cause.getMessage());
        ctx.writeAndFlush(RespValue.err("ERR " + cause.getMessage()));
        ctx.close();
    }
}