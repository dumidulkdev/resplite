package dev.dumidu.resplite.persistence;

import dev.dumidu.resplite.store.Keyspace;
import dev.dumidu.resplite.store.StoredEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class AofRewriter {
    private AofRewriter() {
    }

    public static void rewrite(Path aofPath, Keyspace keyspace) throws IOException {
        Path tmp = aofPath.resolveSibling(aofPath.getFileName() + ".rewrite.tmp");
        try (AofWriter writer = new AofWriter(tmp, dev.dumidu.resplite.config.AofFsyncPolicy.ALWAYS)) {
            long now = keyspace.nowMs();
            keyspace.forEachEntry((key, entry) -> {
                if (entry.isExpiredAt(now)) {
                    return;
                }
                try {
                    List<String> set = new ArrayList<>();
                    set.add("SET");
                    set.add(key);
                    set.add(new String(entry.value(), StandardCharsets.UTF_8));
                    writer.append(set);
                    if (entry.hasExpiry()) {
                        writer.append(List.of("EXPIREAT", key, Long.toString(entry.expireAtEpochMs())));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        Files.move(tmp, aofPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}