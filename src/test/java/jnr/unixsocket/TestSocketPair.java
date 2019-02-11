package jnr.unixsocket;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

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

    abstract Socket server();

    abstract Socket client();

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
