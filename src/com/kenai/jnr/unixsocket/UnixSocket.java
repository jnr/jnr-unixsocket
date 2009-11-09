
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
