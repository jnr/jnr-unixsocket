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

import jnr.ffi.Platform;
import jnr.ffi.Platform.OS;

public class UnixSocketAddress extends java.net.SocketAddress {

    private static transient OS currentOS = Platform.getNativePlatform().getOS();

    private static final long serialVersionUID = 4821337010221569096L;
    private final transient SockAddrUnix address;

    UnixSocketAddress() {
        address = SockAddrUnix.create();
        address.setFamily(ProtocolFamily.PF_UNIX);
    }

    public UnixSocketAddress(java.io.File path) {
        address = SockAddrUnix.create();
        address.setFamily(ProtocolFamily.PF_UNIX);
        address.setPath(path.getPath());
    }

    public UnixSocketAddress(final String path) {
        address = SockAddrUnix.create();
        address.setFamily(ProtocolFamily.PF_UNIX);
        address.setPath(path);
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

    private String humanReadablePath() {
        String ret = path();
        // Handle abstract namespace like netstat: replace NUL by '@'
        if (ret.indexOf('\000') == 0) {
            return ret.replace('\000', '@');
        }
        return ret;
    }

    @Override
    public String toString() {
        return "[family=" + address.getFamily() + " path=" + humanReadablePath() + "]";
    }

    @Override
    public boolean equals(Object _other) {
        if (!(_other instanceof UnixSocketAddress)) {
            return false;
        }

        UnixSocketAddress other = (UnixSocketAddress)_other;

        return address.getFamily() == other.address.getFamily() &&
            path().equals(other.path());
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
}
