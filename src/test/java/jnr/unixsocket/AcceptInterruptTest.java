package jnr.unixsocket;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketAddress;
import junit.framework.Assert;

import org.junit.Test;

public class AcceptInterruptTest {
    @Test
    public void testAcceptCloseInterrupt() throws Exception {
        File file = File.createTempFile("test", ".sock");
        file.delete();
        file.deleteOnExit();
        
        final UnixServerSocketChannel channel = UnixServerSocketChannel.open();
        channel.socket().bind(new UnixSocketAddress(file));
        
        final AtomicBoolean run = new AtomicBoolean(true);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch complete = new CountDownLatch(1);
        Thread accept = new Acceptor(complete, start, channel, run);
        
        // Start accepting thread
        accept.setDaemon(true);
        accept.start();
        Assert.assertTrue(start.await(5,TimeUnit.SECONDS));
        
        // Mark as no longer running
        run.set(false);
        
        // Close and Interrupt
        channel.close();
        accept.interrupt();
        Assert.assertTrue(complete.await(5,TimeUnit.SECONDS));
    }

    @Test
    public void testAcceptInterrupt() throws Exception {
        File file = File.createTempFile("test", ".sock");
        file.delete();
        file.deleteOnExit();
        
        final UnixServerSocketChannel channel = UnixServerSocketChannel.open();
        channel.socket().bind(new UnixSocketAddress(file));
        
        final AtomicBoolean run = new AtomicBoolean(true);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch complete = new CountDownLatch(1);
        Thread accept = new Acceptor(complete, start, channel, run);
        
        // Start accepting thread
        accept.setDaemon(true);
        accept.start();
        Assert.assertTrue(start.await(5,TimeUnit.SECONDS));
        
        // Mark as no longer running
        run.set(false);
        
        accept.interrupt();
        Assert.assertTrue(complete.await(5,TimeUnit.SECONDS));
    }
        
    private final class Acceptor extends Thread {
	private final CountDownLatch complete;
	private final CountDownLatch start;
	private final UnixServerSocketChannel channel;
	private final AtomicBoolean run;

	private Acceptor(CountDownLatch complete, CountDownLatch start, UnixServerSocketChannel channel,
			AtomicBoolean run) {
	    this.complete = complete;
	    this.start = start;
	    this.channel = channel;
	    this.run = run;
	}

	@Override public void run() {
	    try {
		while(run.get()) {
		    if (start.getCount()>0)
			start.countDown();
		    try {
			channel.accept();
			System.err.println("accepted");
		    }
		    catch (IOException e) {
			e.printStackTrace();
		    }
		    finally {
			System.err.println("finally");
		    }
		}
	    }
	    finally {
		complete.countDown();
	    }
	}
    }
}
