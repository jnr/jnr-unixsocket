package jnr.unixsocket;

import org.junit.Test;
import org.junit.Assume;

import static junit.framework.Assert.*;

import jnr.ffi.Platform;
import jnr.ffi.Platform.OS;

public class UnixSocketChannelTest {

    @Test
    public void testForUnnamedSockets() throws Exception {
        UnixSocketChannel[] sp = UnixSocketChannel.pair();

        // getpeername check
        assertEquals("remote socket path", "", sp[0].getRemoteSocketAddress().path());
        assertEquals("remote socket path", "", sp[1].getRemoteSocketAddress().path());

        // getsockname check
        assertEquals("local socket path", "", sp[0].getLocalSocketAddress().path());
        assertEquals("local socket path", "", sp[1].getLocalSocketAddress().path());
    }

    @Test
    public void testAutobind() throws Exception {
        Assume.assumeTrue(OS.LINUX == Platform.getNativePlatform().getOS());

        // see http://man7.org/linux/man-pages/man7/unix.7.html
        final String RE = "^\\000([0-9a-f]){5}$";

        UnixSocketChannel ch = UnixSocketChannel.open();
        ch.bind(null);
        UnixSocketAddress a = ch.getLocalSocketAddress();
        assertTrue("socket path pattern matches " + RE, a.path().matches(RE));
    }

    @Test
    public void testAbstractNamespace() throws Exception {
        Assume.assumeTrue(OS.LINUX == Platform.getNativePlatform().getOS());

        final String ABSTRACT = "\000foobarbaz";

        UnixSocketAddress a = new UnixSocketAddress(ABSTRACT);
        UnixSocketChannel ch = UnixSocketChannel.open();
        ch.bind(a);
        assertEquals("local socket path", ABSTRACT, ch.getLocalSocketAddress().path());
    }

}
