/*
 * Copyright (C) 2009 Wayne Meissner
 *
 * This file is part of java native runtime.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.kenai.jnr.unixsocket;

import com.kenai.constantine.platform.ProtocolFamily;
import com.kenai.constantine.platform.Sock;
import com.kenai.jaffl.LastError;
import com.kenai.jaffl.Library;
import com.kenai.jaffl.Platform;
import com.kenai.jaffl.annotations.In;
import com.kenai.jaffl.annotations.Out;
import com.kenai.jaffl.annotations.Transient;
import com.kenai.jaffl.byref.IntByReference;
import java.io.IOException;

class Native {
    static final String[] libnames = Platform.getPlatform().getOS() == Platform.OS.SOLARIS
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
        int fcntl(int fd, int cmd, int data);
        String strerror(int error);
    }

    static final LibC libsocket() {
        return LibC.INSTANCE;
    }

    static final LibC libc() {
        return LibC.INSTANCE;
    }

    static int socket(ProtocolFamily domain, Sock type, int protocol) throws IOException {
        int fd = libsocket().socket(domain.value(), type.value(), protocol);
        if (fd < 0) {
            throw new IOException(getLastErrorString());
        }
        return fd;
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
        return strerror(LastError.getLastError());
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
}
