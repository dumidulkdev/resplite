package dev.dumidu.resplite.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Decodes one or more complete RESP values per channel read. Incomplete frames wait for more bytes.
 * Inline telnet-style commands (no leading type byte) are parsed as bulk-string arrays.
 */
public class RespDecoder extends ByteToMessageDecoder {

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    while (in.isReadable()) {
      in.markReaderIndex();
      byte first = in.getByte(in.readerIndex());
      if (first != '+' && first != '-' && first != ':' && first != '$' && first != '*') {
        RespValue inline = tryReadInline(in);
        if (inline == null) {
          in.resetReaderIndex();
          return;
        }
        out.add(inline);
        continue;
      }
      try {
        RespValue value = readValue(in);
        if (value == null) {
          in.resetReaderIndex();
          return;
        }
        out.add(value);
      } catch (RespDecodeException e) {
        in.resetReaderIndex();
        out.add(RespValue.err("ERR protocol error: " + e.getMessage()));
        in.skipBytes(in.readableBytes());
        return;
      }
    }
  }

  private RespValue tryReadInline(ByteBuf in) {
    int lineEnd = findLineEnd(in);
    if (lineEnd < 0) {
      return null;
    }
    int len = lineEnd - in.readerIndex();
    if (len == 0) {
      skipLine(in);
      return null;
    }
    String line = in.toString(in.readerIndex(), len, StandardCharsets.UTF_8);
    skipLine(in);
    String[] parts = line.trim().split("\\s+");
    List<RespValue> bulks = new ArrayList<>();
    for (String p : parts) {
      bulks.add(RespValue.BulkString.ofUtf8(p));
    }
    return new RespValue.ArrayValue(bulks);
  }

  public RespValue readValue(ByteBuf in) throws RespDecodeException {
    if (!in.isReadable()) {
      return null;
    }
    in.markReaderIndex();
    byte type = in.readByte();
    RespValue value = switch (type) {
      case '+' -> readSimple(in);
      case '-' -> readError(in);
      case ':' -> readInteger(in);
      case '$' -> readBulk(in);
      case '*' -> readArray(in);
      default -> throw new RespDecodeException("unknown RESP type byte");
    };
    if (value == null) {
      in.resetReaderIndex();
    }
    return value;
  }

  private RespValue readSimple(ByteBuf in) throws RespDecodeException {
    String line = readLine(in);
    if (line == null) {
      return null;
    }
    return new RespValue.SimpleString(line);
  }

  private RespValue readError(ByteBuf in) throws RespDecodeException {
    String line = readLine(in);
    if (line == null) {
      return null;
    }
    return new RespValue.ErrorReply(line);
  }

  private RespValue readInteger(ByteBuf in) throws RespDecodeException {
    String line = readLine(in);
    if (line == null) {
      return null;
    }
    try {
      return new RespValue.IntegerValue(Long.parseLong(line.trim()));
    } catch (NumberFormatException e) {
      throw new RespDecodeException("invalid integer");
    }
  }

  private RespValue readBulk(ByteBuf in) throws RespDecodeException {
    int bulkContentStart = in.readerIndex();
    String line = readLine(in);
    if (line == null) {
      return null;
    }
    int len;
    try {
      len = Integer.parseInt(line.trim());
    } catch (NumberFormatException e) {
      throw new RespDecodeException("invalid bulk length");
    }
    if (len == -1) {
      return RespValue.BulkString.nullBulk();
    }
    if (len < -1) {
      throw new RespDecodeException("invalid bulk length");
    }
    if (in.readableBytes() < len + 2) {
      in.readerIndex(bulkContentStart);
      return null;
    }
    byte[] data = new byte[len];
    in.readBytes(data);
    if (!consumeCrlf(in)) {
      throw new RespDecodeException("expected CRLF after bulk");
    }
    return new RespValue.BulkString(data);
  }

  private RespValue readArray(ByteBuf in) throws RespDecodeException {
    String line = readLine(in);
    if (line == null) {
      return null;
    }
    int count;
    try {
      count = Integer.parseInt(line.trim());
    } catch (NumberFormatException e) {
      throw new RespDecodeException("invalid array length");
    }
    if (count < 0) {
      throw new RespDecodeException("invalid array length");
    }
    if (count == 0) {
      return new RespValue.ArrayValue(List.of());
    }
    List<RespValue> items = new ArrayList<>(count);
    int startIdx = in.readerIndex();
    for (int i = 0; i < count; i++) {
      RespValue child = readValue(in);
      if (child == null) {
        in.readerIndex(startIdx);
        return null;
      }
      items.add(child);
    }
    return new RespValue.ArrayValue(items);
  }

  private String readLine(ByteBuf in) throws RespDecodeException {
    int end = findLineEnd(in);
    if (end < 0) {
      return null;
    }
    String line = in.toString(in.readerIndex(), end - in.readerIndex(), StandardCharsets.UTF_8);
    skipLine(in);
    if (line.endsWith("\r")) {
      line = line.substring(0, line.length() - 1);
    }
    if (line.indexOf('\0') >= 0) {
      throw new RespDecodeException("invalid character in line");
    }
    return line;
  }

  private int findLineEnd(ByteBuf in) {
    for (int i = in.readerIndex(); i < in.writerIndex(); i++) {
      byte b = in.getByte(i);
      if (b == '\n') {
        return i;
      }
    }
    return -1;
  }

  private void skipLine(ByteBuf in) {
    while (in.isReadable()) {
      byte b = in.readByte();
      if (b == '\n') {
        break;
      }
    }
  }

  private boolean consumeCrlf(ByteBuf in) {
    if (in.readableBytes() < 2) {
      return false;
    }
    byte a = in.getByte(in.readerIndex());
    byte b = in.getByte(in.readerIndex() + 1);
    if (a == '\r' && b == '\n') {
      in.skipBytes(2);
      return true;
    }
    if (a == '\n') {
      in.skipBytes(1);
      return true;
    }
    return false;
  }
}