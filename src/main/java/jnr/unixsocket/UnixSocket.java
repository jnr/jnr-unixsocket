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
 * 
 * ported from  https://github.com/softprops/unisockets/blob/master/unisockets-core/src/main/scala/Socket.scala 
 */
package jnr.unixsocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

class UnixSocket extends java.net.Socket {

	private UnixSocketChannel chan;

	volatile private boolean closed = false;
	volatile private boolean indown = false;
	volatile private boolean outdown = false;

	private InputStream in;
	private OutputStream out;

	public UnixSocket(UnixSocketChannel chan) {
		this.chan = chan;
		in = Channels.newInputStream(chan);
		out = Channels.newOutputStream(chan);
	}

	public void bind(SocketAddress addr) {
		throw new UnsupportedOperationException("bind not supported");
	}

	@Override
	public void close() throws IOException {
		chan.close();
		closed = true;
	}

	@Override
	public void connect(SocketAddress addr) throws IOException {
		connect(addr, 0);
	}

	public void connect(SocketAddress addr, Integer timeout) throws IOException {
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

	public InputStream getInputStream() throws IOException {
		if (chan.isConnected()) {
			return in;
		} else {
			throw new IOException("not connected");
		}
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

	@Override
	public boolean isInputShutdown() {
		return indown;
	}

	@Override
	public boolean isOutputShutdown() {
		return outdown;
	}

	@Override
	public void shutdownInput() throws IOException {
		chan.shutdownInput();
		indown = true;
	}

	@Override
	public void shutdownOutput() throws IOException {
		chan.shutdownOutput();
		outdown = true;
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