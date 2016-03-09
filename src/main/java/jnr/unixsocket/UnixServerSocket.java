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
 */

package jnr.unixsocket;

import java.io.IOException;
import java.net.SocketAddress;

public class UnixServerSocket {
    final UnixServerSocketChannel channel;
    final int fd;
    volatile UnixSocketAddress localAddress;

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
        localAddress = (UnixSocketAddress) endpoint;

        if (Native.bind(fd, localAddress.getStruct(), localAddress.length()) < 0) {
            throw new IOException(Native.getLastErrorString());
        }

        if (Native.listen(fd, backlog) < 0) {
            throw new IOException(Native.getLastErrorString());
        }
    }
    
}
