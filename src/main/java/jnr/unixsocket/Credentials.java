/*
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
