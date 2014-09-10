package jnr.unixsocket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jnr.ffi.Platform;
import jnr.ffi.Platform.OS;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CredentialsFunctionalTest {
    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    private File serverSocket;
    private ExecutorService async = Executors.newSingleThreadExecutor();

    @Before
    public void createSockets() throws IOException {
        Assume.assumeTrue(OS.LINUX == Platform.getNativePlatform().getOS());

        serverSocket = tempFolder.newFile("serverSocket");
        serverSocket.delete(); //JUnit is "helpful" and creates it for us
    }

    @Test(timeout=30000)
    public void credentials() throws IOException, ExecutionException, InterruptedException {
        UnixSocketAddress address = new UnixSocketAddress(serverSocket);
        final UnixServerSocket socket = new UnixServerSocket();
        socket.bind(address);

        Future<UnixSocket> socketFuture = async.submit(new Callable<UnixSocket>() {
            public UnixSocket call() throws Exception {
                return socket.accept();
            }
        });

        UnixSocketChannel client = UnixSocketChannel.open(address);
        UnixSocket server = socketFuture.get();

        assertNotNull("Client socket must be non-null.", client);
        assertNotNull("Server socket must be non-null.", server);

        Credentials clientCreds = client.getCredentials();
        Credentials serverCreds = server.getCredentials();

        int myPid = getCurrentPid();
        int loginUid = getLoginUid();

        assertEquals("Current PID should match client credentials",
                myPid, clientCreds.getPid());
        assertEquals("Current PID should match server credentials",
                myPid, serverCreds.getPid());

        assertEquals("Login UID should match client credentials",
                loginUid, clientCreds.getUid());
        assertEquals("Login UID should match server credentials",
                loginUid, serverCreds.getUid());

        //don't have an easy way of getting effective GID, but they should be the same
        assertEquals("Client/server running in same process, GID should be the same",
                clientCreds.getGid(), serverCreds.getGid());
    }

    public int getCurrentPid() {
        String[] nameParts = ManagementFactory.getRuntimeMXBean().getName().split("@", 2);
        assertEquals("Cannot determine PID", 2, nameParts.length);
        return Integer.valueOf(nameParts[0]);
    }

    /*
     * A Linux-only utility method.
     */
    public int getLoginUid() throws IOException {
        FileReader fr = null;
        StringBuilder uidText = new StringBuilder();
        try {
            fr = new FileReader("/proc/self/loginuid");
            char[] buf = new char[16];
            int read = -1;
            while ((read = fr.read(buf)) > -1) {
                uidText.append(buf, 0, read);
            }
        } catch (IOException ioe) {
            fail("Unable to determine login uid: " + ioe.getMessage());
        } finally {
            fr.close();
        }

        return Integer.valueOf(uidText.toString());
    }
}
