package dev.dumidu.resplite.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class RespLiteBenchmark {
    private RespLiteBenchmark() {
    }

    public record Result(double opsPerSec, long p50Nanos, long p99Nanos) { }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 6380;
        int clients = args.length > 2 ? Integer.parseInt(args[2]) : 8;
        int requests = args.length > 3 ? Integer.parseInt(args[3]) : 20_000;
        Result r = run(host, port, clients, requests);
        System.out.printf("clients=%d requests=%d ops/sec=%.0f p50_us=%d p99_us=%d%n",
                clients, requests * clients, r.opsPerSec(), r.p50Nanos() / 1000, r.p99Nanos() / 1000);
    }

    public static Result run(String host, int port, int clients, int totalPerClient) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(clients);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(clients);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        long wallStart = System.nanoTime();
        for (int c = 0; c < clients; c++) {
            final int id = c;
            pool.submit(() -> {
                try (Socket socket = new Socket(host, port)) {
                    socket.setTcpNoDelay(true);
                    InputStream in = socket.getInputStream();
                    OutputStream out = socket.getOutputStream();
                    start.await();
                    for (int i = 0; i < totalPerClient; i++) {
                        String key = "bench:" + id + ":" + i;
                        long t0 = System.nanoTime();
                        writeSet(out, key, "v");
                        readUntilCrlfLine(in);
                        writeGet(out, key);
                        readBulkReply(in);
                        latencies.add(System.nanoTime() - t0);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await(120, TimeUnit.SECONDS);
        pool.shutdownNow();
        long elapsed = System.nanoTime() - wallStart;
        long ops = (long) clients * totalPerClient * 2;
        double opsPerSec = ops * 1_000_000_000.0 / elapsed;
        latencies.sort(Long::compareTo);
        long p50 = latencies.isEmpty() ? 0 : latencies.get(latencies.size() / 2);
        long p99 = latencies.isEmpty() ? 0 : latencies.get(Math.min(latencies.size() - 1, (int) (latencies.size() * 0.99)));
        return new Result(opsPerSec, p50, p99);
    }

    private static void writeSet(OutputStream out, String key, String value) throws IOException {
        String cmd = "*3\r\n$3\r\nSET\r\n$" + key.length() + "\r\n" + key + "\r\n$" + value.length()
                + "\r\n" + value + "\r\n";
        out.write(cmd.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static void writeGet(OutputStream out, String key) throws IOException {
        String cmd = "*2\r\n$3\r\nGET\r\n$" + key.length() + "\r\n" + key + "\r\n";
        out.write(cmd.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static void readUntilCrlfLine(InputStream in) throws IOException {
        int prev = -1;
        while (true) {
            int b = in.read();
            if (b < 0) {
                throw new IOException("EOF");
            }
            if (prev == '\r' && b == '\n') {
                return;
            }
            prev = b;
        }
    }

    private static void readBulkReply(InputStream in) throws IOException {
        int type = in.read();
        if (type == '$') {
            readUntilCrlfLine(in);
            readUntilCrlfLine(in);
        } else if (type == '+') {
            readUntilCrlfLine(in);
        }
    }
}