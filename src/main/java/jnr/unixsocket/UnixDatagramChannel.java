/*
 * Copyright (C) 2016 Fritz Elfert
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

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.UnsupportedAddressTypeException;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.unixsocket.impl.AbstractNativeDatagramChannel;

public class UnixDatagramChannel extends AbstractNativeDatagramChannel {
    static enum State {
        UNINITIALIZED,
        CONNECTED,
        IDLE,
    }
    private State state;
    private UnixSocketAddress remoteAddress = null;
    private UnixSocketAddress localAddress = null;
    private final ReadWriteLock stateLock = new ReentrantReadWriteLock();
    private final BindHandler bindHandler;

    public static final UnixDatagramChannel open() throws IOException {
        return new UnixDatagramChannel();
    }

    public static final UnixDatagramChannel open(ProtocolFamily domain, int protocol) throws IOException {
        return new UnixDatagramChannel(domain, protocol);
    }

    public static final UnixDatagramChannel[] pair() throws IOException {
        int[] sockets = { -1, -1 };
        Native.socketpair(ProtocolFamily.PF_UNIX, Sock.SOCK_DGRAM, 0, sockets);
        return new UnixDatagramChannel[] {
            new UnixDatagramChannel(sockets[0], State.CONNECTED, true),
                new UnixDatagramChannel(sockets[1], State.CONNECTED, true)
        };
    }

    private UnixDatagramChannel() throws IOException {
        this(Native.socket(ProtocolFamily.PF_UNIX, Sock.SOCK_DGRAM, 0));
    }

    UnixDatagramChannel(ProtocolFamily domain, int protocol) throws IOException
    {
        this(Native.socket(domain, Sock.SOCK_DGRAM, protocol));
    }

    UnixDatagramChannel(int fd) {
        this(fd, State.IDLE, false);
    }

    UnixDatagramChannel(int fd, State initialState, boolean initialBoundState) {
        super(fd);
        stateLock.writeLock().lock();
        try {
            state = initialState;
            bindHandler = new BindHandler(initialBoundState);
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    UnixDatagramChannel(int fd, UnixSocketAddress remote) throws IOException {
        this(fd);
        connect(remote);
    }

    @Override
    public UnixDatagramChannel bind(SocketAddress local) throws IOException {
        localAddress = bindHandler.bind(getFD(), local);
        return this;
    }

    public UnixDatagramChannel connect(UnixSocketAddress remote) {
        stateLock.writeLock().lock();
        remoteAddress = remote;
        state = State.CONNECTED;
        stateLock.writeLock().unlock();
        return this;
    }

    public UnixDatagramChannel disconnect() throws IOException {
        stateLock.writeLock().lock();
        remoteAddress = null;
        state = State.IDLE;
        stateLock.writeLock().unlock();
        return this;
    }

    boolean isBound() {
        return bindHandler.isBound();
    }

    public boolean isConnected() {
        stateLock.readLock().lock();
        boolean isConnected = state == State.CONNECTED;
        stateLock.readLock().unlock();
        return isConnected;
    }

    public final UnixSocketAddress getRemoteSocketAddress() {
        if (!isConnected()) {
            return null;
        }
        return remoteAddress != null ? remoteAddress : (remoteAddress = Common.getpeername(getFD()));
    }

    public final UnixSocketAddress getLocalSocketAddress() {
        return localAddress != null ? localAddress : (localAddress = Common.getsockname(getFD()));
    }

    @Override
    public UnixSocketAddress receive(ByteBuffer src) throws IOException {
        UnixSocketAddress remote = new UnixSocketAddress();
        int n = Native.recvfrom(getFD(), src, remote.getStruct());
        if (n < 0) {
            throw new IOException(Native.getLastErrorString());
        }
        return remote;
    }

    @Override
    public int send(ByteBuffer src, SocketAddress target) throws IOException {
        UnixSocketAddress remote = null;
        if (null == target) {
            if (isConnected()) {
                remote = remoteAddress;
            } else {
                throw new IllegalArgumentException("Destination address cannot be null on unconnected datagram sockets");
            }
        } else {
            if (!(target instanceof UnixSocketAddress)) {
                throw new UnsupportedAddressTypeException();
            }
            remote = (UnixSocketAddress)target;
        }
        SockAddrUnix sa = (null == remote) ? null : remote.getStruct();
        int addrlen = (null == sa) ? 0 : sa.length();
        int n = Native.sendto(getFD(), src, sa, addrlen);
        if (n < 0) {
            throw new IOException(Native.getLastErrorString());
        }

        return n;
    }

    @Override
    public DatagramChannel connect(SocketAddress remote) throws IOException {
        if (remote instanceof UnixSocketAddress) {
            return connect(((UnixSocketAddress) remote));
        } else {
            throw new UnsupportedAddressTypeException();
        }
    }

    @Override
    public UnixDatagramSocket socket() {
        try {
            return new UnixDatagramSocket(this);
        } catch (SocketException e) {
            throw new NullPointerException("Could not create UnixDatagramSocket");
        }
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

    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();

        private static Set<SocketOption<?>> defaultOptions() {
            HashSet<SocketOption<?>> set = new HashSet<SocketOption<?>>(5);
            set.add(UnixSocketOptions.SO_SNDBUF);
            set.add(UnixSocketOptions.SO_SNDTIMEO);
            set.add(UnixSocketOptions.SO_RCVBUF);
            set.add(UnixSocketOptions.SO_RCVTIMEO);
            set.add(UnixSocketOptions.SO_PEERCRED);
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
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        return Common.getSocketOption(getFD(), name);
    }

    @Override
    public <T> DatagramChannel setOption(SocketOption<T> name, T value)
        throws IOException {
        if (name == null) {
            throw new IllegalArgumentException("name may not be null");
        }
        if (!supportedOptions().contains(name)) {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        Common.setSocketOption(getFD(), name, value);
        return this;
    }

    @Override
    public MembershipKey join(InetAddress group, NetworkInterface interf) {
        throw new UnsupportedOperationException("join is not supported");
    }

    @Override
    public MembershipKey join(InetAddress group, NetworkInterface interf, InetAddress source) {
        throw new UnsupportedOperationException("join is not supported");
    }

}
