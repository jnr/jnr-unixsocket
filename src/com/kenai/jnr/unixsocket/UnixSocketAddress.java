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

public class UnixSocketAddress extends java.net.SocketAddress {
    private final SockAddrUnix address;

    UnixSocketAddress() {
        address = SockAddrUnix.create();
        address.setFamily(ProtocolFamily.PF_UNIX);
    }

    public UnixSocketAddress(java.io.File path) {
        address = SockAddrUnix.create();
        address.setFamily(ProtocolFamily.PF_UNIX);
        address.setPath(path.getAbsolutePath());
    }
    
    SockAddrUnix getStruct() {
        return address;
    }
    
    int length() {
        return address.length();
    }

    @Override
    public String toString() {
        return "[family=" + address.getFamily() + " path=" + address.getPath() + "]";
    }
}
