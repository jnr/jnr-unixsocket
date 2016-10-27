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
import java.nio.channels.Channels;
import java.nio.channels.DatagramChannel;

class UnixDatagramSocket extends DatagramSocket {

	private final UnixDatagramChannel chan;

	volatile private boolean closed = false;

	public UnixDatagramSocket(UnixDatagramChannel chan) throws SocketException {
        this.chan = chan;
	}

	public void bind(SocketAddress addr) {
		throw new UnsupportedOperationException("bind not supported");
	}

	@Override
	public void close() {
		closed = true;
	}

	@Override
	public void connect(SocketAddress addr) {
		if (addr instanceof UnixSocketAddress) {
            try {
			chan.connect((UnixSocketAddress) addr);
            } catch (IOException e) {
            }
		} else {
			throw new IllegalArgumentException("address of type "
					+ addr.getClass() + " are not supported. Use "
					+ UnixSocketAddress.class + " instead");
		}
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
		return false;
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
