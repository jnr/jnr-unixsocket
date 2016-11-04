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

package jnr.unixsocket.example;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixDatagramChannel;

import jnr.ffi.Platform;
import jnr.ffi.Platform.OS;

import java.lang.management.ManagementFactory;

public class LocalSyslogClient {

    private StringBuffer line = new StringBuffer();
    private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd HH:mm:ss", Locale.US);


    private String formatDate() {
        synchronized(this) {
            return sdf.format(new Date());
        }
    }

    private void formatLine(final int pri, final String tag, final int pid, final String[] args) {
        line.setLength(0);
        line.append(String.format("<%d>", pri))
            .append(formatDate())
            .append(" ")
            .append(tag);
        if (0 < pid) {
            line.append(String.format("[%d]", pid));
        }
        line.append(":");
        for (String arg : args) {
            line.append(" ").append(arg);
        }
    }

    private enum Priority {
        LOG_EMERG,
        LOG_ALERT,
        LOG_CRIT,
        LOG_ERR,
        LOG_WARNING,
        LOG_NOTICE,
        LOG_INFO,
        LOG_DEBUG;
    }

    private enum Facility {
        LOG_KERN(0 << 3),
        LOG_USER(1 << 3),
        LOG_MAIL(2 << 3),
        LOG_DAEMON(3 << 3),
        LOG_AUTH(4 << 3),
        LOG_SYSLOG(5 << 3),
        LOG_LPR(6 << 3),
        LOG_NEWS(7 << 3),
        LOG_UUCP(8 << 3),
        LOG_CRON(9 << 3),
        LOG_AUTHPRIV(10 << 3),
        LOG_FTP(11 << 3),
        LOG_LOCAL0(16 << 3),
        LOG_LOCAL1(17 << 3),
        LOG_LOCAL2(18 << 3),
        LOG_LOCAL3(19 << 3),
        LOG_LOCAL4(20 << 3),
        LOG_LOCAL5(21 << 3),
        LOG_LOCAL6(22 << 3),
        LOG_LOCAL7(23 << 3);

        private int myValue;

        Facility(int value) {
            myValue = value;
        }

        public int getValue() {
            return myValue;
        }
    }

    private int makePri(Priority priority, Facility facility) {
        return priority.ordinal() | facility.getValue();
    }

    private String getSocketPath() {
        if (Platform.getNativePlatform().getOS() == OS.DARWIN) {
            return "/var/run/syslog";
        }
        return "/dev/log";
    }

    private int getPid() {
        String[] nameParts = ManagementFactory.getRuntimeMXBean().getName().split("@", 2);
        if (2 == nameParts.length) {
            return Integer.parseInt(nameParts[0]);
        }
        return 0;
    }

    private void doit(String[] args) throws IOException, InterruptedException {
        java.io.File path = new java.io.File(getSocketPath());
        if (!path.exists()) {
            throw new IOException(String.format("%s does not exist", path.getAbsolutePath()));
        }
        UnixSocketAddress address = new UnixSocketAddress(path);
        UnixDatagramChannel channel = UnixDatagramChannel.open();
        int pri = makePri(Priority.LOG_WARNING, Facility.LOG_DAEMON);
        int pid = getPid();
        String tag = "whatever";
        if (args.length > 0) {
            formatLine(pri, tag, pid, args);
            ByteBuffer buf = ByteBuffer.wrap(line.toString().getBytes(StandardCharsets.UTF_8));
            channel.send(buf, address);
        } else {
            formatLine(pri, tag, pid, new String[]{"The quick brown fox jumps\nover the lazy dog"});
            ByteBuffer buf = ByteBuffer.wrap(line.toString().getBytes(StandardCharsets.UTF_8));
            channel.send(buf, address);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        LocalSyslogClient client = new LocalSyslogClient();
        client.doit(args);
    }
}
