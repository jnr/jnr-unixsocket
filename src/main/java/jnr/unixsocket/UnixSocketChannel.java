/*
 * Copyright (C) 2009 Wayne Meissner
 * Copyright (C) 2016 Marcus Linke
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

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jnr.constants.platform.Errno;
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.unixsocket.impl.AbstractNativeSocketChannel;
import jnr.ffi.LastError;

/**
 * A {@link java.nio.channels.Channel} implementation that uses a native unix
 * socket
 */
public class UnixSocketChannel extends AbstractNativeSocketChannel {
    enum State {
        UNINITIALIZED,
        CONNECTED,
        IDLE,
        CONNECTING,
    }

    private State state;
    private UnixSocketAddress remoteAddress = null;
    private UnixSocketAddress localAddress = null;
    private final ReadWriteLock stateLock = new ReentrantReadWriteLock();
    private final BindHandler bindHandler;

    public static final UnixSocketChannel open() throws IOException {
        return new UnixSocketChannel();
    }

    public static final UnixSocketChannel open(UnixSocketAddress remote)
            throws IOException {
        UnixSocketChannel channel = new UnixSocketChannel();

        try {
            channel.connect(remote);
        } catch (IOException e) {
            channel.close();
            throw e;
        }
        return channel;
    }

    public static final UnixSocketChannel create() throws IOException {
        return new UnixSocketChannel();
    }

    public static final UnixSocketChannel[] pair() throws IOException {
        int[] sockets = { -1, -1 };
        Native.socketpair(ProtocolFamily.PF_UNIX, Sock.SOCK_STREAM, 0, sockets);
        return new UnixSocketChannel[] {
                new UnixSocketChannel(sockets[0], State.CONNECTED, true),
                new UnixSocketChannel(sockets[1], State.CONNECTED, true) };
    }

    /**
     * Create a UnixSocketChannel to wrap an existing file descriptor
     * (presumably itself a UNIX socket).
     *
     * @param fd
     *            the file descriptor to wrap
     * @return the new UnixSocketChannel instance
     */
    public static final UnixSocketChannel fromFD(int fd) {
        return new UnixSocketChannel(fd);
    }

    UnixSocketChannel() throws IOException {
        this(Native.socket(ProtocolFamily.PF_UNIX, Sock.SOCK_STREAM, 0));
    }

    UnixSocketChannel(int fd) {
        this(fd, State.CONNECTED, false);
    }

    UnixSocketChannel(int fd, State initialState, boolean initialBoundState) {
        super(fd);
        stateLock.writeLock().lock();
        try {
            state = initialState;
            bindHandler = new BindHandler(initialBoundState);
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    private boolean doConnect(SockAddrUnix remote) throws IOException {
        if (Native.connect(getFD(), remote, remote.length()) != 0) {
            Errno error = Errno.valueOf(LastError.getLastError(jnr.ffi.Runtime
                    .getSystemRuntime()));

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

    boolean isBound() {
        return bindHandler.isBound();
    }

    public boolean isConnected() {
        stateLock.readLock().lock();
        boolean result = state == State.CONNECTED;
        stateLock.readLock().unlock();
        return result;
    }

    private boolean isIdle() {
        stateLock.readLock().lock();
        boolean result = state == State.IDLE;
        stateLock.readLock().unlock();
        return result;
    }

    public boolean isConnectionPending() {
        stateLock.readLock().lock();
        boolean isConnectionPending = state == State.CONNECTING;
        stateLock.readLock().unlock();
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
                throw new IllegalStateException(
                        "socket is not waiting for connect to complete");
            }
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    public final UnixSocketAddress getRemoteSocketAddress() {
        if (!isConnected()) {
            return null;
        }

        if (remoteAddress != null) {
            return remoteAddress;
        } else {
            remoteAddress = Common.getpeername(getFD());
            return remoteAddress;
        }
    }

    public final UnixSocketAddress getLocalSocketAddress() {
        if (localAddress != null) {
            return localAddress;
        } else {
            localAddress = Common.getsockname(getFD());
            return localAddress;
        }
    }

    @Override
    public boolean connect(SocketAddress remote) throws IOException {
        if (remote instanceof UnixSocketAddress) {
            return connect(((UnixSocketAddress) remote));
        } else {
            throw new UnsupportedAddressTypeException();
        }
    }

    @Override
    public UnixSocket socket() {
        return new UnixSocket(this);
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length)
            throws IOException {

        if (isConnected()) {
            return super.write(srcs, offset, length);
        } else if (isIdle()) {
            return 0;
        } else {
            throw new ClosedChannelException();
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (isConnected()) {
            return super.read(dst);
        } else if (isIdle()) {
            return 0;
        } else {
            throw new ClosedChannelException();
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (isConnected()) {
            return super.write(src);
        } else if (isIdle()) {
            return 0;
        } else {
            throw new ClosedChannelException();
        }
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        return remoteAddress;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return localAddress;
    }

    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();

        private static Set<SocketOption<?>> defaultOptions() {
            HashSet<SocketOption<?>> set = new HashSet<SocketOption<?>>(5);
            set.add(UnixSocketOptions.SO_SNDBUF);
            set.add(UnixSocketOptions.SO_SNDTIMEO);
            set.add(UnixSocketOptions.SO_RCVBUF);
            set.add(UnixSocketOptions.SO_RCVTIMEO);
            set.add(UnixSocketOptions.SO_PEERCRED);
            set.add(UnixSocketOptions.SO_KEEPALIVE);
            set.add(UnixSocketOptions.SO_PASSCRED);
            return Collections.unmodifiableSet(set);
        }
    }

    @Override
    public final Set<SocketOption<?>> supportedOptions() {
        return DefaultOptionsHolder.defaultOptions;
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        if (!supportedOptions().contains(name)) {
            throw new UnsupportedOperationException("'" + name
                    + "' not supported");
        }
        return Common.getSocketOption(getFD(), name);
    }

    @Override
    public <T> SocketChannel setOption(SocketOption<T> name, T value)
            throws IOException {
        if (name == null) {
            throw new IllegalArgumentException("name may not be null");
        }
        if (!supportedOptions().contains(name)) {
            throw new UnsupportedOperationException("'" + name
                    + "' not supported");
        }
        Common.setSocketOption(getFD(), name, value);
        return this;
    }

    @Override
    public synchronized UnixSocketChannel bind(SocketAddress local) throws IOException {
        localAddress = bindHandler.bind(getFD(), local);
        return this;
    }

}
