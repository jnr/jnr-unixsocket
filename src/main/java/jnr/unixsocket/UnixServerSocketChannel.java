/*
 * This file is part of the JNR project (http://jnr.kenai.com)
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package jnr.unixsocket;

import com.kenai.constantine.platform.ProtocolFamily;
import com.kenai.constantine.platform.Sock;
import com.kenai.jaffl.byref.IntByReference;
import jnr.enxio.channels.NativeServerSocketChannel;
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
