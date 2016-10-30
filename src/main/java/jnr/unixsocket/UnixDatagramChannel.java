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

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Set;

import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.enxio.channels.AbstractNativeDatagramChannel;

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

    public static final UnixDatagramChannel open() throws IOException {
        return new UnixDatagramChannel();
    }

    public static final UnixDatagramChannel[] pair() throws IOException {
        int[] sockets = { -1, -1 };
        Native.socketpair(ProtocolFamily.PF_UNIX, Sock.SOCK_DGRAM, 0, sockets);
        return new UnixDatagramChannel[] {
            new UnixDatagramChannel(sockets[0], State.CONNECTED),
                new UnixDatagramChannel(sockets[1], State.CONNECTED)
        };
    }

    private UnixDatagramChannel() throws IOException {
        this(Native.socket(ProtocolFamily.PF_UNIX, Sock.SOCK_DGRAM, 0));
    }

    UnixDatagramChannel(int fd) {
        this(fd, State.IDLE);
    }

    UnixDatagramChannel(int fd, State initialState) {
        super(fd);
        stateLock.writeLock().lock();
        state = initialState;
        stateLock.writeLock().unlock();
    }

    UnixDatagramChannel(int fd, UnixSocketAddress remote) throws IOException {
        this(fd);
        connect(remote);
    }

	@Override
	public UnixDatagramChannel bind(SocketAddress local) throws IOException {
        if (null != local && !(local instanceof UnixSocketAddress)) {
            throw new UnsupportedAddressTypeException();
        }
        localAddress = Common.bind(getFD(), (UnixSocketAddress)local);
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

    public boolean isConnected() {
        stateLock.readLock().lock();
        boolean isConnected = state == State.CONNECTED;
        stateLock.readLock().lock();
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
	public DatagramSocket socket() {
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

	@Override
	public <T> T getOption(java.net.SocketOption<T> name) throws IOException {
		throw new UnsupportedOperationException("getOption is not supported");
	}

	@Override
	public <T> DatagramChannel setOption(java.net.SocketOption<T> name, T value)
			throws IOException {
		throw new UnsupportedOperationException("setOption is not supported");
	}

	@Override
	public Set<java.net.SocketOption<?>> supportedOptions() {
		throw new UnsupportedOperationException("supportedOptions is not supported");
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
