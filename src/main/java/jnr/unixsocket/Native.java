/*
 * Copyright (C) 2009 Wayne Meissner
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

import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;
import jnr.ffi.*;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.annotations.Transient;
import jnr.ffi.byref.IntByReference;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class Native {
    static final String[] libnames = Platform.getNativePlatform().getOS() == Platform.OS.SOLARIS
                        ? new String[] { "socket", "nsl", "c" }
                        : new String[] { "c" };
    public static interface LibC {
        static final LibC INSTANCE = Library.loadLibrary(LibC.class, libnames);
        public static final int F_GETFL = com.kenai.constantine.platform.Fcntl.F_GETFL.value();
        public static final int F_SETFL = com.kenai.constantine.platform.Fcntl.F_SETFL.value();
        public static final int O_NONBLOCK = com.kenai.constantine.platform.OpenFlags.O_NONBLOCK.value();

        int socket(int domain, int type, int protocol);
        int listen(int fd, int backlog);
        int bind(int fd, @In @Out @Transient SockAddrUnix addr, int len);
        int accept(int fd, @Out SockAddrUnix addr, @In @Out IntByReference len);
        int connect(int s, @In @Transient SockAddrUnix name, int namelen);
        int getsockname(int fd, @Out SockAddrUnix addr, @In @Out IntByReference len);
        int getpeername(int fd, @Out SockAddrUnix addr, @In @Out IntByReference len);
        int socketpair(int domain, int type, int protocol, @Out int[] sv);
        int fcntl(int fd, int cmd, int data);
        int getsockopt(int s, int level, int optname, @Out ByteBuffer optval, @In @Out IntByReference optlen);
        int setsockopt(int s, int level, int optname, @In ByteBuffer optval, int optlen);
        String strerror(int error);
    }

    static final LibC libsocket() {
        return LibC.INSTANCE;
    }

    static final LibC libc() {
        return LibC.INSTANCE;
    }

    static int socket(ProtocolFamily domain, Sock type, int protocol) throws IOException {
        int fd = libsocket().socket(domain.intValue(), type.intValue(), protocol);
        if (fd < 0) {
            throw new IOException(getLastErrorString());
        }
        return fd;
    }

    static int socketpair(ProtocolFamily domain, Sock type, int protocol, int[] sv) throws IOException {
        if (libsocket().socketpair(domain.intValue(), type.intValue(), protocol, sv) < 0) {
            throw new IOException("socketpair(2) failed " + Native.getLastErrorString());
        }
        return 0;
    }

    static int listen(int fd, int backlog) {
        return libsocket().listen(fd, backlog);
    }

    static int bind(int fd, SockAddrUnix addr, int len) {
        return libsocket().bind(fd, addr, len);
    }

    static int accept(int fd, SockAddrUnix addr, IntByReference len) {
        return libsocket().accept(fd, addr, len);
    }

    static int connect(int fd, SockAddrUnix addr, int len) {
        return libsocket().connect(fd, addr, len);
    }

    static String getLastErrorString() {
        return strerror(LastError.getLastError(jnr.ffi.Runtime.getSystemRuntime()));
    }

    static String strerror(int error) {
        return libc().strerror(error);
    }

    public static void setBlocking(int fd, boolean block) {
        int flags = libc().fcntl(fd, LibC.F_GETFL, 0);
        if (block) {
            flags &= ~LibC.O_NONBLOCK;
        } else {
            flags |= LibC.O_NONBLOCK;
        }
        libc().fcntl(fd, LibC.F_SETFL, flags);
    }

    public static int setsockopt(int s, SocketLevel level, SocketOption optname, boolean optval) {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putInt(optval ? 1 : 0).flip();
        return libsocket().setsockopt(s, level.intValue(), optname.intValue(), buf, buf.remaining());
    }

    public static int getsockopt(int s, SocketLevel level, SocketOption optname, Struct data) {
        Pointer struct_ptr = Struct.getMemory(data);
        IntByReference ref = new IntByReference(Struct.size(data));
        ByteBuffer buf = ByteBuffer.wrap((byte[])struct_ptr.array());

        return Native.libsocket().getsockopt(s, level.intValue(), optname.intValue(), buf, ref);
    }
}
