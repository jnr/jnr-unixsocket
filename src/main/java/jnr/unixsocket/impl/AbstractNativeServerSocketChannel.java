/*
 * Copyright (C) 2019 Jesse Wilson
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

package jnr.unixsocket.impl;

import jnr.constants.platform.Shutdown;
import jnr.enxio.channels.Native;
import jnr.enxio.channels.NativeServerSocketChannel;

import java.io.IOException;
import java.nio.channels.spi.SelectorProvider;

public abstract class AbstractNativeServerSocketChannel extends NativeServerSocketChannel {
    public AbstractNativeServerSocketChannel(int fd) {
        super(fd);
    }

    public AbstractNativeServerSocketChannel(SelectorProvider provider, int fd, int ops) {
        super(provider, fd, ops);
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {
        // Shutdown to interrupt any potentially blocked threads. This is necessary on Linux.
        Native.shutdown(getFD(), SHUT_RD);
        Native.close(getFD());
    }

    private static final int SHUT_RD = Shutdown.SHUT_RD.intValue();
}
