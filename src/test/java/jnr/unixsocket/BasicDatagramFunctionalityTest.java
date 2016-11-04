/*
 * Copyright (C) 2016 Fritz Elfert
 * 
 * (ported from https://github.com/softprops/unisockets/blob/master/unisockets-core/src/main/scala/Socket.scala)
 *
 * This file is part of the JNR project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package jnr.unixsocket;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;

import java.util.Set;

import org.junit.Assume;
import org.junit.Test;

import jnr.ffi.Platform;
import jnr.ffi.Platform.OS;

import static junit.framework.Assert.*;

public class BasicDatagramFunctionalityTest {
    private static final String DATA = "foo bar baz. The quick brown fox jumps over the lazy dog. ";
    volatile Throwable serverException;
    volatile long received = 0;

    private UnixSocketAddress makeAddress() throws IOException {
        File socketFile = Files.createTempFile("jnr-unixsocket-test", ".sock").toFile();
        socketFile.delete();
        socketFile.deleteOnExit();
        return new UnixSocketAddress(socketFile);
    }

    private void basicOperation(final long minBytesToSend) throws Throwable {
        serverException = null;
        final StringBuffer rxdata = new StringBuffer();
        final StringBuffer txdata = new StringBuffer();
        final ByteBuffer rxbuf = ByteBuffer.allocate(1024);
        final ByteBuffer txbuf = ByteBuffer.allocate(2024);
        final UnixSocketAddress serverAddress = makeAddress();

        Thread serverThread = new Thread("server side") {
            final UnixDatagramChannel serverChannel = UnixDatagramChannel.open().bind(serverAddress);

            public void run() {
                while (null == serverException) {
                    try {
                        rxbuf.clear();
                        serverChannel.receive(rxbuf);
                        rxbuf.flip();
                        int count = rxbuf.limit();
                        rxdata.append(StandardCharsets.UTF_8.decode(rxbuf).toString());
                        received += count;;
                    } catch (IOException ex) {
                        serverException = ex;
                    }
                }
            }
        };
        serverThread.start();

        // client logic
        DatagramChannel clientChannel = UnixDatagramChannel.open();
        received = 0;
        long written = 0;
        while (null == serverException && written < minBytesToSend) {
            txbuf.put(StandardCharsets.UTF_8.encode(DATA));
            txbuf.flip();
            written += clientChannel.send(txbuf, serverAddress);
            txbuf.compact();
            txdata.append(DATA);
            if (null != serverException) {
                throw new Exception().initCause(serverException);
            }
        }
        clientChannel.close();
        while (null == serverException && received < written) {
            Thread.sleep(100);
        }

        assertTrue("More than 0 bytes written", written > 0);
        assertEquals("received", written, received);
        assertEquals("received data", txdata.toString(), rxdata.toString());
    }

    @Test
    public void smallBasicOperationTest() throws Throwable {
        basicOperation(DATA.length());
    }

    @Test
    public void largeBasicOperationTest() throws Throwable {
        Assume.assumeTrue(OS.LINUX == Platform.getNativePlatform().getOS());

        basicOperation(1000L * DATA.length());
    }

    @Test
    public void doubleBindTest() throws Exception {
        UnixDatagramChannel ch = UnixDatagramChannel.open().bind(null);
        try {
            ch.bind(null);
            fail("Should have thrown AlreadyBoundException");
        } catch (AlreadyBoundException ex) {
        }
        try {
            ch.socket().bind(null);
            fail("Should have thrown SocketException");
        } catch (SocketException ex) {
            assertEquals("exception message", ex.getMessage(), "already bound");
        }
    }

    @Test
    public void socketBufferTest() throws Exception {
        UnixDatagramChannel ch = UnixDatagramChannel.open();
        int rxs = ch.getOption(UnixSocketOptions.SO_RCVBUF);
        int txs = ch.getOption(UnixSocketOptions.SO_SNDBUF);
        System.out.println(String.format("rxbuf=%d, txbuf=%d", rxs, txs));
        ch.setOption(UnixSocketOptions.SO_RCVBUF, rxs - 100);
        ch.setOption(UnixSocketOptions.SO_SNDBUF, txs - 100);
        rxs = ch.getOption(UnixSocketOptions.SO_RCVBUF);
        txs = ch.getOption(UnixSocketOptions.SO_SNDBUF);
        System.out.println(String.format("rxbuf=%d, txbuf=%d", rxs, txs));
    }
}
