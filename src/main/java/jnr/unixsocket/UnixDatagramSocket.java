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
 * 
 */
package jnr.unixsocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.channels.DatagramChannel;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A SOCK_DGRAM variant of an AF_UNIX socket.
 * This specializaton of DatagramSocket delegates
 * most of it's funtionality to the corresponding
 * UnixDatagramChannel.
 */
public class UnixDatagramSocket extends DatagramSocket {

    private final UnixDatagramChannel chan;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Constructs a new instance.
     * @param channel The channel to use.
     * @throws SocketException if the socket could not be created.
     */
    UnixDatagramSocket(final UnixDatagramChannel channel) throws SocketException {
        chan = channel;
    }


    /**
     * Constructs a new unbound instance.
     * @throws SocketException if the socket could not be created.
     */
    public UnixDatagramSocket() throws SocketException {
        chan = null;
    }

    /**
     * Binds this UnixDatagramSocket to a specific AF_UNIX address.
     * <p>
     * If the address is {@code null}, then on Linux, an autobind will be performed,
     * which will bind this socket in Linux' abstract namespace on a unique path, chosen by
     * the system. On all other platforms, A temporary path in the regular filesystem will be chosen.
     *<p>
     * @param   local The {@link UnixSocketAddress} to bind to.
     * @throws  SocketException if any error happens during the bind, or if the
     *          socket is already bound.
     * @throws UnsupportedAddressTypeException if addr is a SocketAddress subclass
     *         not supported by this socket.
     */
    @Override
    public void bind(final SocketAddress local) throws SocketException {
        if (null != chan) {
            if (isClosed()) {
                throw new SocketException("Socket is closed");
            }
            if (isBound()) {
                throw new SocketException("already bound");
            }
            try {
                chan.bind(local);
            } catch (IOException e) {
                throw (SocketException)new SocketException().initCause(e);
            }
        }
    }

    @Override
    public synchronized void disconnect() {
        if (isClosed()) {
            return;
        }
        if (null != chan) {
            try {
                chan.disconnect();
            } catch (IOException e) {
                ignore();
            }
        }
    }

    @Override
    public synchronized void close() {
        if (null != chan && closed.compareAndSet(false, true)) {
            try {
                chan.close();
            } catch (IOException e) {
                ignore();
            }
        }
    }

    @Override
    public void connect(SocketAddress addr) throws SocketException {
        try {
            chan.connect(addr);
        } catch (IOException e) {
            throw (SocketException)new SocketException().initCause(e);
        }
    }

    @Override
    public void connect(InetAddress addr, int port) {
        throw new UnsupportedOperationException("connect(InetAddress, int) is not supported");
    }

    @Override
    public DatagramChannel getChannel() {
        return chan;
    }

    /**
     * Returns the address to which this socket is connected (<b>NOT implemented</b>).
     * Since AF_UNIX sockets can not have an InetAddress, this returns always {@code null}.
     * Use {@link #getRemoteSocketAddress} instead, which always returns a {@link UnixSocketAddress}.
     * @return {@code null} always.
     */
    @Override
    public InetAddress getInetAddress() {
        return null;
    }

    /**
     * Returns the address of the endpoint this socket is bound to.
     *
     * @return a {@code SocketAddress} representing the local endpoint of this
     *         socket, or {@code null} if it is closed or not bound.
     *         A non-null return value is always of type {@link UnixSocketAddress}
     * @see #bind(SocketAddress)
     */
    @Override
    public SocketAddress getLocalSocketAddress() {
        if (isClosed()) {
            return null;
        }
        if (null == chan) {
            return null;
        }
        return chan.getLocalSocketAddress();
    }

    /**
     * Returns the address of the endpoint this socket is connected to, or
     * {@code null} if it is unconnected.
     *
     * @return a {@code SocketAddress} representing the remote
     *         endpoint of this socket, or {@code null} if it is
     *         not connected.
     *         A non-null return value is always of type {@link UnixSocketAddress}
     */
    @Override
    public SocketAddress getRemoteSocketAddress() {
        if (!isConnected()) {
            return null;
        }
        return chan.getRemoteSocketAddress();
    }

    @Override
    public boolean isBound() {
        if (null == chan) {
            return false;
        }
        return chan.isBound();
    }

    @Override
    public boolean isClosed() {
        if (null == chan) {
            return false;
        }
        return closed.get();
    }

    @Override
    public boolean isConnected() {
        if (null == chan) {
            return false;
        }
        return chan.isConnected();
    }

    /**
     * Retrieves the credentials for this UNIX socket. Clients calling this
     * method will receive the server's credentials, and servers will receive
     * the client's credentials. User ID, group ID, and PID are supplied.
     *
     * See man unix 7; SCM_CREDENTIALS
     *
     * @throws UnsupportedOperationException if the underlying socket library
     *         doesn't support the SO_PEERCRED option
     * @throws SocketException if fetching the socket option failed.
     *
     * @return the credentials of the remote; null if not connected
     */
    public final Credentials getCredentials() throws SocketException {
        if (!chan.isConnected()) {
            return null;
        }
        try {
            return chan.getOption(UnixSocketOptions.SO_PEERCRED);
        } catch (IOException e) {
            throw (SocketException)new SocketException().initCause(e);
        }
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        try {
            return chan.getOption(UnixSocketOptions.SO_RCVBUF).intValue();
        } catch (IOException e) {
            throw (SocketException)new SocketException().initCause(e);
        }
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        try {
            return chan.getOption(UnixSocketOptions.SO_SNDBUF).intValue();
        } catch (IOException e) {
            throw (SocketException)new SocketException().initCause(e);
        }
    }

    @Override
    public int getSoTimeout() throws SocketException {
        try {
            return chan.getOption(UnixSocketOptions.SO_RCVTIMEO).intValue();
        } catch (IOException e) {
            throw (SocketException)new SocketException().initCause(e);
        }
    }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        try {
            chan.setOption(UnixSocketOptions.SO_RCVBUF, Integer.valueOf(size));
        } catch (IOException e) {
            throw (SocketException)new SocketException().initCause(e);
        }
    }

    @Override
    public void setSendBufferSize(int size) throws SocketException {
        try {
            chan.setOption(UnixSocketOptions.SO_SNDBUF, Integer.valueOf(size));
        } catch (IOException e) {
            throw (SocketException)new SocketException().initCause(e);
        }
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        try {
            chan.setOption(UnixSocketOptions.SO_RCVTIMEO, Integer.valueOf(timeout));
        } catch (IOException e) {
            throw (SocketException)new SocketException().initCause(e);
        }
    }

    /**
     * Sends a datagram packet from this socket (<b>NOT implemented</b>).
     * Unfortunately, {@link java.net.DatagramPacket} is final and can not deal
     * with AF_UNIX addresses. Therefore, this functionality was omitted.
     * @see java.net.DatagramPacket
     * @see java.net.DatagramSocket#send
     * @throws UnsupportedOperationException always.
     */
    @Override
    public void send(DatagramPacket p) throws IOException {
        throw new UnsupportedOperationException("sending DatagramPackets is not supported");
    }

    /**
     * Receives a datagram packet from this socket (<b>NOT implemented</b>).
     * Unfortunately, {@link java.net.DatagramPacket} is final and can not deal
     * with AF_UNIX addresses. Therefore, this functionality was omitted.
     * @see java.net.DatagramPacket
     * @see java.net.DatagramSocket#receive
     * @throws UnsupportedOperationException always.
     */
    @Override
    public synchronized void receive(DatagramPacket p) throws IOException {
        throw new UnsupportedOperationException("receiving DatagramPackets is not supported");
    }

    private void ignore() {
    }
}
