package jnr.unixsocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetBoundException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Confirm that UNIX sockets work similarly to TCP sockets.
 */
@RunWith(Parameterized.class)
public class SocketInteropTest {
    @Rule
    public Timeout timeout = new Timeout(5, TimeUnit.SECONDS);

    @Parameter
    public TestSocketPair.Factory socketPairFactory;

    private TestSocketPair socketPair;

    @Parameters(name = "{0}")
    public static List<Object[]> parameters() {
        return Arrays.asList(
                new Object[] { UnixSocketPair.FACTORY },
                new Object[] { TcpSocketPair.FACTORY }
        );
    }

    @Before
    public void setUp() throws Exception {
        socketPair = socketPairFactory.createUnconnected();
    }

    @After
    public void tearDown() throws Exception {
        socketPair.close();
    }

    @Test
    public void serverWritesAndClientReads() throws IOException {
        socketPair.connectBlocking();

        OutputStream serverOut = socketPair.serverChannel().socket().getOutputStream();
        serverOut.write("message from server to client\n".getBytes(UTF_8));
        serverOut.flush();

        InputStream clientIn = socketPair.clientChannel().socket().getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientIn, UTF_8));
        assertEquals("message from server to client", reader.readLine());
    }

    @Test
    public void clientWritesAndServerReads() throws IOException {
        socketPair.connectBlocking();

        OutputStream clientOut = socketPair.clientChannel().socket().getOutputStream();
        clientOut.write("message from client to server\n".getBytes(UTF_8));
        clientOut.flush();

        InputStream serverIn = socketPair.serverChannel().socket().getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(serverIn, UTF_8));
        assertEquals("message from client to server", reader.readLine());
    }

    @Test
    public void acceptThrowsWhenServerSocketIsNotYetBound() throws IOException {
        try {
            socketPair.serverAccept();
            fail();
        } catch (NotYetBoundException expected) {
        }
    }

    @Test
    public void acceptThrowsWhenServerSocketIsClosed() throws IOException {
        socketPair.serverBind();
        socketPair.serverSocketChannel().close();
        try {
            socketPair.serverAccept();
            fail();
        } catch (ClosedChannelException expected) {
        }
    }

    @Test
    public void acceptThrowsWhenServerSocketIsAsynchronouslyClosed() throws IOException {
        socketPair.serverBind();
        closeLater(socketPair.serverSocketChannel(), 500, TimeUnit.MILLISECONDS);
        try {
            socketPair.serverAccept();
            fail();
        } catch (AsynchronousCloseException expected) {
        }
    }

    private void closeLater(final Closeable closeable, final long delay, final TimeUnit timeUnit) {
        new Thread(getClass().getName() + ".closeLater") {
            @Override
            public void run() {
                try {
                    Thread.sleep(timeUnit.toMillis(delay));
                    closeable.close();
                } catch (IOException | InterruptedException ignored) {
                }
            }
        }.start();
    }

    @Test
    public void acceptThrowsWhenAcceptingThreadIsInterrupted() throws IOException {
        socketPair.serverBind();
        interruptLater(Thread.currentThread(), 500, TimeUnit.MILLISECONDS);
        try {
            socketPair.serverAccept();
            fail();
        } catch (ClosedByInterruptException expected) {
        }
        // This has a side-effect of clearing the interrupted state. Otherwise later tests may fail!
        assertTrue(Thread.interrupted());
    }

    private void interruptLater(final Thread target, final long delay, final TimeUnit timeUnit) {
        new Thread(getClass().getName() + ".interruptLater") {
            @Override
            public void run() {
                try {
                    Thread.sleep(timeUnit.toMillis(delay));
                    target.interrupt();
                } catch (InterruptedException ignored) {
                }
            }
        }.start();
    }
}
