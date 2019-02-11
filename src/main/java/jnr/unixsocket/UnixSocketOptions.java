/*
 * Copyright (C) 2016 Fritz Elfert
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

import java.net.SocketOption;

/**
 * Defines common socket options for AF_UNIX sockets.
 */
public final class UnixSocketOptions {

    private static class GenericOption<T> implements SocketOption<T> {
        private final String name;
        private final Class<T> type;
        GenericOption(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }
        @Override public String name() { return name; }
        @Override public Class<T> type() { return type; }
        @Override public String toString() { return name; }
    }

    /**
     * Get/Set size of the socket send buffer.
     */
    public static final SocketOption<Integer> SO_SNDBUF =
        new GenericOption<Integer>("SO_SNDBUF", Integer.class);

    /**
     * Get/Set send timeout.
     */
    public static final SocketOption<Integer> SO_SNDTIMEO =
        new GenericOption<Integer>("SO_SNDTIMEO", Integer.class);

    /**
     * Get/Set size of the socket receive buffer.
     */
    public static final SocketOption<Integer> SO_RCVBUF =
        new GenericOption<Integer>("SO_RCVBUF", Integer.class);

    /**
     * Get/Set receive timeout.
     */
    public static final SocketOption<Integer> SO_RCVTIMEO =
        new GenericOption<Integer>("SO_RCVTIMEO", Integer.class);

    /**
     * Keep connection alive.
     */
    public static final SocketOption<Boolean> SO_KEEPALIVE =
        new GenericOption<Boolean>("SO_KEEPALIVE", Boolean.class);

    /**
     * Fetch peer credentials.
     */
    public static final SocketOption<Credentials> SO_PEERCRED =
        new GenericOption<Credentials>("SO_PEERCRED", Credentials.class);

    /**
     * Enable credential transmission.
     */
    public static final SocketOption<Boolean> SO_PASSCRED =
        new GenericOption<Boolean>("SO_PASSCRED", Boolean.class);

}

