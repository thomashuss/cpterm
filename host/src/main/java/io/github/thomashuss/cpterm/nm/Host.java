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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Sends and listens for messages to and from the extension.
 *
 * @param <T> type of message
 */
public abstract class Host<T>
{
    private static final Object lock = new Object();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final NativeMessagingInputStream nis = new NativeMessagingInputStream();
    private static final OutputStream os = System.out;
    private final Class<T> messageClass;

    public Host(Class<T> messageClass)
    {
        this.messageClass = messageClass;
    }

    /**
     * Listen for new messages and invoke {@link Host#received(T)} when a message is received.  Blocks until no more
     * messages can be received.
     *
     * @throws IOException if the message can't be read
     */
    public final void listen()
    throws IOException
    {
        while (nis.waitForMessage()) {
            synchronized (lock) {
                if (!received(mapper.readValue(nis, messageClass))) {
                    break;
                }
            }
        }
    }

    /**
     * Send a message to the extension.
     *
     * @param t message to send
     * @throws IOException if the message can't be sent
     */
    public final void send(T t)
    throws IOException
    {
        synchronized (lock) {
            byte[] ser = mapper.writeValueAsBytes(t);
            os.write(ser.length & 0xFF);
            os.write((ser.length >> 8) & 0xFF);
            os.write((ser.length >> 16) & 0xFF);
            os.write((ser.length >> 24) & 0xFF);
            os.write(ser);
        }
    }

    /**
     * Invoked on the same thread as {@link Host#listen()} when a message is received.  This method blocks listening.
     *
     * @param t received message
     * @return {@code true} if should continue listening for messages
     */
    public abstract boolean received(T t);
}
