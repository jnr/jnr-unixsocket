/*
 * Copyright (C) 2009 Wayne Meissner
 *
 * This file is part of the JNR project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jnr.unixsocket;

import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.enxio.channels.NativeServerSocketChannel;
import jnr.ffi.byref.IntByReference;

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
        super(provider, fd, SelectionKey.OP_ACCEPT | SelectionKey.OP_READ);
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

    public final UnixSocketAddress getRemoteSocketAddress() {
        return null;
    }

    public final UnixSocketAddress getLocalSocketAddress() {
        return socket.localAddress;
    }
}
