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

public class UnixSocketAddress extends java.net.SocketAddress {
    private final SockAddrUnix address;

    UnixSocketAddress() {
        address = SockAddrUnix.create();
        address.setFamily(ProtocolFamily.PF_UNIX);
    }

    public UnixSocketAddress(java.io.File path) {
        address = SockAddrUnix.create();
        address.setFamily(ProtocolFamily.PF_UNIX);
        address.setPath(path.getPath());
    }
    
    SockAddrUnix getStruct() {
        return address;
    }
    
    int length() {
        return address.length();
    }

    public String path() {
        return address.getPath();
    }

    @Override
    public String toString() {
        return "[family=" + address.getFamily() + " path=" + address.getPath() + "]";
    }

    @Override
    public boolean equals(Object _other) {
        if (!(_other instanceof UnixSocketAddress)) return false;

        UnixSocketAddress other = (UnixSocketAddress)_other;

        return address.getFamily() == other.address.getFamily() &&
                address.getPath().equals(other.address.getPath());
    }
}
