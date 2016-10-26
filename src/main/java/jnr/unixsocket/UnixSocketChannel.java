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
import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;
import jnr.enxio.channels.NativeSocketChannel;
import jnr.ffi.*;
import jnr.ffi.byref.IntByReference;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static jnr.unixsocket.SockAddrUnix.HEADER_LENGTH;

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
    private State state;
    private UnixSocketAddress remoteAddress = null;
    private UnixSocketAddress localAddress = null;
    private final ReadWriteLock stateLock = new ReentrantReadWriteLock();

    public static final UnixSocketChannel open() throws IOException {
        return new UnixSocketChannel();
    }

    public static final UnixSocketChannel open(UnixSocketAddress remote) throws IOException {
        UnixSocketChannel channel = new UnixSocketChannel();
        try {
            channel.connect(remote);
        } catch (IOException e) {
            channel.close();
            throw e;
        }
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

    /**
     * Create a UnixSocketChannel to wrap an existing file descriptor (presumably itself a UNIX socket).
     *
     * @param fd the file descriptor to wrap
     * @return the new UnixSocketChannel instance
     */
    public static final UnixSocketChannel fromFD(int fd) {
        return fromFD(fd, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    /**
     * Create a UnixSocketChannel to wrap an existing file descriptor (presumably itself a UNIX socket).
     *
     * @param fd the file descriptor to wrap
     * @param ops the SelectionKey operations the socket supports
     * @return the new UnixSocketChannel instance
     */
    public static final UnixSocketChannel fromFD(int fd, int ops) {
        return new UnixSocketChannel(fd, ops);
    }

    private UnixSocketChannel() throws IOException {
        super(Native.socket(ProtocolFamily.PF_UNIX, Sock.SOCK_STREAM, 0),
                SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        stateLock.writeLock().lock();
        state = State.IDLE;
        stateLock.writeLock().unlock();
    }

    UnixSocketChannel(int fd, int ops) {
        super(fd, ops);
        stateLock.writeLock().lock();
        state = State.CONNECTED;
        stateLock.writeLock().unlock();
    }

    UnixSocketChannel(int fd, UnixSocketAddress remote) {
        super(fd, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        stateLock.writeLock().lock();
        state = State.CONNECTED;
        stateLock.writeLock().unlock();
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

            stateLock.writeLock().lock();
            state = State.CONNECTING;
            stateLock.writeLock().unlock();
            return false;

        } else {

            stateLock.writeLock().lock();
            state = State.CONNECTED;
            stateLock.writeLock().unlock();
            return true;
        }
    }

    public boolean isConnected() {
        stateLock.readLock().lock();
        boolean isConnected = state == State.CONNECTED;
        stateLock.readLock().lock();
        return isConnected;
    }

    public boolean isConnectionPending() {
        stateLock.readLock().lock();
        boolean isConnectionPending = state == State.CONNECTING;
        stateLock.readLock().lock();
        return isConnectionPending;
    }

    public boolean finishConnect() throws IOException {
        stateLock.writeLock().lock();
        try {
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
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    public final UnixSocketAddress getRemoteSocketAddress() {
        if (!isConnected()) {
            return null;
        }
        return remoteAddress != null ? remoteAddress : (remoteAddress = getpeername(getFD()));
    }

    public final UnixSocketAddress getLocalSocketAddress() {
        if (!isConnected()) {
            return null;
        }

        return localAddress != null ? localAddress : (localAddress = getsockname(getFD()));
    }

    /**
     * Retrieves the credentials for this UNIX socket. If this socket channel
     * is not in a connected state, this method will return null.
     *
     * See man unix 7; SCM_CREDENTIALS
     *
     * @throws UnsupportedOperationException if the underlying socket library
     *         doesn't support the SO_PEERCRED option
     *
     * @return the credentials of the remote; null if not connected
     */
    public final Credentials getCredentials() {
        if (!isConnected()) {
            return null;
        }

        return Credentials.getCredentials(getFD());
    }

    static UnixSocketAddress getpeername(int sockfd) {
        UnixSocketAddress remote = new UnixSocketAddress();
        SockAddrUnix addr = remote.getStruct();
        IntByReference len = new IntByReference(addr.getMaximumLength());

        if (Native.libc().getpeername(sockfd, addr, len) < 0) {
            throw new Error(Native.getLastErrorString());
        }

        // Handle unnamed sockets
        if (len.getValue() == addr.getHeaderLength()) addr.setPath("");

        return remote;
    }

    static UnixSocketAddress getsockname(int sockfd) {
        UnixSocketAddress remote = new UnixSocketAddress();
        SockAddrUnix addr = remote.getStruct();
        int maxLength = addr.getMaximumLength();
        IntByReference len = new IntByReference(addr.getMaximumLength());

        if (Native.libc().getsockname(sockfd, addr, len) < 0) {
            throw new Error(Native.getLastErrorString());
        }

        int headerLength = addr.getHeaderLength();
        if (len.getValue() == headerLength) {
            addr.setPath("");
        } else if (len.getValue() < maxLength) {            /* On MacOS (perhaps others), we get minimum length */
            String path = addr.getPath();                   /* of 14 for "" path vs 2 on linux.  Feels like FFI */
            int newLength = len.getValue() - headerLength;  /* is not handling char * for us right? If a longer */
            if (newLength < path.length()) {                /* path it seems to record proper length? truncate  */
                addr.setPath(path.substring(0, len.getValue() - headerLength));  /* to it then.                 */
            }
        }

        return remote;
    }

    public boolean getKeepAlive() {
        int ret = Native.getsockopt(getFD(), SocketLevel.SOL_SOCKET, SocketOption.SO_KEEPALIVE.intValue());
        return (ret == 1) ? true : false;
    }

    public void setKeepAlive(boolean on) {
        Native.setsockopt(getFD(), SocketLevel.SOL_SOCKET, SocketOption.SO_KEEPALIVE, on);
    }

    public int getSoTimeout() {
        return Native.getsockopt(getFD(), SocketLevel.SOL_SOCKET, SocketOption.SO_RCVTIMEO.intValue());
    }

    public void setSoTimeout(int timeout) {
        Native.setsockopt(getFD(), SocketLevel.SOL_SOCKET, SocketOption.SO_RCVTIMEO, timeout);
    }
}
