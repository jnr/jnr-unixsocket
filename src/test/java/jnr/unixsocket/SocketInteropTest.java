package jnr.unixsocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

/**
 * Confirm that UNIX sockets work similarly to TCP sockets.
 */
@RunWith(Parameterized.class)
public class SocketInteropTest {
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
}
