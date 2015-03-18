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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;
import jnr.ffi.LastError;
import jnr.ffi.Library;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Platform;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.annotations.Transient;
import jnr.ffi.byref.IntByReference;
import jnr.posix.DefaultNativeTimeval;
import jnr.posix.Timeval;

class Native {
    static final String[] libnames = Platform.getNativePlatform().getOS() == Platform.OS.SOLARIS
                        ? new String[] { "socket", "nsl", "c" }
                        : new String[] { "c" };
    public static interface LibC {
        
        public static final int F_GETFL = jnr.constants.platform.Fcntl.F_GETFL.intValue();
        public static final int F_SETFL = jnr.constants.platform.Fcntl.F_SETFL.intValue();
        public static final int O_NONBLOCK = jnr.constants.platform.OpenFlags.O_NONBLOCK.intValue();

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
        int getsockopt(int s, int level, int optname, @Out Timeval optval, @In @Out IntByReference optlen);
        int setsockopt(int s, int level, int optname, @In ByteBuffer optval, int optlen);
        int setsockopt(int s, int level, int optname, @In Timeval optval, int optlen);
        String strerror(int error);
    }
    
    static final LibC INSTANCE;
    
    static {
        LibraryLoader<LibC> loader = LibraryLoader.create(LibC.class);
        for (String libraryName : libnames) {
            loader.library(libraryName);
        }
        INSTANCE = loader.load();
    }

    static final LibC libsocket() {
        return INSTANCE;
    }

    static final LibC libc() {
        return INSTANCE;
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
        return setsockopt(s, level, optname, optval ? 1 : 0);
    }

    public static int setsockopt(int s, SocketLevel level, SocketOption optname, int optval) {
        if (optname == SocketOption.SO_RCVTIMEO) {
            DefaultNativeTimeval t = new DefaultNativeTimeval(Runtime.getSystemRuntime());
            t.setTime(new long [] {optval / 1000, (optval % 1000) * 1000});
            return libsocket().setsockopt(s, level.intValue(), optname.intValue(), t, DefaultNativeTimeval.size(t));
        } else {
            ByteBuffer buf = ByteBuffer.allocate(4);
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.putInt(optval).flip();
            return libsocket().setsockopt(s, level.intValue(), optname.intValue(), buf, buf.remaining());
        }
    }

    public static int getsockopt (int s, SocketLevel level, int optname) {
        IntByReference ref;
        if (optname == SocketOption.SO_RCVTIMEO.intValue()) {
            DefaultNativeTimeval t = new DefaultNativeTimeval(Runtime.getSystemRuntime());
            ref = new IntByReference(DefaultNativeTimeval.size(t));
            Native.libsocket().getsockopt(s, level.intValue(), optname, t, ref);
            return (t.tv_sec.intValue() * 1000) + (t.tv_usec.intValue() / 1000);
        } else {
            ByteBuffer buf = ByteBuffer.allocate(4);
            buf.order(ByteOrder.BIG_ENDIAN);
            ref = new IntByReference(4);
            Native.libsocket().getsockopt(s, level.intValue(), optname, buf, ref);
            return buf.getInt();
        }
    }

    public static int getsockopt(int s, SocketLevel level, SocketOption optname, Struct data) {
        Pointer struct_ptr = Struct.getMemory(data);
        IntByReference ref = new IntByReference(Struct.size(data));
        ByteBuffer buf = ByteBuffer.wrap((byte[])struct_ptr.array());

        return Native.libsocket().getsockopt(s, level.intValue(), optname.intValue(), buf, ref);
    }

    public static boolean getboolsockopt (int s, SocketLevel level, int optname) {
        return getsockopt(s, level, optname) != 0;
    }
}
