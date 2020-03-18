/*
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

package jnr.unixsocket.example;

import jnr.enxio.channels.NativeSelectorProvider;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import jnr.unixsocket.UnixServerSocket;
import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

public class UnixServer {

    public static void main(String[] args) throws IOException {
        java.io.File path = new java.io.File("/tmp/fubar.sock");
        path.deleteOnExit();
        UnixSocketAddress address = new UnixSocketAddress(path);
        UnixServerSocketChannel channel = UnixServerSocketChannel.open();

        try {
            Selector sel = NativeSelectorProvider.getInstance().openSelector();
            channel.configureBlocking(false);
            channel.socket().bind(address);
            channel.register(sel, SelectionKey.OP_ACCEPT, new ServerActor(channel, sel));

            while (sel.select() > 0) {
                Set<SelectionKey> keys = sel.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();
                boolean running = false;
                boolean cancelled = false;
                while ( iterator.hasNext()  ) {
                    SelectionKey k = iterator.next();
                    Actor a = (Actor) k.attachment();
                    if (a.rxready()) {
                        running = true;
                    } else {
                        k.cancel();
                        cancelled = true;
                    }
                    iterator.remove();
                }
                if (!running && cancelled) {
                    System.out.println("No Actors Running any more");
                    break;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(UnixServerSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("UnixServer EXIT");
    }

    static interface Actor {
        public boolean rxready();
    }

    static final class ServerActor implements Actor {
        private final UnixServerSocketChannel channel;
        private final Selector selector;

        public ServerActor(UnixServerSocketChannel channel, Selector selector) {
            this.channel = channel;
            this.selector = selector;
        }
        public final boolean rxready() {
            try {
                UnixSocketChannel client = channel.accept();
                client.configureBlocking(false);
                client.register(selector, SelectionKey.OP_READ, new ClientActor(client));
                return true;
            } catch (IOException ex) {
                return false;
            }
        }
    }
    static final class ClientActor implements Actor {
        private final UnixSocketChannel channel;

        public ClientActor(UnixSocketChannel channel) {
            this.channel = channel;
        }

        public final boolean rxready() {
            try {
                ByteBuffer buf = ByteBuffer.allocate(1024);
                int n;

                while ((n = channel.read(buf)) > 0) {
                    UnixSocketAddress remote = channel.getRemoteSocketAddress();
                    System.out.printf("Read in %d bytes from %s%n", n, remote);

                    if (n > 0) {
                        buf.flip();
                        channel.write(buf);
                        buf.clear();
                    } else if (n < 0) {
                        return false;
                    }
                }

            } catch (IOException ex) {
                ex.printStackTrace();
                return false;
            }
            return true;
        }
    }
}
