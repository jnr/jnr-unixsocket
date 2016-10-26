package jnr.unixsocket;

import org.junit.Test;

import static junit.framework.Assert.*;

public class UnixSocketChannelTest {
    @Test
    public void testForUnnamedSockets() throws Exception {
        UnixSocketChannel[] sp = UnixSocketChannel.pair();

        // getpeername check
        assertEquals(sp[0].getRemoteSocketAddress().path(), "");
        assertEquals(sp[1].getRemoteSocketAddress().path(), "");

        // getsockname check
        assertEquals(sp[0].getLocalSocketAddress().path(), "");
        assertEquals(sp[1].getLocalSocketAddress().path(), "");
    }
}
