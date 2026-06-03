package dev.saseq;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wraps process stdin so the JVM terminates as soon as the MCP client detaches.
 *
 * <p>The stdio transport reads {@code System.in}; when the parent process (e.g.
 * {@code docker run -i}) closes the pipe, that read returns EOF and the MCP
 * session ends. JDA's gateway threads are non-daemon, however, so the JVM keeps
 * running indefinitely after the client is gone. Under {@code docker run --rm}
 * that leaves the container alive forever — it is never reaped because the
 * process never exits — and successive client reconnects orphan a fresh
 * container each time.
 *
 * <p>Detecting EOF on the same stream the transport reads (without consuming any
 * payload bytes) lets us shut the JVM down the moment the client disconnects, so
 * the container exits and {@code --rm} cleans it up.
 */
public final class ExitOnEofInputStream extends FilterInputStream {
    private final AtomicBoolean fired = new AtomicBoolean(false);
    private final Runnable onEof;

    public ExitOnEofInputStream(InputStream in) {
        this(in, () -> {
            // Exit off the reader thread so the transport can unwind and any
            // shutdown hooks run without re-entering this call.
            Thread t = new Thread(() -> System.exit(0), "stdin-eof-exit");
            t.setDaemon(true);
            t.start();
        });
    }

    ExitOnEofInputStream(InputStream in, Runnable onEof) {
        super(in);
        this.onEof = onEof;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b == -1) {
            fireOnce();
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n == -1) {
            fireOnce();
        }
        return n;
    }

    private void fireOnce() {
        if (fired.compareAndSet(false, true)) {
            onEof.run();
        }
    }
}
