package jnr.unixsocket.posix;

public final class AixTimeval extends Timeval {
    public final SignedLong tv_sec = new SignedLong();
    public final Signed32 tv_usec = new Signed32();

    public AixTimeval(jnr.ffi.Runtime runtime) {
        super(runtime);
    }

    public void setTime(long[] timeval) {
        assert timeval.length == 2;
        tv_sec.set(timeval[0]);
        tv_usec.set((int)timeval[1]);
    }

    public void sec(long sec) {
        this.tv_sec.set(sec);
    }

    public void usec(long usec) {
        this.tv_usec.set((int)usec);
    }

    public long sec() {
        return tv_sec.get();
    }

    public long usec() {
        return tv_usec.get();
    }
}
