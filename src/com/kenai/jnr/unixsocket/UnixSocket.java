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

package com.kenai.jnr.unixsocket;

import com.kenai.jnr.enxio.channels.NativeSocketChannel;
import java.nio.channels.Channel;

public class UnixSocket {
    private final NativeSocketChannel channel;

    UnixSocket(NativeSocketChannel channel) {
        this.channel = channel;
    }

    public final Channel getChannel() {
        return channel;
    }
}
