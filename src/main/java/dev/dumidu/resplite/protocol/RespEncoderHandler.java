package dev.dumidu.resplite.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RespEncoderHandler extends MessageToByteEncoder<RespValue> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RespValue msg, ByteBuf out) {
        RespEncode.write(out, msg);
    }
}