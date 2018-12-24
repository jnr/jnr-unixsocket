package jnr.unixsocket;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

/**
 * A TCP or UNIX socket pair for testing.
 */
abstract class TestSocketPair implements Closeable {
    abstract void connectBlocking() throws IOException;

    abstract SocketAddress socketAddress();

    abstract SocketChannel serverChannel();

    abstract SocketChannel clientChannel();

    final void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

    abstract static class Factory {
        abstract TestSocketPair createUnconnected();
    }
}
