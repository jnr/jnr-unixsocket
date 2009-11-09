
package com.kenai.jnr.unixsocket;

import com.kenai.constantine.platform.ProtocolFamily;
import com.kenai.constantine.platform.Sock;
import com.kenai.jaffl.byref.IntByReference;
import com.kenai.jnr.enxio.channels.NativeServerSocketChannel;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;

/**
 *
 */
public class UnixServerSocketChannel extends NativeServerSocketChannel {

    private final UnixServerSocket socket;

    UnixServerSocketChannel(UnixServerSocket socket) throws IOException {
        super(Native.socket(ProtocolFamily.PF_UNIX, Sock.SOCK_STREAM, 0));
        this.socket = new UnixServerSocket(this);
    }

    UnixServerSocketChannel(SelectorProvider provider, int fd) {
        super(provider, fd, SelectionKey.OP_ACCEPT);
        this.socket = new UnixServerSocket(this);
    }

    public static UnixServerSocketChannel open() throws IOException {
        return new UnixServerSocket().channel;
    }

    public UnixSocketChannel accept() throws IOException {
        UnixSocketAddress remote = new UnixSocketAddress();
        SockAddrUnix addr = remote.getStruct();
        IntByReference len = new IntByReference(addr.getMaximumLength());

        int clientfd = Native.accept(getFD(), addr, len);

        if (clientfd < 0) {
            throw new IOException("accept failed: " + Native.getLastErrorString());
        }

        // Always force the socket back to blocking mode
        Native.setBlocking(clientfd, true);

        return new UnixSocketChannel(clientfd, remote);
    }

    public final UnixServerSocket socket() {
        return socket;
    }
}
