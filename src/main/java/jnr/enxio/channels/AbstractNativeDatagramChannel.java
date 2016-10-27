/*
 * Copyright (C) 2016 Fritz Elfert
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

package jnr.enxio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.spi.SelectorProvider;

import jnr.constants.platform.Errno;

/**
 * A {@link java.nio.channels.Channel} implementation that uses a native unix socket
 */
public abstract class AbstractNativeDatagramChannel extends DatagramChannel
        implements ByteChannel, NativeSelectableChannel {

	private int fd = -1;

	public AbstractNativeDatagramChannel(int fd) {
		this(NativeSelectorProvider.getInstance(), fd);
	}

	AbstractNativeDatagramChannel(SelectorProvider provider, int fd) {
		super(provider);
		this.fd = fd;
	}

	public void setFD(int fd) {
		this.fd = fd;
	}

	@Override
	protected void implCloseSelectableChannel() throws IOException {
		Native.close(fd);
	}

	@Override
	protected void implConfigureBlocking(boolean block) throws IOException {
		Native.setBlocking(fd, block);
	}

	public final int getFD() {
		return fd;
	}

	public int read(ByteBuffer dst) throws IOException {

		ByteBuffer buffer = ByteBuffer.allocate(dst.remaining());

		int n = Native.read(fd, buffer);

		buffer.flip();

		dst.put(buffer);

		switch (n) {
		case 0:
			return -1;

		case -1:
			Errno lastError = Native.getLastError();
			switch (lastError) {
			case EAGAIN:
			case EWOULDBLOCK:
				return 0;

			default:
				throw new IOException(Native.getLastErrorString());
			}

		default: {

			return n;
		}
		}
	}

	@Override
	public long read(ByteBuffer[] dsts, int offset, int length)
			throws IOException {
		long total = 0;

		for (int i = 0; i < length; i++) {
			ByteBuffer dst = dsts[offset + i];
			long read = read(dst);
			if (read == -1) {
				return read;
			}
			total += read;
		}

		return total;
	}

	public int write(ByteBuffer src) throws IOException {

		ByteBuffer buffer = ByteBuffer.allocate(src.remaining());

		buffer.put(src);

		buffer.position(0);

		int n = Native.write(fd, buffer);

		if (n < 0) {
			throw new IOException(Native.getLastErrorString());
		}

		return n;
	}

	@Override
	public long write(ByteBuffer[] srcs, int offset, int length)
			throws IOException {

		long result = 0;
		int index = 0;

		for (index = offset; index < length; index++) {
			result += write(srcs[index]);
		}

		return result;
	}

}
