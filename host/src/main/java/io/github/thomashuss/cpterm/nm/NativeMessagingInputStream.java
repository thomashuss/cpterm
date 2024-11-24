/*
 *  Copyright (C) 2024 Thomas Huss
 *
 *  CPTerm is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later
 *  version.
 *
 *  CPTerm is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program. If not, see https://www.gnu.org/licenses/.
 */

package io.github.thomashuss.cpterm.nm;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provides a fixed length stream from the "indeterminate length" {@link System#in} stream so that Jackson
 * can deserialize a single message.
 */
class NativeMessagingInputStream
        extends InputStream
{
    private static final InputStream in = System.in;
    private int u_length;
    private int u_pos;

    /**
     * Listen for messages.  Blocks until a message is in the stream or no more messages can be read.
     *
     * @return {@code true} if a message can be read, {@code false} otherwise
     * @throws IOException from {@link InputStream#read()}
     */
    boolean waitForMessage()
    throws IOException
    {
        byte b0 = (byte) in.read();
        if (b0 == -1) return false;
        byte b1 = (byte) in.read();
        byte b2 = (byte) in.read();
        byte b3 = (byte) in.read();
        u_length = (b3 << 24) & 0xff000000 | (b2 << 16) & 0x00ff0000 | (b1 << 8) & 0x0000ff00
                | (b0) & 0x000000ff;
        u_pos = 0;
        return true;
    }

    @Override
    public int available()
    throws IOException
    {
        if (Integer.compareUnsigned(u_pos, u_length) >= 0) {
            return 0;
        }
        int u_remaining = u_length - u_pos;
        return Math.min(in.available(),
                Integer.compareUnsigned(u_remaining, Integer.MAX_VALUE) >= 0 ? Integer.MAX_VALUE : u_remaining);
    }

    @Override
    public int read()
    throws IOException
    {
        if (Integer.compareUnsigned(u_pos, u_length) >= 0) {
            return -1;
        }
        int ret = in.read();
        if (ret != -1) {
            u_pos++;
        }
        return ret;
    }

    @Override
    public int read(byte[] buffer, int offset, int len)
    throws IOException
    {
        int a = available();
        if (a == -1) {
            return -1;
        }
        int ret = in.read(buffer, offset, Math.min(len, a));
        if (ret != -1) {
            u_pos += ret;
        }
        return ret;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void close()
    throws IOException
    {
        while (read() != -1) ;
    }
}
