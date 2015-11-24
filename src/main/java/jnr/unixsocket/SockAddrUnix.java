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
import jnr.ffi.*;

/**
 * Native unix domain socket address structure.
 */
abstract class SockAddrUnix extends Struct {
    public final static int ADDR_LENGTH = 108;
    
    protected abstract UTF8String getPathField();
    protected abstract NumberField getFamilyField();

    SockAddrUnix() {
        super(jnr.ffi.Runtime.getSystemRuntime());
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
        getPathField().set(path);
    }

    /**
     * Gets the file system path of this socket address
     *
     * @return A String
     */
    final java.lang.String getPath() {
        return getPathField().get();
    }

    /**
     * Gets the maximum length of this address (including len/family header)
     *
     * @return The maximum size of the address in bytes
     */
    int getMaximumLength() {
        return 2 + getPathField().length();
    }

    /**
     * Gets the actual length of this address (including len/family header)
     *
     * @return The actual size of this address, in bytes
     */
    int length() {
        return 2 + strlen(getPathField());
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
        protected final UTF8String getPathField() {
            return sun_addr;
        }
        protected final NumberField getFamilyField() {
            return sun_family;
        }
    }


    /**
     * An implementation of {@link SockAddrUnix} for Linux, Solaris, et, al
     */
    static final class DefaultSockAddrUnix extends SockAddrUnix {
        public final Unsigned16 sun_family = new Unsigned16();
        public final UTF8String sun_addr = new UTF8String(ADDR_LENGTH);

        protected final UTF8String getPathField() {
            return sun_addr;
        }

        protected final NumberField getFamilyField() {
            return sun_family;
        }
    }
}
