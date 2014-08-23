package jnr.unixsocket;

import jnr.ffi.Struct;

/**
 * Native structure for SCM_CREDENTIALS. See 'man unix 7'.
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
