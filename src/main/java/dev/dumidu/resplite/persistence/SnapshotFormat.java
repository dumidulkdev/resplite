package dev.dumidu.resplite.persistence;

import dev.dumidu.resplite.store.Keyspace;
import dev.dumidu.resplite.store.StoredEntry;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

public final class SnapshotFormat {
    public static final byte[] MAGIC = {'R', 'D', 'B', 'L'};
    public static final int VERSION = 1;

    private SnapshotFormat() {
    }

    public static void write(Path target, Keyspace keyspace) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        Map<String, StoredEntry> data = keyspace.snapshot();
        long now = keyspace.nowMs();
        int count = 0;
        for (var e : data.entrySet()) {
            if (!e.getValue().isExpiredAt(now)) {
                count++;
            }
        }
        try (var bodyOut = new DataOutputStream(body)) {
            bodyOut.writeInt(count);
            for (var e : data.entrySet()) {
                StoredEntry entry = e.getValue();
                if (entry.isExpiredAt(now)) {
                    continue;
                }
                writeString(bodyOut, e.getKey());
                writeBytes(bodyOut, entry.value());
                bodyOut.writeLong(entry.expireAtEpochMs());
            }
        }
        byte[] bodyBytes = body.toByteArray();
        CRC32 crc = new CRC32();
        crc.update(bodyBytes);

        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try (var out = new DataOutputStream(Files.newOutputStream(tmp))) {
            out.write(MAGIC);
            out.writeInt(VERSION);
            out.writeLong(crc.getValue());
            out.write(bodyBytes);
        }
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void load(Path source, Keyspace keyspace) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        byte[] raw = Files.readAllBytes(source);
        try (var in = new DataInputStream(new java.io.ByteArrayInputStream(raw))) {
            byte[] magic = in.readNBytes(4);
            if (!java.util.Arrays.equals(magic, MAGIC)) {
                throw new IOException("invalid snapshot magic");
            }
            int version = in.readInt();
            if (version != VERSION) {
                throw new IOException("unsupported snapshot version");
            }
            long expectedCrc = in.readLong();
            byte[] body = in.readAllBytes();
            CRC32 crc = new CRC32();
            crc.update(body);
            if (crc.getValue() != expectedCrc) {
                throw new IOException("snapshot checksum mismatch");
            }
            try (var bodyIn = new DataInputStream(new java.io.ByteArrayInputStream(body))) {
                int count = bodyIn.readInt();
                Map<String, StoredEntry> entries = new HashMap<>();
                long now = keyspace.nowMs();
                for (int i = 0; i < count; i++) {
                    String key = readString(bodyIn);
                    byte[] value = readBytes(bodyIn);
                    long expireAt = bodyIn.readLong();
                    if (expireAt > 0 && expireAt <= now) {
                        continue;
                    }
                    entries.put(key, new StoredEntry(value, expireAt));
                }
                keyspace.restore(entries);
            }
        }
    }

    private static void writeString(DataOutputStream out, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(b.length);
        out.write(b);
    }

    private static void writeBytes(DataOutputStream out, byte[] b) throws IOException {
        out.writeInt(b.length);
        out.write(b);
    }

    private static String readString(DataInputStream in) throws IOException {
        int len = in.readInt();
        byte[] b = in.readNBytes(len);
        return new String(b, StandardCharsets.UTF_8);
    }

    private static byte[] readBytes(DataInputStream in) throws IOException {
        int len = in.readInt();
        return in.readNBytes(len);
    }
}