package jnr.unixsocket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

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

    @Test
    public void testInterruptRead() throws Exception {
        Path socketPath = getTemporarySocketFileName();
        startServer(socketPath);

        int readTimeoutInMilliseconds = 5000;

        UnixSocket socket = createClient(socketPath, readTimeoutInMilliseconds);
        CountDownLatch readStartLatch = new CountDownLatch(1);
        ReadFromSocketRunnable runnable = new ReadFromSocketRunnable(readStartLatch, socket);

        Thread readThread = new Thread(runnable);

        readThread.setDaemon(true);

        long startTime = System.nanoTime();
        readThread.start();
        readStartLatch.await();
        Thread.sleep(100); // Wait for the thread to call read()
        socket.close();
        readThread.join();
        long stopTime = System.nanoTime();

        long duration = stopTime - startTime;
        long durationInMilliseconds = duration / 1_000_000;

        assertTrue("read() was not interrupted by close() before read() timed out", durationInMilliseconds < readTimeoutInMilliseconds);
        assertEquals("read() threw an exception", null, runnable.getThrownOnThread());
    }

    private Path getTemporarySocketFileName() throws IOException {
        Path socketPath = Files.createTempFile("jnr-unixsocket-tests", ".sock");
        Files.delete(socketPath);
        socketPath.toFile().deleteOnExit();

        return socketPath;
    }

    private void startServer(Path socketPath) throws IOException {
        UnixServerSocketChannel serverChannel = UnixServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new UnixSocketAddress(socketPath.toFile()));
    }

    private UnixSocket createClient(Path socketPath, int readTimeoutInMilliseconds) throws IOException {
        UnixSocketChannel clientChannel = UnixSocketChannel.open(new UnixSocketAddress(socketPath.toFile()));
        UnixSocket socket = new UnixSocket(clientChannel);
        socket.setSoTimeout(readTimeoutInMilliseconds);

        return socket;
    }

    private class ReadFromSocketRunnable implements Runnable {
        private CountDownLatch readStartLatch;
        private UnixSocket socket;
        private IOException thrownOnThread;

        private ReadFromSocketRunnable(CountDownLatch readStartLatch, UnixSocket socket) {
            this.readStartLatch = readStartLatch;
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                readStartLatch.countDown();
                socket.getInputStream().read();
            } catch (IOException e) {
                // EBADF (bad file descriptor) is thrown when read() is interrupted
                if (!e.getMessage().equals("Bad file descriptor")) {
                    thrownOnThread = e;
                }
            }
        }

        private IOException getThrownOnThread() {
            return thrownOnThread;
        }
    }
}
