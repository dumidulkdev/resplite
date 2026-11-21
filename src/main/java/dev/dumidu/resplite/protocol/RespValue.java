package dev.dumidu.resplite.protocol;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public sealed interface RespValue
        permits RespValue.SimpleString, RespValue.ErrorReply, RespValue.IntegerValue,
                RespValue.BulkString, RespValue.ArrayValue {

    record SimpleString(String value) implements RespValue {
        @Override
        public String toString() {
            return value;
        }
    }

    record ErrorReply(String message) implements RespValue {
        public ErrorReply {
            if (message != null && !message.startsWith("ERR")) {
                message = "ERR " + message;
            }
        }
    }

    record IntegerValue(long value) implements RespValue { }

    record BulkString(byte[] content) implements RespValue {
        public static BulkString nullBulk() {
            return new BulkString(null);
        }

        public boolean isNull() {
            return content == null;
        }

        public String asUtf8() {
            return content == null ? null : new String(content, StandardCharsets.UTF_8);
        }

        public static BulkString ofUtf8(String s) {
            return s == null ? nullBulk() : new BulkString(s.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BulkString(byte[] that))) return false;
            return Arrays.equals(content, that);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(content);
        }
    }

    record ArrayValue(List<RespValue> elements) implements RespValue {
        public ArrayValue {
            elements = elements == null ? List.of() : List.copyOf(elements);
        }
    }

    static SimpleString ok(String msg) {
        return new SimpleString(msg);
    }

    static ErrorReply err(String msg) {
        return new ErrorReply(msg);
    }

    static IntegerValue integer(long v) {
        return new IntegerValue(v);
    }
}