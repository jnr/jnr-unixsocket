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

import jnr.constants.platform.Errno;
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.enxio.channels.NativeSocketChannel;
import jnr.ffi.*;
import jnr.ffi.byref.IntByReference;

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

    public static final UnixSocketChannel[] pair() throws IOException {
        int[] sockets = { -1, -1 };
        Native.socketpair(ProtocolFamily.PF_UNIX, Sock.SOCK_STREAM, 0, sockets);
        return new UnixSocketChannel[] {
            new UnixSocketChannel(sockets[0], SelectionKey.OP_READ | SelectionKey.OP_WRITE),
            new UnixSocketChannel(sockets[1], SelectionKey.OP_READ | SelectionKey.OP_WRITE)
        };
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
            Errno error = Errno.valueOf(LastError.getLastError(jnr.ffi.Runtime.getSystemRuntime()));

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
