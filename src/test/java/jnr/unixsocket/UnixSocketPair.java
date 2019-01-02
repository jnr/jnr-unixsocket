package jnr.unixsocket;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

class UnixSocketPair extends TestSocketPair {
    static final Factory FACTORY = new Factory() {
        @Override
        TestSocketPair createUnconnected() {
            return new UnixSocketPair();
        }
    };

    private final File file;
    private final UnixSocketAddress address;

    private UnixServerSocketChannel serverSocketChannel;
    private UnixSocketChannel serverChannel;
    private UnixSocketChannel clientChannel;

    UnixSocketPair() {
        file = new File("/tmp/jnr-unixsocket-test" + UUID.randomUUID() + ".sock");
        address = new UnixSocketAddress(file);
    }

    @Override
    void connectBlocking() throws IOException {
        if (serverSocketChannel != null || serverChannel != null || clientChannel != null) {
            throw new IllegalStateException("already connected");
        }

        serverSocketChannel = UnixServerSocketChannel.open();
        serverSocketChannel.configureBlocking(true);
        serverSocketChannel.socket().bind(address);

        clientChannel = UnixSocketChannel.open();
        clientChannel.connect(new UnixSocketAddress(file));

        serverChannel = serverSocketChannel.accept();
    }

    @Override
    UnixSocketAddress socketAddress() {
        return address;
    }

    @Override
    UnixSocketChannel serverChannel() {
        return serverChannel;
    }

    @Override
    UnixSocketChannel clientChannel() {
        return clientChannel;
    }

    @Override
    public void close() throws IOException {
        closeQuietly(serverSocketChannel);
        closeQuietly(serverChannel);
        closeQuietly(clientChannel);
        file.delete();
    }
}
