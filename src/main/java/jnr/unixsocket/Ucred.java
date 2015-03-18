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

import jnr.ffi.Struct;

/**
 * Native structure for SCM_CREDENTIALS. See 'man 7 unix'.
 */
final class Ucred extends Struct {
    final pid_t pid = new pid_t();
    final uid_t uid = new uid_t();
    final gid_t gid = new gid_t();

    public Ucred() {
        super(jnr.ffi.Runtime.getSystemRuntime());
    }

    final pid_t getPidField() {
        return pid;
    }

    final uid_t getUidField() {
        return uid;
    }

    final gid_t getGidField() {
        return gid;
    }
}
