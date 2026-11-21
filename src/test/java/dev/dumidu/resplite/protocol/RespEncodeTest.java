package dev.dumidu.resplite.protocol;

import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RespEncodeTest {

    @Test
    void encodesSimpleString() {
        assertUtf8(RespEncode.encode(new RespValue.SimpleString("OK")), "+OK\r\n");
    }

    @Test
    void encodesError() {
        assertUtf8(RespEncode.encode(new RespValue.ErrorReply("ERR bad")), "-ERR bad\r\n");
    }

    @Test
    void encodesInteger() {
        assertUtf8(RespEncode.encode(new RespValue.IntegerValue(42)), ":42\r\n");
    }

    @Test
    void encodesBulkString() {
        assertUtf8(RespEncode.encode(RespValue.BulkString.ofUtf8("hello")), "$5\r\nhello\r\n");
    }

    @Test
    void encodesNullBulk() {
        assertUtf8(RespEncode.encode(RespValue.BulkString.nullBulk()), "$-1\r\n");
    }

    @Test
    void encodesArray() {
        RespValue arr = new RespValue.ArrayValue(List.of(
                new RespValue.SimpleString("OK"),
                new RespValue.IntegerValue(1)
        ));
        assertUtf8(RespEncode.encode(arr), "*2\r\n+OK\r\n:1\r\n");
    }

    private static void assertUtf8(ByteBuf buf, String expected) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo(expected);
    }
}