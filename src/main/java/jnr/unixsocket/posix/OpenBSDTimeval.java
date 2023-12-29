package jnr.unixsocket.posix;

public final class OpenBSDTimeval extends Timeval {
    public final Signed64 tv_sec = new Signed64();
    public final SignedLong tv_usec = new SignedLong();

    public OpenBSDTimeval(jnr.ffi.Runtime runtime) {
        super(runtime);
    }

    public void setTime(long[] timeval) {
        assert timeval.length == 2;
        tv_sec.set(timeval[0]);
        tv_usec.set(timeval[1]);
    }

    public void sec(long sec) {
        this.tv_sec.set(sec);
    }

    public void usec(long usec) {
        this.tv_usec.set(usec);
    }

    public long sec() {
        return tv_sec.get();
    }

    public long usec() {
        return tv_usec.get();
    }
}
