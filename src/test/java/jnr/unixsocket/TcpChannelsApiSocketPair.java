package jnr.unixsocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * TCP sockets created with the java.nio channels APIs.
 */
class TcpChannelsApiSocketPair extends TestSocketPair {
    static final Factory FACTORY = new Factory() {
        @Override
        TestSocketPair createUnconnected() throws IOException {
            return new TcpChannelsApiSocketPair();
        }
    };

    private final ServerSocketChannel serverSocketChannel;
    private InetSocketAddress serverAddress;
    private SocketChannel serverChannel;
    private SocketChannel clientChannel;

    TcpChannelsApiSocketPair() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
    }

    @Override
    void serverBind() throws IOException {
        if (serverAddress != null) {
            throw new IllegalStateException("already bound");
        }

        ServerSocket serverSocket = serverSocketChannel.socket();
        serverSocket.setReuseAddress(true);
        serverSocketChannel.bind(new InetSocketAddress(0));
        serverSocketChannel.configureBlocking(true);
        serverAddress = new InetSocketAddress(serverSocket.getInetAddress(), serverSocket.getLocalPort());
    }

    @Override
    void clientConnect() throws IOException {
        if (clientChannel != null) {
            throw new IllegalStateException("already connected");
        }

        clientChannel = SocketChannel.open();
        clientChannel.connect(serverAddress);
    }

    @Override
    void serverAccept() throws IOException {
        if (serverChannel != null) {
            throw new IllegalStateException("already accepted");
        }

        serverChannel = serverSocketChannel.accept();
    }

    @Override
    SocketAddress socketAddress() {
        return serverAddress;
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
    }
}
