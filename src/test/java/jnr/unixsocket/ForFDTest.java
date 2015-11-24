package jnr.unixsocket;

import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import org.junit.Test;

import java.io.File;
import java.nio.ByteBuffer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Created by headius on 11/24/15.
 */
public class ForFDTest {
    private static final File SOCKADDR = new File("/tmp/jnr-unixsocket-forfd" + System.currentTimeMillis() + ".sock");
    static { SOCKADDR.deleteOnExit(); }
    private static final UnixSocketAddress ADDRESS = new UnixSocketAddress(SOCKADDR);
    private static final String FOOBAR = "foobar";

    private volatile Exception serverException;

    @Test
    public void testForFD() throws Exception {
        int fd = 0;
        UnixSocketChannel channel = null;

        try {
            final UnixServerSocketChannel server = UnixServerSocketChannel.open();
            server.socket().bind(ADDRESS);

            new Thread("accept thread") {
                public void run() {
                    UnixSocketChannel channel = null;

                    try {
                        channel = server.accept();
                        channel.write(ByteBuffer.wrap(FOOBAR.getBytes()));
                    } catch (Exception e) {
                        serverException = e;
                    } finally {
                        try {channel.close();} catch (Exception e) {}
                    }
                }
            }.start();

            fd = Native.socket(ProtocolFamily.PF_UNIX, Sock.SOCK_STREAM, 0);

            assertTrue("socket failed", fd > 0);

            int ret = Native.connect(fd, ADDRESS.getStruct(), ADDRESS.getStruct().length());

            assertTrue("connect failed", ret >= 0);

            channel = UnixSocketChannel.fromFD(fd);

            assertNotNull(channel);

            ByteBuffer buf = ByteBuffer.allocate(1024);

            channel.read(buf);

            assertEquals(FOOBAR.length(), buf.position());

            buf.flip();
            String result = new String(buf.array(), buf.position(), buf.limit(), "UTF-8");

            assertEquals(FOOBAR, result);

            if (serverException != null) throw serverException;
        } finally {
            channel.close();
        }
    }
}
