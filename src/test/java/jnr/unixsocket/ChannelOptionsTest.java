/*
 * Copyright (C) 2016 Fritz Elfert
 * 
 * (ported from https://github.com/softprops/unisockets/blob/master/unisockets-core/src/main/scala/Socket.scala)
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
 * 
 */
package jnr.unixsocket;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;

import java.util.Set;

import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

import jnr.ffi.Platform;
import jnr.ffi.Platform.OS;

import static junit.framework.Assert.*;

public class ChannelOptionsTest {

    @Test
    public void readonlyDatagramChannelOptionTest() throws Exception {
        UnixDatagramChannel[] sp = UnixDatagramChannel.pair();
        UnixDatagramChannel ch = sp[0];
        Credentials c = ch.socket().getCredentials();
        try {
            // SO_PEERCRED is readonly
            ch.setOption(UnixSocketOptions.SO_PEERCRED, c);
            fail("Should have thrown AssertionError");
        } catch (AssertionError ae) {
            assertEquals("exception message", ae.getMessage(), "Option not found or not writable");
        }
    }

    @Test
    public void readonlySocketChannelOptionTest() throws Exception {
        UnixSocketChannel[] sp = UnixSocketChannel.pair();
        UnixSocketChannel ch = sp[0];
        Credentials c = ch.socket().getCredentials();
        try {
            // SO_PEERCRED is readonly
            ch.setOption(UnixSocketOptions.SO_PEERCRED, c);
            fail("Should have thrown AssertionError");
        } catch (AssertionError ae) {
            assertEquals("exception message", ae.getMessage(), "Option not found or not writable");
        }
    }

    @Test
    public void unsupportedChannelOptionTest() throws Exception {
        UnixDatagramChannel ch = UnixDatagramChannel.open();
        try {
            // SO_KEEPALIVE is suitable only for SOCK_STREAM sockets
            ch.getOption(UnixSocketOptions.SO_KEEPALIVE);
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException uoe) {
            assertEquals("exception message", uoe.getMessage(), "'SO_KEEPALIVE' not supported");
        }
    }

    @Test
    public void keepaliveOptionTest() throws Exception {
        UnixSocketChannel ch = UnixSocketChannel.open();
        boolean origValue = ch.getOption(UnixSocketOptions.SO_KEEPALIVE).booleanValue();
        assertEquals("Initial value of SO_KEEPALIVE", origValue, false);
        ch.setOption(UnixSocketOptions.SO_KEEPALIVE, Boolean.TRUE);
        boolean changedValue = ch.getOption(UnixSocketOptions.SO_KEEPALIVE).booleanValue();
        assertEquals("Changed value of SO_KEEPALIVE", changedValue, true);
        ch.setOption(UnixSocketOptions.SO_KEEPALIVE, Boolean.FALSE);
        changedValue = ch.getOption(UnixSocketOptions.SO_KEEPALIVE).booleanValue();
        assertEquals("Changed value of SO_KEEPALIVE", changedValue, origValue);
    }

    @Test
    public void invalidOptionValueTest() throws Exception {
        UnixSocketChannel ch = UnixSocketChannel.open();
        try {
            ch.setOption(UnixSocketOptions.SO_RCVTIMEO, Integer.valueOf(-1));
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            assertEquals("exception message", iae.getMessage(), "Invalid send/receive timeout");
        }
        try {
            ch.setOption(UnixSocketOptions.SO_SNDTIMEO, Integer.valueOf(-1));
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            assertEquals("exception message", iae.getMessage(), "Invalid send/receive timeout");
        }
        try {
            ch.setOption(UnixSocketOptions.SO_RCVBUF, Integer.valueOf(-1));
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            assertEquals("exception message", iae.getMessage(), "Invalid send/receive buffer size");
        }
        try {
            ch.setOption(UnixSocketOptions.SO_SNDBUF, Integer.valueOf(-1));
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            assertEquals("exception message", iae.getMessage(), "Invalid send/receive buffer size");
        }
    }

    @Test
    @Ignore
    // Linux doubles the values when setting. Check what other platforms do.
    public void socketBufferTest() throws Exception {
        UnixDatagramChannel ch = UnixDatagramChannel.open();
        int rxs = ch.getOption(UnixSocketOptions.SO_RCVBUF);
        int txs = ch.getOption(UnixSocketOptions.SO_SNDBUF);
        System.out.println(String.format("rxbuf=%d, txbuf=%d", rxs, txs));
        ch.setOption(UnixSocketOptions.SO_RCVBUF, rxs - 100);
        ch.setOption(UnixSocketOptions.SO_SNDBUF, txs - 100);
        rxs = ch.getOption(UnixSocketOptions.SO_RCVBUF);
        txs = ch.getOption(UnixSocketOptions.SO_SNDBUF);
        System.out.println(String.format("rxbuf=%d, txbuf=%d", rxs, txs));
    }
}
