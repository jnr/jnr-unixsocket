/*
 * Copyright (C) 2009 Wayne Meissner
 * Copyright (C) 2016 Marcus Linke
 *
 * (ported from https://github.com/softprops/unisockets/blob/master/unisockets-core/src/main/scala/Socket.scala)
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class UnixSocket extends java.net.Socket {

    private UnixSocketChannel chan;

    private AtomicBoolean closed = new AtomicBoolean(false);
    private AtomicBoolean indown = new AtomicBoolean(false);
    private AtomicBoolean outdown = new AtomicBoolean(false);

    private InputStream in;
    private OutputStream out;

    public UnixSocket(UnixSocketChannel chan) {
        this.chan = chan;
        in = Channels.newInputStream(new UnselectableByteChannel(chan));
        out = Channels.newOutputStream(new UnselectableByteChannel(chan));
    }

    @Override
    public void bind(SocketAddress local) throws IOException {
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
    public void close() throws IOException {
        if (null != chan && closed.compareAndSet(false, true)) {
            try {
                chan.close();
            } catch (IOException e) {
                ignore();
            }
        }
    }

    @Override
    public void connect(SocketAddress addr) throws IOException {
        connect(addr, 0);
    }

    @Override
    public void connect(SocketAddress addr, int timeout) throws IOException {
        if (addr instanceof UnixSocketAddress) {
            chan.connect((UnixSocketAddress) addr);
        } else {
            throw new IllegalArgumentException("address of type "
                    + addr.getClass() + " are not supported. Use "
                    + UnixSocketAddress.class + " instead");
        }
    }

    @Override
    public SocketChannel getChannel() {
        return chan;
    }

    @Override
    public InetAddress getInetAddress() {
        return null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (chan.isConnected()) {
            return in;
        } else {
            throw new IOException("not connected");
        }
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return chan.getLocalSocketAddress();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (chan.isConnected()) {
            return out;
        } else {
            throw new IOException("not connected");
        }
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        SocketAddress address = chan.getRemoteSocketAddress();

        if (address != null) {
            return address;
        } else {
            return null;
        }
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
        return closed.get();
    }

    @Override
    public boolean isConnected() {
        return chan.isConnected();
    }

    @Override
    public boolean isInputShutdown() {
        return indown.get();
    }

    @Override
    public boolean isOutputShutdown() {
        return outdown.get();
    }

    @Override
    public void shutdownInput() throws IOException {
        if (indown.compareAndSet(false, true)) {
            chan.shutdownInput();
        }
    }

    @Override
    public void shutdownOutput() throws IOException {
        if (outdown.compareAndSet(false, true)) {
            chan.shutdownOutput();
        }
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
    public boolean getKeepAlive() throws SocketException {
        try {
            return chan.getOption(UnixSocketOptions.SO_KEEPALIVE).booleanValue();
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
    public void setKeepAlive(boolean on) throws SocketException {
        try {
            chan.setOption(UnixSocketOptions.SO_KEEPALIVE, Boolean.valueOf(on));
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

    private void ignore() {
    }

    /**
     * A byte channel that doesn't implement {@link SelectableChannel}. Though
     * that type isn't in the public API, if the channel passed in implements
     * that interface then unwanted synchronization is performed which can harm
     * concurrency and can cause deadlocks.
     *
     * https://bugs.openjdk.java.net/browse/JDK-4774871
     */
    static final class UnselectableByteChannel implements ReadableByteChannel, WritableByteChannel {
        private final UnixSocketChannel channel;

        UnselectableByteChannel(UnixSocketChannel channel) {
            this.channel = channel;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            return channel.write(src);
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            return channel.read(dst);
        }

        @Override
        public boolean isOpen() {
            return channel.isOpen();
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }
    }
}
