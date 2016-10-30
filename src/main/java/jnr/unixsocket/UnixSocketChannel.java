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
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.Set;

import jnr.constants.platform.Errno;
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;
import jnr.enxio.channels.AbstractNativeSocketChannel;
import jnr.ffi.LastError;

/**
 * A {@link java.nio.channels.Channel} implementation that uses a native unix
 * socket
 */
public class UnixSocketChannel extends AbstractNativeSocketChannel {
    enum State {
        UNINITIALIZED, CONNECTED, IDLE, CONNECTING,
    }

    private volatile State state;
    private UnixSocketAddress remoteAddress = null;
    private UnixSocketAddress localAddress = null;

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
        return new UnixSocketChannel[] { new UnixSocketChannel(sockets[0]),
            new UnixSocketChannel(sockets[1]) };
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

    private UnixSocketChannel() throws IOException {
        super(Native.socket(ProtocolFamily.PF_UNIX, Sock.SOCK_STREAM, 0));
        state = State.IDLE;
    }

    UnixSocketChannel(int fd) {
        super(fd);
        state = State.CONNECTED;
    }

    UnixSocketChannel(int fd, UnixSocketAddress remote) {
        super(fd);
        state = State.CONNECTED;
        remoteAddress = remote;
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
                throw new IllegalStateException(
                        "socket is not waiting for connect to complete");
        }
    }

    public final UnixSocketAddress getRemoteSocketAddress() {
        if (state != State.CONNECTED) {
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

    /**
     * Retrieves the credentials for this UNIX socket. If this socket channel is
     * not in a connected state, this method will return null.
     *
     * See man unix 7; SCM_CREDENTIALS
     *
     * @throws UnsupportedOperationException
     *             if the underlying socket library doesn't support the
     *             SO_PEERCRED option
     *
     * @return the credentials of the remote; null if not connected
     */
    public final Credentials getCredentials() {
        if (state != State.CONNECTED) {
            return null;
        }

        return Credentials.getCredentials(getFD());
    }

    public boolean getKeepAlive() {
        int ret = Native.getsockopt(getFD(), SocketLevel.SOL_SOCKET,
                SocketOption.SO_KEEPALIVE.intValue());
        return (ret == 1) ? true : false;
    }

    public void setKeepAlive(boolean on) {
        Native.setsockopt(getFD(), SocketLevel.SOL_SOCKET,
                SocketOption.SO_KEEPALIVE, on);
    }

    public int getSoTimeout() {
        return Native.getsockopt(getFD(), SocketLevel.SOL_SOCKET,
                SocketOption.SO_RCVTIMEO.intValue());
    }

    public void setSoTimeout(int timeout) {
        Native.setsockopt(getFD(), SocketLevel.SOL_SOCKET,
                SocketOption.SO_RCVTIMEO, timeout);
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
    public Socket socket() {
        return new UnixSocket(this);
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length)
    throws IOException {

    if (state == State.CONNECTED) {
        return super.write(srcs, offset, length);
    } else if (state == State.IDLE) {
        return 0;
    } else {
        throw new ClosedChannelException();
    }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (state == State.CONNECTED) {
            return super.read(dst);
        } else if (state == State.IDLE) {
            return 0;
        } else {
            throw new ClosedChannelException();
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (state == State.CONNECTED) {
            return super.write(src);
        } else if (state == State.IDLE) {
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

    @Override
    public <T> T getOption(java.net.SocketOption<T> name) throws IOException {
        throw new UnsupportedOperationException("getOption is not supported");
    }

    @Override
    public <T> SocketChannel setOption(java.net.SocketOption<T> name, T value)
    throws IOException {
    throw new UnsupportedOperationException("setOption is not supported");
    }

    @Override
    public Set<java.net.SocketOption<?>> supportedOptions() {
        throw new UnsupportedOperationException(
                "supportedOptions is not supported");
    }

    @Override
    public SocketChannel bind(SocketAddress local) throws IOException {
        if (null != local && !(local instanceof UnixSocketAddress)) {
            throw new UnsupportedAddressTypeException();
        }
        localAddress = Common.bind(getFD(), (UnixSocketAddress)local);
        return this;
    }

}
