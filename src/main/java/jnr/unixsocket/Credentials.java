/*
 * Copyright (C) 2014 Greg Vanore
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

import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;

/**
 * This class represents the peer credentials, retrievable from an AF_UNIX socket.
 * <p>
 * An instance of this class can be retrieved, using either the socket-level methods
 * {@link UnixSocket#getCredentials} and {@link UnixDatagramSocket#getCredentials} or by specifying
 * {@link UnixSocketOptions#SO_PEERCRED} as argument to one of the
 * channel-level methods {@link UnixSocketChannel#getOption} and {@link UnixDatagramChannel#getOption}.
 * <p>
 * <b>See also:</b> <a href="http://man7.org/linux/man-pages/man7/socket.7.html">socket (7)</a>
 */
public final class Credentials {
    private final Ucred ucred;

    Credentials(Ucred ucred) {
        this.ucred = ucred;
    }

    /**
     * Retrieves the peer's process ID.
     * @return The PID.
     */
    public int getPid() {
        return ucred.getPidField().intValue();
    }

    /**
     * Retrieves the peer's numeric effective user ID.
     * @return The EUID.
     */
    public int getUid() {
        return ucred.getUidField().intValue();
    }

    /**
     * Retrieves the peer's numeric effective group ID.
     * @return The EGID.
     */
    public int getGid() {
        return ucred.getGidField().intValue();
    }

    /**
     * Returns a human readable description of this instance.
     */
    @Override
    public java.lang.String toString() {
        return java.lang.String.format("[uid=%d gid=%d pid=%d]", getUid(), getGid(), getPid());
    }

    static Credentials getCredentials(int fd) {
        Ucred c = new Ucred();
        int error = Native.getsockopt(fd, SocketLevel.SOL_SOCKET, SocketOption.SO_PEERCRED, c);
        if (error != 0) {
            throw new UnsupportedOperationException(Native.getLastErrorString());
        }

        return new Credentials(c);
    }
}
