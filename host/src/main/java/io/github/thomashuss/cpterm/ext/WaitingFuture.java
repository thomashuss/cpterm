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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link Future} which is not waiting on an ephemeral thread and is completed when provided some object.
 *
 * @param <V> type returned by {@link WaitingFuture#get()}
 */
public abstract class WaitingFuture<V>
        implements Future<V>
{
    private final CountDownLatch latch;
    private volatile V result;
    private volatile Exception e;

    public WaitingFuture()
    {
        latch = new CountDownLatch(1);
    }

    /**
     * Pass {@code o} to {@link WaitingFuture#offered(Object)}, and if it returns
     * non-{@code null}, complete the future.
     *
     * @param o object which might complete the future
     * @return {@code true} if {@code o} completed the future
     */
    public boolean offer(Object o)
    {
        if (result == null) {
            try {
                V v = offered(o);
                if (v != null) {
                    result = v;
                    latch.countDown();
                    return true;
                }
            } catch (Exception e) {
                this.e = e;
                latch.countDown();
            }
        }
        return false;
    }

    /**
     * Compute a {@code V} from {@code o} if possible; else return {@code null}.
     *
     * @param o object offered by {@link WaitingFuture#offer(Object)}
     * @return new {@code V} if {@code o} completes this future, {@code null} otherwise
     */
    protected abstract V offered(Object o);

    @Override
    public boolean cancel(boolean b)
    {
        return false;
    }

    @Override
    public boolean isCancelled()
    {
        return false;
    }

    @Override
    public boolean isDone()
    {
        return result != null || e != null;
    }

    @Override
    public V get()
    throws InterruptedException, ExecutionException
    {
        latch.await();
        if (e != null) {
            throw new ExecutionException(e);
        }
        return result;
    }

    @Override
    public V get(long l, TimeUnit timeUnit)
    throws InterruptedException, ExecutionException, TimeoutException
    {
        if (latch.await(l, timeUnit)) {
            if (e != null) {
                throw new ExecutionException(e);
            }
            return result;
        }
        throw new TimeoutException();
    }
}
