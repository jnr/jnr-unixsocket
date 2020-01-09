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

package jnr.unixsocket.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import jnr.constants.platform.Errno;
import jnr.enxio.channels.Native;
import jnr.enxio.channels.NativeException;

/**
 * Helper class, providing common methods.
 */
final class Common {

    private int _fd = -1;

    Common(int fd) {
        _fd = fd;
    }

    void setFD(int fd) {
        _fd = fd;
    }

    int getFD() {
        return _fd;
    }

    int read(ByteBuffer dst) throws IOException {

        ByteBuffer buffer = ByteBuffer.allocate(dst.remaining());

        int n = Native.read(_fd, buffer);

        buffer.flip();

        dst.put(buffer);

        switch (n) {
            case 0:
                return -1;

            case -1:
                Errno lastError = Native.getLastError();
                switch (lastError) {
                    case EAGAIN:
                    case EWOULDBLOCK:
                        return 0;

                    default:
                        throw new NativeException(Native.getLastErrorString(), lastError);
                }

            default: {

                         return n;
            }
        }
    }

    long read(ByteBuffer[] dsts, int offset, int length)
        throws IOException {
        long total = 0;

        for (int i = 0; i < length; i++) {
            ByteBuffer dst = dsts[offset + i];
            long read = read(dst);
            if (read == -1) {
                return read;
            }
            total += read;
        }

        return total;
    }

    int write(ByteBuffer src) throws IOException {

        int r = src.remaining();
        
        ByteBuffer buffer = ByteBuffer.allocate(r);
        
        buffer.put(src);
        
        buffer.position(0);

        int n = Native.write(_fd, buffer);

        if (n >=0 ) {
            if (n < r) {
                src.position(src.position()-(r-n));
            }
        } else {
            Errno lastError = Native.getLastError();
            switch (lastError) {
                case EAGAIN:
                case EWOULDBLOCK:
                    src.position(src.position()-r);
                    return 0;
            default:
                throw new NativeException(Native.getLastErrorString(), lastError);
            }
        }

        return n;
    }

    long write(ByteBuffer[] srcs, int offset, int length) throws IOException {

        long result = 0;

        for (int index = offset; index < length; ++index) {
            ByteBuffer buffer = srcs[index];
            int remaining = buffer.remaining();
            int written = 0;
            while (true) {
                int w = write(buffer);
                written += w;
                if (w == 0 || written == remaining) {
                    break;
                }
            }
            result += written;
            if (written < remaining) {
                break;
            }
        }

        return result;
    }

}
