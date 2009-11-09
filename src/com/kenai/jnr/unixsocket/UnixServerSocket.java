/*
 * Copyright (C) 2009 Wayne Meissner
 *
 * This file is part of java native runtime.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.kenai.jnr.unixsocket;

import java.io.IOException;
import java.net.SocketAddress;

public class UnixServerSocket {
    final UnixServerSocketChannel channel;
    final int fd;

    public UnixServerSocket() throws IOException {
        this.channel = new UnixServerSocketChannel(this);
        this.fd = channel.getFD();
    }

    UnixServerSocket(UnixServerSocketChannel channel) {
        this.channel = channel;
        this.fd = channel.getFD();
    }

    public UnixSocket accept() throws IOException {
        return new UnixSocket(channel.accept());
    }

    public void bind(SocketAddress endpoint) throws java.io.IOException {
        bind(endpoint, 128);
    }

    public void bind(SocketAddress endpoint, int backlog) throws java.io.IOException {
        if (!(endpoint instanceof UnixSocketAddress)) {
            throw new IOException("Invalid address");
        }
        UnixSocketAddress addr = (UnixSocketAddress) endpoint;

        if (Native.bind(fd, addr.getStruct(), addr.length()) < 0) {
            throw new IOException("bind failed: " + Native.getLastErrorString());
        }

        if (Native.listen(fd, backlog) < 0) {
            throw new IOException("listen failed: " + Native.getLastErrorString());
        }
    }
    
}
