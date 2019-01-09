package jnr.unixsocket;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;

/**
 * A TCP or UNIX socket pair for testing.
 */
abstract class TestSocketPair implements Closeable {
    void connectBlocking() throws IOException {
        serverBind();
        clientConnect();
        serverAccept();
    }

    abstract void serverBind() throws IOException;

    abstract void serverAccept() throws IOException;

    abstract void clientConnect() throws IOException;

    abstract SocketAddress socketAddress();

    abstract SelectableChannel serverSocketChannel();

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
        abstract TestSocketPair createUnconnected() throws IOException;
    }
}
