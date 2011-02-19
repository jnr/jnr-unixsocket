/*
 * This file is part of the JNR project (http://jnr.kenai.com)
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

package jnr.unixsocket;

import com.kenai.constantine.platform.SocketLevel;
import com.kenai.constantine.platform.SocketOption;
import com.kenai.jaffl.byref.IntByReference;
import jnr.enxio.channels.NativeSocketChannel;
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

        Native.libsocket().getsockopt(channel.getFD(), SocketLevel.SOL_SOCKET.value(), SocketOption.SO_KEEPALIVE.value(), buf, ref);

        return buf.getInt(0) != 0;
    }
}
