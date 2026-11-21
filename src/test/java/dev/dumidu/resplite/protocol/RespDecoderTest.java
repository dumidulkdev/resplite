package dev.dumidu.resplite.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RespDecoderTest {

    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        channel = new EmbeddedChannel(new RespDecoder());
    }

    @AfterEach
    void tearDown() {
        channel.finishAndReleaseAll();
    }

    @Test
    void decodesSimpleString() {
        writeInbound("+PONG\r\n");
        RespValue v = readOne();
        assertThat(v).isInstanceOf(RespValue.SimpleString.class);
        assertThat(((RespValue.SimpleString) v).value()).isEqualTo("PONG");
    }

    @Test
    void decodesErrorAndInteger() {
        writeInbound("-ERR nope\r\n:100\r\n");
        assertThat(((RespValue.ErrorReply) readOne()).message()).isEqualTo("ERR nope");
        assertThat(((RespValue.IntegerValue) readOne()).value()).isEqualTo(100L);
    }

    @Test
    void decodesBulkAndNull() {
        writeInbound("$3\r\nfoo\r\n$-1\r\n");
        assertThat(((RespValue.BulkString) readOne()).asUtf8()).isEqualTo("foo");
        assertThat(((RespValue.BulkString) readOne()).isNull()).isTrue();
    }

    @Test
    void decodesArray() {
        writeInbound("*2\r\n$3\r\nSET\r\n$3\r\nkey\r\n");
        RespValue.ArrayValue arr = (RespValue.ArrayValue) readOne();
        assertThat(arr.elements()).hasSize(2);
        assertThat(((RespValue.BulkString) arr.elements().get(0)).asUtf8()).isEqualTo("SET");
    }

    @Test
    void handlesFragmentedBulk() throws RespDecodeException {
        RespDecoder decoder = new RespDecoder();
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes("$10\r\nhello".getBytes());
        assertThat(decoder.readValue(buf)).isNull();
        assertThat(buf.readerIndex()).isZero();
        buf.writeBytes("world\r\n".getBytes());
        RespValue v = decoder.readValue(buf);
        assertThat(((RespValue.BulkString) v).asUtf8()).isEqualTo("helloworld");
    }

    @Test
    void pipelinesMultipleCommands() {
        writeInbound("*1\r\n$4\r\nPING\r\n*1\r\n$4\r\nPING\r\n");
        RespValue.ArrayValue a1 = (RespValue.ArrayValue) readOne();
        RespValue.ArrayValue a2 = (RespValue.ArrayValue) readOne();
        assertThat(((RespValue.BulkString) a1.elements().getFirst()).asUtf8()).isEqualTo("PING");
        assertThat(((RespValue.BulkString) a2.elements().getFirst()).asUtf8()).isEqualTo("PING");
    }

    @Test
    void inlinePingCommand() {
        writeInbound("PING\r\n");
        RespValue.ArrayValue arr = (RespValue.ArrayValue) readOne();
        assertThat(((RespValue.BulkString) arr.elements().getFirst()).asUtf8()).isEqualTo("PING");
    }

    @Test
    void inlineMultiArg() {
        writeInbound("SET mykey myval\r\n");
        RespValue.ArrayValue arr = (RespValue.ArrayValue) readOne();
        assertThat(arr.elements()).hasSize(3);
    }

    @Test
    void protocolErrorReturnsErrReply() {
        writeInbound("$bad\r\n");
        RespValue v = readOne();
        assertThat(v).isInstanceOf(RespValue.ErrorReply.class);
    }

  private void writeInbound(String s) {
        channel.writeInbound(Unpooled.copiedBuffer(s.getBytes()));
    }

    private RespValue readOne() {
        Object msg = channel.readInbound();
        assertThat(msg).isNotNull();
        return (RespValue) msg;
    }

    @Test
    void decoderRoundTripWithEncoder() throws RespDecodeException {
        RespDecoder decoder = new RespDecoder();
        RespValue original = new RespValue.ArrayValue(List.of(
                RespValue.BulkString.ofUtf8("GET"),
                RespValue.BulkString.ofUtf8("k")
        ));
        ByteBuf encoded = RespEncode.encode(original);
        RespValue decoded = decoder.readValue(encoded);
        assertThat(decoded).isInstanceOf(RespValue.ArrayValue.class);
        assertThat(encoded.readableBytes()).isZero();
    }
}