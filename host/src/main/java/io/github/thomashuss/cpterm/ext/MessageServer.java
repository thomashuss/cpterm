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

package io.github.thomashuss.cpterm.ext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Implements a simple TCP server which runs on a separate thread and invokes an implemented method
 * when a message is received.  Only allows one connection at a time.
 */
public abstract class MessageServer
{
    private static final Logger logger = LoggerFactory.getLogger(MessageServer.class);
    private final int port;
    private volatile ServerSocket serverSocket;

    /**
     * Create a new server.
     *
     * @param port to listen on
     */
    public MessageServer(int port)
    {
        this.port = port;
    }

    /**
     * Start the server, only if it hasn't already been started.
     */
    public synchronized final void start()
    {
        if (serverSocket == null) {
            new Thread(this::run).start();
        }
    }

    /**
     * Stop the server.  Log any errors that occur.
     */
    public synchronized final void stop()
    {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("Couldn't stop server", e);
            }
        }
    }

    /**
     * Handle messages as they are received.
     *
     * @param in  the entire message received
     * @param out write to the client
     */
    public abstract void received(String in, PrintWriter out);

    /**
     * Listen for connections indefinitely.
     */
    private void run()
    {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            this.serverSocket = serverSocket;
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    if (clientSocket.getInetAddress().isLoopbackAddress()) {
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                             PrintWriter out = new PrintWriter(clientSocket.getOutputStream())) {
                            synchronized (this) {
                                received(in.readLine(), out);
                            }
                        }
                    }
                } catch (SocketException ignored) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("Server terminated unexpectedly", e);
        }
    }
}
