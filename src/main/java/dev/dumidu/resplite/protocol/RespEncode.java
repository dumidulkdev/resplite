package dev.dumidu.resplite.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class RespEncode {
    private static final byte[] CRLF = {'\r', '\n'};

    private RespEncode() {
    }

    public static ByteBuf encode(RespValue value) {
        ByteBuf buf = Unpooled.buffer();
        write(buf, value);
        return buf;
    }

    public static void write(ByteBuf buf, RespValue value) {
        switch (value) {
            case RespValue.SimpleString s -> writeSimple(buf, s.value());
            case RespValue.ErrorReply e -> writeError(buf, e.message());
            case RespValue.IntegerValue i -> writeInteger(buf, i.value());
            case RespValue.BulkString b -> writeBulk(buf, b);
            case RespValue.ArrayValue a -> writeArray(buf, a.elements());
        }
    }

    public static void writeSimple(ByteBuf buf, String value) {
        buf.writeByte('+');
        buf.writeCharSequence(value, StandardCharsets.UTF_8);
        buf.writeBytes(CRLF);
    }

    public static void writeError(ByteBuf buf, String message) {
        buf.writeByte('-');
        buf.writeCharSequence(message, StandardCharsets.UTF_8);
        buf.writeBytes(CRLF);
    }

    public static void writeInteger(ByteBuf buf, long value) {
        buf.writeByte(':');
        buf.writeCharSequence(Long.toString(value), StandardCharsets.UTF_8);
        buf.writeBytes(CRLF);
    }

    public static void writeBulk(ByteBuf buf, RespValue.BulkString bulk) {
        if (bulk.isNull()) {
            buf.writeCharSequence("$-1\r\n", StandardCharsets.US_ASCII);
            return;
        }
        byte[] content = bulk.content();
        buf.writeByte('$');
        buf.writeCharSequence(Integer.toString(content.length), StandardCharsets.US_ASCII);
        buf.writeBytes(CRLF);
        buf.writeBytes(content);
        buf.writeBytes(CRLF);
    }

    public static void writeArray(ByteBuf buf, List<RespValue> elements) {
        buf.writeByte('*');
        buf.writeCharSequence(Integer.toString(elements.size()), StandardCharsets.US_ASCII);
        buf.writeBytes(CRLF);
        for (RespValue el : elements) {
            write(buf, el);
        }
    }

    /** Encodes a command as a RESP array of bulk strings (for AOF). */
    public static void writeCommand(ByteBuf buf, List<String> args) {
        buf.writeByte('*');
        buf.writeCharSequence(Integer.toString(args.size()), StandardCharsets.US_ASCII);
        buf.writeBytes(CRLF);
        for (String arg : args) {
            byte[] bytes = arg.getBytes(StandardCharsets.UTF_8);
            buf.writeByte('$');
            buf.writeCharSequence(Integer.toString(bytes.length), StandardCharsets.US_ASCII);
            buf.writeBytes(CRLF);
            buf.writeBytes(bytes);
            buf.writeBytes(CRLF);
        }
    }
}