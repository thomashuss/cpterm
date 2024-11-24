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

package io.github.thomashuss.cpterm.artifacts.code;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs a thread which invokes the implemented {@link #modified} method when the file at the provided path
 * is modified.
 */
public abstract class Watcher
{
    private static final Logger logger = LoggerFactory.getLogger(Watcher.class);
    private static final WatchService watcher;

    static {
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected final Path path;
    private final Path parent;
    private Thread thread;
    private AtomicBoolean flag;

    /**
     * Create a new Watcher for a single file.
     *
     * @param path path to watched file
     */
    public Watcher(Path path)
    {
        this.path = path;
        parent = path.getParent();
    }

    /**
     * Invoke if and only if no more instances of {@code Watcher} will be used.
     *
     * @throws IOException if there was a problem closing the underlying {@link WatchService}
     */
    public static void close()
    throws IOException
    {
        watcher.close();
    }

    /**
     * Run the file watcher on a separate thread.
     *
     * @throws IOException if there was a problem registering the file with the {@link WatchService}
     */
    public final void start()
    throws IOException
    {
        stop();
        (thread = new Thread(new WatcherListener(flag = new AtomicBoolean(true),
                parent.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY)))).start();
    }

    /**
     * Stop watching the current file and try to stop the watcher thread.
     */
    public final void stop()
    {
        if (thread != null && flag != null && flag.getAndSet(false)) {
            synchronized (this) {
                thread.interrupt();
            }
        }
    }

    /**
     * Invoked on the file watcher thread when the file is modified.
     */
    protected abstract void modified();

    private class WatcherListener
            implements Runnable
    {
        private final AtomicBoolean flag;
        private final WatchKey targetKey;

        private WatcherListener(AtomicBoolean flag, WatchKey targetKey)
        {
            this.flag = flag;
            this.targetKey = targetKey;
        }

        @Override
        public void run()
        {
            logger.debug("Starting watcher for {}", path);
            while (flag.get()) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException e) {
                    break;
                }
                synchronized (Watcher.this) {
                    if (targetKey.equals(key)) {
                        for (WatchEvent<?> e : key.pollEvents()) {
                            if (parent.resolve((Path) e.context()).equals(path)) {
                                logger.debug("Modified");
                                modified();
                            }
                        }
                        if (!key.reset()) {
                            break;
                        }
                    }
                }
            }
            logger.debug("Ending watcher thread");
            flag.set(false);
            targetKey.cancel();
        }
    }
}
