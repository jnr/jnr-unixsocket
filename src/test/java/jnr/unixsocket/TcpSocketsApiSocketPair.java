package jnr.unixsocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * TCP sockets created with the java.io sockets APIs. The sockets returned by
 * this class do not have channels.
 */
class TcpSocketsApiSocketPair extends TestSocketPair {
    static final Factory FACTORY = new Factory() {
        @Override
        TestSocketPair createUnconnected() throws IOException {
            return new TcpSocketsApiSocketPair();
        }
    };

    private final ServerSocket serverSocket;
    private InetSocketAddress serverAddress;
    private Socket server;
    private Socket client;

    public TcpSocketsApiSocketPair() throws IOException {
        serverSocket = new ServerSocket();
    }

    @Override
    void serverBind() throws IOException {
        if (serverAddress != null) {
            throw new IllegalStateException("already bound");
        }

        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(0));
        serverAddress = new InetSocketAddress(serverSocket.getInetAddress(), serverSocket.getLocalPort());
    }

    @Override
    void clientConnect() throws IOException {
        if (client != null) {
            throw new IllegalStateException("already connected");
        }

        client = new Socket();
        client.connect(serverAddress);
    }

    @Override
    void serverAccept() throws IOException {
        if (server != null) {
            throw new IllegalStateException("already accepted");
        }

        server = serverSocket.accept();
    }

    @Override
    SocketAddress socketAddress() {
        return serverAddress;
    }

    @Override
    Socket server() {
        return server;
    }

    @Override
    Socket client() {
        return client;
    }

    @Override
    public void close() throws IOException {
        closeQuietly(serverSocket);
        closeQuietly(server);
        closeQuietly(client);
    }
}
