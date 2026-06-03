package dev.saseq;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ExitOnEofInputStreamTest {

    @Test
    void firesOnceWhenStreamReachesEof() throws IOException {
        AtomicInteger fired = new AtomicInteger(0);
        InputStream in = new ExitOnEofInputStream(
                new ByteArrayInputStream("hi".getBytes(StandardCharsets.UTF_8)),
                fired::incrementAndGet);

        assertThat(in.read()).isEqualTo('h');
        assertThat(in.read()).isEqualTo('i');
        assertThat(fired.get()).isZero();      // payload bytes must not trigger exit

        assertThat(in.read()).isEqualTo(-1);    // EOF
        assertThat(in.read()).isEqualTo(-1);    // still EOF on subsequent reads
        assertThat(fired.get()).isEqualTo(1);   // but the exit hook runs exactly once
    }

    @Test
    void firesOnEofViaBufferRead() throws IOException {
        AtomicInteger fired = new AtomicInteger(0);
        InputStream in = new ExitOnEofInputStream(
                new ByteArrayInputStream(new byte[0]),
                fired::incrementAndGet);

        byte[] buf = new byte[8];
        assertThat(in.read(buf, 0, buf.length)).isEqualTo(-1);
        assertThat(fired.get()).isEqualTo(1);
    }

    @Test
    void passesPayloadThroughViaBufferRead() throws IOException {
        AtomicInteger fired = new AtomicInteger(0);
        InputStream in = new ExitOnEofInputStream(
                new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8)),
                fired::incrementAndGet);

        byte[] buf = new byte[7];
        int n = in.read(buf, 0, buf.length);
        assertThat(n).isEqualTo(7);
        assertThat(new String(buf, 0, n, StandardCharsets.UTF_8)).isEqualTo("payload");
        assertThat(fired.get()).isZero();
    }
}
