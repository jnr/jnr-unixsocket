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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import jnr.constants.platform.ProtocolFamily;

/**
 * This class represents an AF_UNIX-style socket address.
 * On Linux, it supports the platform-specific abstract name space.
 * <p>
 * Using an abstract name space is denoted by the socket path starting with
 * a NUL byte. Sockets in abstract name space have no entry in the file system.
 * When linux performs autobind, it constructs the resulting path with a
 * leading NUL, followed by a unique 5-digit hexadecimal number.
 */
public class UnixSocketAddress extends java.net.SocketAddress {

    private static final long serialVersionUID = 4821337010221569096L;
    private transient SockAddrUnix address;

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

    /**
     * Retrieves the path.
     * @return The path of this AF_UNIX address.
     * Note: On Linux,  can contain a leading NUL byte, if this address
     * resides in abstract namespace.
     */
    public String path() {
        return address.getPath();
    }

    /**
     * Returns a human readable path.
     * On Linux, AF_UNIX sockets can be bound/connected in abstract namespace.
     * This is denoted by a leading NUL byte in the path.
     * In order to be properly displayed, this method returns a path prefixed
     * by '@' like netstat, lsof an similar tools.
     * @return The human readable path of this address.
     */
    public String humanReadablePath() {
        String ret = path();
        // Handle abstract namespace like netstat: replace NUL by '@'
        if (ret.indexOf('\000') == 0) {
            return ret.replace('\000', '@');
        }
        return ret;
    }

    /**
     * Retrieves a human readable description of this address.
     * @return The human readable description of this address.
     */
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

    // Serializable
    private void writeObject(ObjectOutputStream o) throws IOException {
        o.defaultWriteObject();
        o.writeObject(path());
    }

    private void readObject(ObjectInputStream o)
        throws IOException, ClassNotFoundException {
        o.defaultReadObject();
        String path = (String)o.readObject();
        if (null == address) {
            address = SockAddrUnix.create();
        }
        address.setPath(path);
        address.setFamily(ProtocolFamily.PF_UNIX);
    }
}
