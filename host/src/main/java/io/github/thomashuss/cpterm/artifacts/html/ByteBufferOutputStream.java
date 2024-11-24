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

package io.github.thomashuss.cpterm.artifacts.html;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Convenience class for wrapping the backing byte array of a {@link ByteArrayOutputStream} into a {@link ByteBuffer}
 * without copying.  Useful for the {@link java.util.Base64.Encoder}, which accepts a {@code ByteBuffer}.
 */
public class ByteBufferOutputStream
        extends ByteArrayOutputStream
{
    public ByteBuffer toByteBuffer()
    {
        return ByteBuffer.wrap(buf, 0, count);
    }
}
