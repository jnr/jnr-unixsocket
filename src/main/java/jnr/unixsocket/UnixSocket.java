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

import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;
import jnr.enxio.channels.NativeSocketChannel;
import jnr.ffi.byref.IntByReference;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channel;

public class UnixSocket {
    private final NativeSocketChannel channel;

    UnixSocket(NativeSocketChannel channel) {
        this.channel = channel;
    }

    public final Channel getChannel() {
        return channel;
    }

    public final void setKeepAlive(boolean on) {
        Native.setsockopt(channel.getFD(), SocketLevel.SOL_SOCKET, SocketOption.SO_KEEPALIVE, on);
    }

    public final boolean getKeepAlive() {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.order(ByteOrder.BIG_ENDIAN);
        IntByReference ref = new IntByReference(4);

        Native.libsocket().getsockopt(channel.getFD(), SocketLevel.SOL_SOCKET.intValue(), SocketOption.SO_KEEPALIVE.intValue(), buf, ref);

        return buf.getInt(0) != 0;
    }

    /**
     * Retrieves the credentials for this UNIX socket. Clients calling this
     * method will receive the server's credentials, and servers will receive
     * the client's credentials. User ID, group ID, and PID are supplied.
     *
     * @see man unix 7; SCM_CREDENTIALS
     *
     * @throws UnsupportedOperationException if the underlying socket library
     *         doesn't support the SO_PEERCRED option
     *
     * @return the credentials of the remote
     */
    public final Credentials getCredentials() {
        return Credentials.getCredentials(channel.getFD());
    }
}
