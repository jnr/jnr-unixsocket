package jnr.unixsocket;

import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;

public final class Credentials {
    private final Ucred ucred;

    Credentials(Ucred ucred) {
        this.ucred = ucred;
    }

    public int getPid() {
        return ucred.getPidField().intValue();
    }

    public int getUid() {
        return ucred.getUidField().intValue();
    }

    public int getGid() {
        return ucred.getGidField().intValue();
    }

    @Override
    public java.lang.String toString() {
        return java.lang.String.format("[uid=%d gid=%d pid=%d]", getUid(), getGid(), getPid());
    }

    static final Credentials getCredentials(int fd) {
        Ucred c = new Ucred();
        int error = Native.getsockopt(fd, SocketLevel.SOL_SOCKET, SocketOption.SO_PEERCRED, c);
        if (error != 0) {
            throw new UnsupportedOperationException(Native.getLastErrorString());
        }

        return new Credentials(c);
    }
}
