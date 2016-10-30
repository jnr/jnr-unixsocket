/*
 * Copyright (C) 2016 Fritz Elfert
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
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.DatagramSocket;
import java.nio.channels.DatagramChannel;
import java.nio.channels.UnsupportedAddressTypeException;

/**
 * A SOCK_DGRAM variant of an AF_UNIX socket.
 * This specializaton of DatagramSocket delegates
 * all it's funtionality to the corresponding
 * UnixDatagramSocket.
 */
public class UnixDatagramSocket extends DatagramSocket {

	private final UnixDatagramChannel chan;

	volatile private boolean closed = false;

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

    @Override
    public void bind(final SocketAddress local) throws SocketException {
        if (null != local && !(local instanceof UnixSocketAddress)) {
            throw new UnsupportedAddressTypeException();
        }
        if (null != chan) {
            try {
                chan.bind(local);
            } catch (IOException e) {
                throw (SocketException)new SocketException().initCause(e);
            }
        }
    }

    @Override
    public void close() {
        if (null != chan) {
            try {
                chan.close();
            } catch (IOException e) {
                // ignore
            }
        }
        closed = true;
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

    @Override
    public InetAddress getInetAddress() {
        return null;
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        UnixSocketAddress address = chan.getLocalSocketAddress();
        if (address != null) {
            return address;
        } else {
            return null;
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
        return null != chan.getLocalSocketAddress();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isConnected() {
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
     *
     * @return the credentials of the remote
     */
    public final Credentials getCredentials() {
        return chan.getCredentials();
    }
}
