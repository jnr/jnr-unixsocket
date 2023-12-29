package jnr.unixsocket.posix;

import jnr.ffi.Struct;

abstract public class Timeval extends Struct {
    public Timeval(jnr.ffi.Runtime runtime) {
        super(runtime);
    }
    abstract public void setTime(long[] timeval);
    public abstract void sec(long sec);
    public abstract void usec(long usec);
    public abstract long sec();
    public abstract long usec();
}
