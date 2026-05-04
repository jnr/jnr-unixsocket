/*
 * Copyright (C) 2026
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
 */
package jnr.unixsocket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class UnixDatagramChannelRoundTripTest {

    private static UnixSocketAddress tempAddress() throws IOException {
        File socketFile = Files.createTempFile("jnr-unixsocket-roundtrip", ".sock").toFile();
        socketFile.delete();
        socketFile.deleteOnExit();
        return new UnixSocketAddress(socketFile);
    }

    @Test
    public void receiveAddressRoundTripCanBeUsedAsReplyTarget() throws Exception {
        final UnixSocketAddress serverAddress = tempAddress();
        final UnixSocketAddress clientAddress = tempAddress();

        final byte[] message = "hello".getBytes(StandardCharsets.UTF_8);
        final int iterations = 1000;

        try (UnixDatagramChannel server = UnixDatagramChannel.open().bind(serverAddress);
             UnixDatagramChannel client = UnixDatagramChannel.open().bind(clientAddress)) {

            for (int i = 0; i < iterations; i++) {
                ByteBuffer tx = ByteBuffer.wrap(message);
                int sent = client.send(tx, serverAddress);
                assertTrue("client->server send should send > 0 bytes", sent > 0);

                ByteBuffer rx = ByteBuffer.allocate(64);
                UnixSocketAddress receivedFrom = server.receive(rx);
                assertEquals("received sender path should match bound client path", clientAddress.humanReadablePath(), receivedFrom.humanReadablePath());

                // If receivedFrom contains trailing garbage bytes, this send may fail
                // with ENOENT (No such file or directory).
                ByteBuffer reply = ByteBuffer.wrap(message);

                int echoed = server.send(reply, receivedFrom);
                assertTrue("server reply send should send > 0 bytes", echoed > 0);

                ByteBuffer clientRx = ByteBuffer.allocate(64);
                UnixSocketAddress echoedFrom = client.receive(clientRx);
                assertEquals("server path should be stable", serverAddress.path(), echoedFrom.path());
            }
        }
    }
}

