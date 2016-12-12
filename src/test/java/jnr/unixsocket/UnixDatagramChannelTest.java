package jnr.unixsocket;

import java.io.File;
import java.nio.file.Files;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.Assume;

import static junit.framework.Assert.*;

import jnr.ffi.Platform;
import jnr.ffi.Platform.OS;

public class UnixDatagramChannelTest {

    @Test
    public void testForUnnamedSockets() throws Exception {
        UnixDatagramChannel[] sp = UnixDatagramChannel.pair();

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

        UnixDatagramChannel ch = UnixDatagramChannel.open();
        ch.bind(null);
        UnixSocketAddress a = ch.getLocalSocketAddress();
        assertTrue("socket path pattern matches " + RE, a.path().matches(RE));
    }

    @Test
    public void testAutobindEmulation() throws Exception {
        Assume.assumeTrue(OS.LINUX != Platform.getNativePlatform().getOS());

        File f = Files.createTempFile("jnr-unixsocket-tmp", ".end").toFile();
        f.delete();
        String path = f.getPath().replaceAll("-tmp.*\\.end", "-tmp");
        final String RE = "^" + Pattern.quote(path) + ".*\\.sock$";
        UnixDatagramChannel ch = UnixDatagramChannel.open();
        ch.bind(null);
        UnixSocketAddress a = ch.getLocalSocketAddress();
        assertTrue("socket path pattern matches " + RE, a.path().matches(RE));
    }

    @Test
    public void testAbstractNamespace() throws Exception {
        Assume.assumeTrue(OS.LINUX == Platform.getNativePlatform().getOS());

        final String ABSTRACT = "\000foobarbaz";

        UnixSocketAddress a = new UnixSocketAddress(ABSTRACT);
        UnixDatagramChannel ch = UnixDatagramChannel.open();
        ch.bind(a);
        assertEquals("local socket path", ABSTRACT, ch.getLocalSocketAddress().path());
    }

}
