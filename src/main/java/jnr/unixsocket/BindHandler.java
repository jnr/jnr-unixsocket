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

import java.io.IOException;
import java.net.SocketAddress;

import java.nio.channels.UnsupportedAddressTypeException;
import java.nio.channels.AlreadyBoundException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class, providing common handling of bind() handling.
 */
final class BindHandler {
    private final AtomicBoolean bound;

    BindHandler(boolean initialState) {
        bound = new AtomicBoolean(initialState);
    }

    boolean isBound() {
        return bound.get();
    }

    synchronized UnixSocketAddress bind(int fd, SocketAddress local) throws IOException {
        if (null != local && !(local instanceof UnixSocketAddress)) {
            throw new UnsupportedAddressTypeException();
        }
        if (bound.get()) {
            throw new AlreadyBoundException();
        } else {
            UnixSocketAddress ret = Common.bind(fd, (UnixSocketAddress)local);
            bound.set(true);
            return ret;
        }
    }

}
