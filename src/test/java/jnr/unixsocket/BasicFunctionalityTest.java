
package jnr.unixsocket;

import jnr.enxio.channels.NativeSelectorProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import static junit.framework.Assert.*;

public class BasicFunctionalityTest {
    private static final File SOCKADDR = new File("/tmp/jnr-unixsocket-test" + System.currentTimeMillis() + ".sock");
    static { SOCKADDR.deleteOnExit(); }
    private static final UnixSocketAddress ADDRESS = new UnixSocketAddress(SOCKADDR);
    private static final String DATA = "blah blah";

    private Thread server;
    private volatile Exception serverException;

    @Test
    public void basicOperation() throws Exception {
        // server logic
        final UnixServerSocketChannel channel = UnixServerSocketChannel.open();
        final Selector sel = NativeSelectorProvider.getInstance().openSelector();
        channel.configureBlocking(false);
        channel.socket().bind(ADDRESS);
        channel.register(sel, SelectionKey.OP_ACCEPT, new ServerActor(channel, sel));

        // TODO: This is ugly but simple enough. Many failures on server side will cause client to hang.
        server = new Thread("server side") {
            public void run() {
                try {
                    while (sel.select() > 0) {
                        Set<SelectionKey> keys = sel.selectedKeys();

                        assertNotNull(keys);
                        assertTrue(keys.size() > 0);

                        for (SelectionKey k : keys) {
                            assertTrue(k.attachment() instanceof Actor);

                            Actor a = (Actor) k.attachment();
                            if (!a.rxready()) {
                                k.cancel();
                            }
                        }
                    }
                } catch (Exception ex) {
                    serverException = ex;
                }
            }
        };

        server.start();

        // client logic
        UnixSocketChannel channel2 = UnixSocketChannel.open(ADDRESS);

        assertEquals(ADDRESS, channel2.getRemoteSocketAddress());

        Channels.newOutputStream(channel2).write(DATA.getBytes());

        InputStreamReader r = new InputStreamReader(Channels.newInputStream(channel2));
        CharBuffer result = CharBuffer.allocate(1024);
        r.read(result);

        assertEquals(DATA.length(), result.position());

        result.flip();

        assertEquals(DATA, result.toString());

        if (serverException != null) throw serverException;
    }

    static interface Actor {
        public boolean rxready();
    }

    final class ServerActor implements Actor {
        private final UnixServerSocketChannel channel;
        private final Selector selector;

        public ServerActor(UnixServerSocketChannel channel, Selector selector) {
            this.channel = channel;
            this.selector = selector;
        }

        public final boolean rxready() {
            try {
                UnixSocketChannel client = channel.accept();

                assertNotNull(client);
                // TODO: This doesn't work for some reason.
//                assertEquals(ADDRESS, client.getLocalSocketAddress());
//                assertEquals("", client.getRemoteSocketAddress().getStruct().getPath());

                client.configureBlocking(false);
                client.register(selector, SelectionKey.OP_READ, new ClientActor(client));

                return true;
            } catch (IOException ex) {
                return false;
            }
        }
    }

    final class ClientActor implements Actor {
        private final UnixSocketChannel channel;

        public ClientActor(UnixSocketChannel channel) {
            this.channel = channel;
        }

        public final boolean rxready() {
            try {
                ByteBuffer buf = ByteBuffer.allocate(1024);
                int n = channel.read(buf);
                // TODO: This doesn't work for some reason.
//                assertEquals(ADDRESS, channel.getRemoteSocketAddress());

                assertEquals(DATA.length(), n);
                
                if (n > 0) {
                    buf.flip();
                    channel.write(buf);
                    return true;
                } else if (n < 0) {
                    return false;
                }

            } catch (IOException ex) {
                ex.printStackTrace();
                return false;
            }
            return true;
        }
    }
}
