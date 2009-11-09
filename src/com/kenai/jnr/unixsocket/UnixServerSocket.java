
package com.kenai.jnr.unixsocket;

import java.io.IOException;
import java.net.SocketAddress;

public class UnixServerSocket {
    final UnixServerSocketChannel channel;
    final int fd;

    public UnixServerSocket() throws IOException {
        this.channel = new UnixServerSocketChannel(this);
        this.fd = channel.getFD();
    }

    UnixServerSocket(UnixServerSocketChannel channel) {
        this.channel = channel;
        this.fd = channel.getFD();
    }

    public UnixSocket accept() throws IOException {
        return new UnixSocket(channel.accept());
    }

    public void bind(SocketAddress endpoint) throws java.io.IOException {
        bind(endpoint, 128);
    }

    public void bind(SocketAddress endpoint, int backlog) throws java.io.IOException {
        if (!(endpoint instanceof UnixSocketAddress)) {
            throw new IOException("Invalid address");
        }
        UnixSocketAddress addr = (UnixSocketAddress) endpoint;

        if (Native.bind(fd, addr.getStruct(), addr.length()) < 0) {
            throw new IOException("bind failed: " + Native.getLastErrorString());
        }

        if (Native.listen(fd, backlog) < 0) {
            throw new IOException("listen failed: " + Native.getLastErrorString());
        }
    }
    
}
