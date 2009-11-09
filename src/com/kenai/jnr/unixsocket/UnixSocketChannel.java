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

package com.kenai.jnr.unixsocket;

import com.kenai.constantine.platform.Errno;
import com.kenai.constantine.platform.ProtocolFamily;
import com.kenai.constantine.platform.Sock;
import com.kenai.jaffl.LastError;
import com.kenai.jaffl.byref.IntByReference;
import com.kenai.jnr.enxio.channels.NativeSocketChannel;
import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * A {@link java.nio.channels.Channel} implementation that uses a native unix socket
 */
public class UnixSocketChannel extends NativeSocketChannel {
    static enum State {
        UNINITIALIZED,
        CONNECTED,
        IDLE,
        CONNECTING,
    }
    private volatile State state;
    private UnixSocketAddress remoteAddress = null;
    private UnixSocketAddress localAddress = null;
    private final Object stateLock = new Object();

    public static final UnixSocketChannel open() throws IOException {
        return new UnixSocketChannel();
    }

    public static final UnixSocketChannel open(UnixSocketAddress remote) throws IOException {
        UnixSocketChannel channel = new UnixSocketChannel();
        channel.connect(remote);
        return channel;
    }

    private UnixSocketChannel() throws IOException {
        super(Native.socket(ProtocolFamily.PF_UNIX, Sock.SOCK_STREAM, 0),
                SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        state = State.IDLE;
    }

    UnixSocketChannel(int fd, int ops) {
        super(fd, ops);
        state = State.CONNECTED;
    }

    UnixSocketChannel(int fd, UnixSocketAddress remote) {
        super(fd, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        state = State.CONNECTED;
        remoteAddress = remote;
    }

    private final boolean doConnect(SockAddrUnix remote) throws IOException {
        if (Native.connect(getFD(), remote, remote.length()) != 0) {
            Errno error = Errno.valueOf(LastError.getLastError());

            switch (error) {
                case EAGAIN:
                case EWOULDBLOCK:
                    return false;

                default:
                    throw new IOException(error.toString());
            }
        }
        return true;
    }

    public boolean connect(UnixSocketAddress remote) throws IOException {
        remoteAddress = remote;
        if (!doConnect(remoteAddress.getStruct())) {

            state = State.CONNECTING;
            return false;
        
        } else {

            state = State.CONNECTED;
            return true;
        }
    }

    public boolean isConnected() {
        return state == State.CONNECTED;
    }

    public boolean isConnectionPending() {
        return state == State.CONNECTING;
    }

    public boolean finishConnect() throws IOException {
        switch (state) {
            case CONNECTED:
                return true;

            case CONNECTING:
                if (!doConnect(remoteAddress.getStruct())) {
                    return false;
                }
                state = State.CONNECTED;
                return true;

            default:
                throw new IllegalStateException("socket is not waiting for connect to complete");
        }
    }

    public final UnixSocketAddress getRemoteSocketAddress() {
        if (state != State.CONNECTED) {
            return null;
        }
        return remoteAddress != null ? remoteAddress : (remoteAddress = getpeername(getFD()));
    }

    public final UnixSocketAddress getLocalSocketAddress() {
        if (state != State.CONNECTED) {
            return null;
        }

        return localAddress != null ? localAddress : (localAddress = getsockname(getFD()));
    }

    static UnixSocketAddress getpeername(int sockfd) {
        UnixSocketAddress remote = new UnixSocketAddress();
        IntByReference len = new IntByReference(remote.getStruct().getMaximumLength());

        if (Native.libc().getpeername(sockfd, remote.getStruct(), len) < 0) {
            throw new Error(Native.getLastErrorString());
        }

        return remote;
    }

    static UnixSocketAddress getsockname(int sockfd) {
        UnixSocketAddress remote = new UnixSocketAddress();
        IntByReference len = new IntByReference(remote.getStruct().getMaximumLength());

        if (Native.libc().getsockname(sockfd, remote.getStruct(), len) < 0) {
            throw new Error(Native.getLastErrorString());
        }

        return remote;
    }
}
