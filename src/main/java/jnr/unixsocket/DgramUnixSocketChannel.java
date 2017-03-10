package jnr.unixsocket;

import jnr.constants.platform.Errno;
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;
import jnr.enxio.channels.NativeSocketChannel;
import jnr.ffi.LastError;
import jnr.ffi.byref.IntByReference;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SelectionKey;

/**
 *
 * DGRAM Unix Socket Channel.
 */
public class DgramUnixSocketChannel extends NativeSocketChannel {

  enum State {

    UNINITIALIZED,
    CONNECTED,
    IDLE,
    CONNECTING,
  }
  private volatile State state;
  private UnixSocketAddress remoteAddress = null;
  private UnixSocketAddress localAddress = null;
  private final Object stateLock = new Object();

  /**
   * Open a new DgramUnixSocketChannel.
   *
   * @return this
   * @throws IOException if socket fails to connect
   */
  public static final DgramUnixSocketChannel open() throws IOException {
    return new DgramUnixSocketChannel();
  }

  /**
   * connect to an unixSocketAddress.
   *
   * @param remote unixSocketAddress
   * @return this
   * @throws IOException if socket fails to connect
   */
  public static final DgramUnixSocketChannel open(UnixSocketAddress remote) throws IOException {
    DgramUnixSocketChannel channel = new DgramUnixSocketChannel();
    channel.connect(remote);
    return channel;
  }

  public static final OutputStream openOutputStream(UnixSocketAddress remote) throws IOException {
    DgramUnixSocketChannel channel = new DgramUnixSocketChannel();
    channel.connect(remote);
    return Channels.newOutputStream(channel);
  }

  /**
   * invoke native socketpair.
   *
   * @return an array of two DgramUnixSocketChannels
   * @throws IOException if socket fails to connect
   */
  public static final DgramUnixSocketChannel[] pair() throws IOException {
    int[] sockets = {-1, -1};
    Native.socketpair(ProtocolFamily.PF_UNIX, Sock.SOCK_DGRAM, 0, sockets);
    return new DgramUnixSocketChannel[]{
        new DgramUnixSocketChannel(sockets[0], SelectionKey.OP_READ | SelectionKey.OP_WRITE),
        new DgramUnixSocketChannel(sockets[1], SelectionKey.OP_READ | SelectionKey.OP_WRITE)
    };
  }

  private DgramUnixSocketChannel() throws IOException {
    super(Native.socket(ProtocolFamily.PF_UNIX, Sock.SOCK_DGRAM, 0),
        SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    state = State.IDLE;
  }

  DgramUnixSocketChannel(int fd, int ops) {
    super(fd, ops);
    state = State.CONNECTED;
  }

  DgramUnixSocketChannel(int fd, UnixSocketAddress remote) {
    super(fd, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    state = State.CONNECTED;
    remoteAddress = remote;
  }

  private boolean doConnect(SockAddrUnix remote) throws IOException {
    if (Native.connect(getFD(), remote, remote.length()) != 0) {
      Errno error = Errno.valueOf(LastError.getLastError(jnr.ffi.Runtime.getSystemRuntime()));

      switch (error) {
        case EAGAIN:
        case EWOULDBLOCK:
          return false;

        default:
          throw new IOException(error.toString());
      }
    }
    return true;
  }

  /**
   * check whether remote unix socket is connected.
   *
   * @param remote remote unix socket address
   * @return whether remote unix socket is connected
   * @throws IOException if socket fails to connect
   */
  public boolean connect(UnixSocketAddress remote) throws IOException {
    remoteAddress = remote;
    if (!doConnect(remoteAddress.getStruct())) {

      state = State.CONNECTING;
      return false;

    } else {

      state = State.CONNECTED;
      return true;
    }
  }

  /**
   * check whether current socket is connected.
   *
   * @return whether current connection is connected
   */
  public boolean isConnected() {
    return state == State.CONNECTED;
  }

  /**
   * check whether current socket is connect pending.
   *
   * @return whether current socket channel is connect pending
   */
  public boolean isConnectionPending() {
    return state == State.CONNECTING;
  }

  /**
   * whether connecting finished.
   *
   * @return whether connecting is finish
   * @throws IOException if socket fails to connect
   */
  public boolean finishConnect() throws IOException {
    switch (state) {
      case CONNECTED:
        return true;

      case CONNECTING:
        if (!doConnect(remoteAddress.getStruct())) {
          return false;
        }
        state = State.CONNECTED;
        return true;

      default:
        throw new IllegalStateException("socket is not waiting for connect to complete");
    }
  }

  /**
   * Retrieves the address of remote socket current channel connecting to.
   *
   * @return the address of remote socket current channel connecting to, null if
   * not connected.
   */
  public final UnixSocketAddress getRemoteSocketAddress() {
    if (state != State.CONNECTED) {
      return null;
    }
    return remoteAddress != null ? remoteAddress : (remoteAddress = getpeername(getFD()));
  }

  /**
   * Retrieves the address of local socket current channel connecting to. If
   * this socket channel is not in a connected state, this method will return
   * null.
   *
   * @return the address of local socket current channel connecting to, null if
   * not connected.
   */
  public final UnixSocketAddress getLocalSocketAddress() {
    if (state != State.CONNECTED) {
      return null;
    }

    return localAddress != null ? localAddress : (localAddress = getsockname(getFD()));
  }

  /**
   * Retrieves the credentials for this UNIX socket. If this socket channel is
   * not in a connected state, this method will return null.
   *
   * <b>man unix 7; SCM_CREDENTIALS </b>
   *
   * @throws UnsupportedOperationException if the underlying socket library
   * doesn't support the SO_PEERCRED option
   *
   * @return the credentials of the remote; null if not connected
   */
  public final Credentials getCredentials() {
    if (state != State.CONNECTED) {
      return null;
    }

    return Credentials.getCredentials(getFD());
  }

  static UnixSocketAddress getpeername(int sockfd) {
    UnixSocketAddress remote = new UnixSocketAddress();
    IntByReference len = new IntByReference(remote.getStruct().getMaximumLength());

    if (Native.libc().getpeername(sockfd, remote.getStruct(), len) < 0) {
      throw new Error(Native.getLastErrorString());
    }

    return remote;
  }

  static UnixSocketAddress getsockname(int sockfd) {
    UnixSocketAddress remote = new UnixSocketAddress();
    IntByReference len = new IntByReference(remote.getStruct().getMaximumLength());

    if (Native.libc().getsockname(sockfd, remote.getStruct(), len) < 0) {
      throw new Error(Native.getLastErrorString());
    }

    return remote;
  }

  /**
   * Retrieves whether socket is KeepAlive by invoke Native getsockopt.
   *
   * @return whether socket is KeepAlive
   */
  public boolean getKeepAlive() {
    int ret = Native.getsockopt(getFD(), SocketLevel.SOL_SOCKET,
        SocketOption.SO_KEEPALIVE.intValue());
    return ret == 1;
  }

  /**
   * Invoke native setsockopt to set socket keep alive.
   *
   * @param on whether socket should keep alive on
   */
  public void setKeepAlive(boolean on) {
    Native.setsockopt(getFD(), SocketLevel.SOL_SOCKET, SocketOption.SO_KEEPALIVE, on);
  }

  /**
   * Retrieves socket timeout time by Invoke native getsockopt to get.
   *
   * @return unit of timeout in seconds
   */
  public int getSoTimeout() {
    return Native.getsockopt(getFD(), SocketLevel.SOL_SOCKET, SocketOption.SO_RCVTIMEO.intValue());
  }

  /**
   * Invoke native setsockopt to set socket timeout time.
   *
   * @param timeout unit of timeout in seconds
   */
  public void setSoTimeout(int timeout) {
    Native.setsockopt(getFD(), SocketLevel.SOL_SOCKET, SocketOption.SO_RCVTIMEO, timeout);
  }
}
