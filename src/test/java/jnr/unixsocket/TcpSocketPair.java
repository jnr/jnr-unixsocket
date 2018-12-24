package jnr.unixsocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

class TcpSocketPair extends TestSocketPair {
    static final Factory FACTORY = new Factory() {
        @Override
        TestSocketPair createUnconnected() {
            return new TcpSocketPair();
        }
    };

    private ServerSocketChannel serverSocketChannel;
    private InetSocketAddress serverAddress;
    private SocketChannel serverChannel;
    private SocketChannel clientChannel;

    @Override
    void connectBlocking() throws IOException {
        if (serverSocketChannel != null || serverChannel != null || clientChannel != null) {
            throw new IllegalStateException("already connected");
        }

        serverSocketChannel = ServerSocketChannel.open();
        ServerSocket serverSocket = serverSocketChannel.socket();
        serverSocket.setReuseAddress(true);
        serverSocketChannel.bind(new InetSocketAddress(0));
        serverSocketChannel.configureBlocking(true);
        serverAddress = new InetSocketAddress(serverSocket.getInetAddress(), serverSocket.getLocalPort());

        clientChannel = SocketChannel.open();
        clientChannel.connect(serverAddress);

        serverChannel = serverSocketChannel.accept();
    }

    @Override
    SocketAddress socketAddress() {
        return serverAddress;
    }

    @Override
    SocketChannel serverChannel() {
        return serverChannel;
    }

    @Override
    SocketChannel clientChannel() {
        return clientChannel;
    }

    @Override
    public void close() throws IOException {
        closeQuietly(serverSocketChannel);
        closeQuietly(serverChannel);
        closeQuietly(clientChannel);
    }
}
