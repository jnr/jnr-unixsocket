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

import static java.nio.charset.StandardCharsets.UTF_8;

import jnr.constants.platform.ProtocolFamily;
import jnr.ffi.Platform;
import jnr.ffi.Platform.OS;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;

/**
 * Native unix domain socket address structure.
 */
abstract class SockAddrUnix extends Struct {

    private static transient OS currentOS = Platform.getNativePlatform().getOS();
    public final static int ADDR_LENGTH = 108;
    public final static int HEADER_LENGTH = 2;

    protected abstract UTF8String getPathField();
    protected abstract NumberField getFamilyField();

    // This is important to be cached here for supporting abstract namespace on Linux
    // (which starts with a NUL byte. path is NOT NUL terminated in this case!)
    private java.lang.String cachedPath;

    SockAddrUnix() {
        super(Runtime.getSystemRuntime());
    }

    /**
     * Sets the protocol family of this unix socket address.
     *
     * @param family The protocol family, usually {@link ProtocolFamily#PF_UNIX}
     */
    final void setFamily(ProtocolFamily family) {
        getFamilyField().set(family.intValue());
    }


    /**
     * Gets the protocol family of this unix socket address.
     *
     * @return The protocol family
     */
    final ProtocolFamily getFamily() {
        return ProtocolFamily.valueOf(getFamilyField().intValue());
    }

    /**
     * Sets the file system path of this socket address
     *
     * @param path The unix socket address
     */
    void setPath(java.lang.String path) {
        cachedPath = path;
        getPathField().set(cachedPath);
    }

    /**
     * Updates the file system path of this socket address.
     * In order to support abstract namespaces, this MUST be
     * called after any native syscall that sets this
     * path struct like getsockname(), getpeername(), accept().
     *
     * @param len the value of the addrlen var, set by the above syscalls.
     */
    void updatePath(final int len) {
        if (currentOS == OS.LINUX) {
            // Linux always returns an accurate length in
            // order to support abstract namespace, where
            // path STARTS with a NUL byte.
            cachedPath = len == HEADER_LENGTH ? "" : getPath(len - HEADER_LENGTH);
        } else {
            // All others might return a len > 0 (typically 14) AND the path is terminated
            // by a NUL byte if it is shorter than sizeof(sun_path)
            cachedPath = getPathField().get();
            int slen = len - HEADER_LENGTH;
            if (slen <= 0) {
                cachedPath = "";
            } else {
                if (slen < getPathField().length() && slen < cachedPath.length()) {
                    cachedPath = cachedPath.substring(0, slen);
                }
            }
        }
    }

    /**
     * Gets the file system path of this socket address
     *
     * @return A String
     */
    final java.lang.String getPath() {
        if (null == cachedPath) {
            cachedPath = getPathField().get();
        }
        return cachedPath;
    }

    /**
     * Gets the path of this socket address, supporting abstract namespace on Linux.
     *
     * @param len The desired length of the string.
     * If the first character of the path is NUL, then this value ist considered
     * exact, otherwise it includes a trailing NUL charater and therefore the actual
     * string length is len - 1.
     */
    final java.lang.String getPath(int len) {
        UTF8String str = getPathField();
        byte [] ba = new byte[str.length()];
        str.getMemory().get(str.offset(), ba, 0, len);
        if (0 != ba[0]) {
            len -= 1;
        }
        return new java.lang.String(java.util.Arrays.copyOf(ba, len), UTF_8);
    }

    /**
     * Gets the maximum length of this address (including len/family header)
     *
     * @return The maximum size of the address in bytes
     */
    int getMaximumLength() {
        return HEADER_LENGTH + getPathField().length();
    }

    /**
     * Gets the actual length of this address (including len/family header)
     *
     * @return The actual size of this address, in bytes
     */
    int length() {
        if (currentOS == OS.LINUX && null != cachedPath) {
            return HEADER_LENGTH + cachedPath.length();
        }
        return HEADER_LENGTH + strlen(getPathField());
    }

    /**
     * Gets len/family header length
     *
     * @return The size of header, in bytes
     */
    int getHeaderLength() {
        return HEADER_LENGTH;
    }

    
    /**
     * Creates a new instance of <tt>SockAddrUnix</tt>
     *
     * @return An instance of <tt>SockAddrUnix</tt>
     */
    static SockAddrUnix create() {
        return Platform.getNativePlatform().isBSD() ? new BSDSockAddrUnix() : new DefaultSockAddrUnix();
    }

    private static final int strlen(UTF8String str) {
        int end = str.getMemory().indexOf(str.offset(), (byte) 0);
        return end >= 0 ? end : str.length();
    }
    
    /**
     * An implementation of {@link SockAddrUnix} for BSD systems 
     */
    static final class BSDSockAddrUnix extends SockAddrUnix {

        public final Unsigned8 sun_len = new Unsigned8();
        public final Unsigned8 sun_family = new Unsigned8();
        public final UTF8String sun_addr = new UTF8String(ADDR_LENGTH);

        @Override
        public void setPath(java.lang.String path) {
            super.setPath(path);
            sun_len.set(path.length());
        }
        protected UTF8String getPathField() {
            return sun_addr;
        }
        protected NumberField getFamilyField() {
            return sun_family;
        }
    }


    /**
     * An implementation of {@link SockAddrUnix} for Linux, Solaris, et, al
     */
    static final class DefaultSockAddrUnix extends SockAddrUnix {
        public final Unsigned16 sun_family = new Unsigned16();
        public final UTF8String sun_addr = new UTF8String(ADDR_LENGTH);

        protected UTF8String getPathField() {
            return sun_addr;
        }

        protected NumberField getFamilyField() {
            return sun_family;
        }
    }
}
