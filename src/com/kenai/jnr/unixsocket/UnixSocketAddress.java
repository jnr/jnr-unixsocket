
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
