package jnr.unixsocket;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

class UnixSocketPair extends TestSocketPair {
    static final Factory FACTORY = new Factory() {
        @Override
        TestSocketPair createUnconnected() throws IOException {
            return new UnixSocketPair();
        }
    };

    private final File file;
    private final UnixSocketAddress address;

    private UnixServerSocketChannel serverSocketChannel;
    private UnixSocketChannel serverChannel;
    private UnixSocketChannel clientChannel;

    UnixSocketPair() throws IOException {
        file = new File("/tmp/jnr-unixsocket-test" + UUID.randomUUID() + ".sock");
        address = new UnixSocketAddress(file);
        serverSocketChannel = UnixServerSocketChannel.open();
    }

    @Override
    void serverBind() throws IOException {
        serverSocketChannel.configureBlocking(true);
        serverSocketChannel.socket().bind(address);
    }

    @Override
    void clientConnect() throws IOException {
        if (clientChannel != null) {
            throw new IllegalStateException("already connected");
        }

        clientChannel = UnixSocketChannel.open();
        clientChannel.connect(new UnixSocketAddress(file));
    }

    @Override
    void serverAccept() throws IOException {
        if (serverChannel != null) {
            throw new IllegalStateException("already accepted");
        }

        serverChannel = serverSocketChannel.accept();
    }

    @Override
    UnixSocketAddress socketAddress() {
        return address;
    }

    @Override
    Socket server() {
        return serverChannel.socket();
    }

    @Override
    Socket client() {
        return clientChannel.socket();
    }

    @Override
    public void close() throws IOException {
        closeQuietly(serverSocketChannel);
        closeQuietly(serverChannel);
        closeQuietly(clientChannel);
        file.delete();
    }
}
