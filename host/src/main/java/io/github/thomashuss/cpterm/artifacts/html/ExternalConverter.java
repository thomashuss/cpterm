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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * A {@link Converter} which delegates to an external process.
 */
public abstract class ExternalConverter
        implements Converter
{
    protected static final Logger logger = LoggerFactory.getLogger(ExternalConverter.class);
    protected final ArrayList<String> args = new ArrayList<>(0);
    protected String exePath;

    protected static void logError(int code, BufferedReader out, BufferedReader err)
    throws ConversionException
    {
        if (code != 0) {
            logger.error("Exit code was {}", code);
            String stdout = out.lines().collect(Collectors.joining("\n"));
            String stderr = err.lines().collect(Collectors.joining("\n"));
            logger.error("stdout:\n{}", stdout);
            logger.error("stderr:\n{}", stderr);
            throw new ConversionException(stdout + '\n' + stderr);
        }
    }

    /**
     * Set the path to the executable.
     *
     * @param exePath path to delegate executable
     */
    public void setExePath(Path exePath)
    {
        if (exePath == null) {
            throw new IllegalArgumentException("Path is null");
        }
        exePath = exePath.toAbsolutePath();
        if (!Files.isExecutable(exePath)) {
            throw new IllegalArgumentException("Path does not point to an executable");
        }
        this.exePath = exePath.toString();
    }

    /**
     * Set additional command line arguments to be used when starting this converter's process.  Tokenization is
     * performed similarly to the Unix shell quoting methodology:
     * <ul>
     *     <li>Arguments are separated by a space.</li>
     *     <li>Text surrounded by {@code '} or {@code "} (single or double quotes) is treated as one argument
     *     regardless of its content; consequently, literal spaces can occur only inside single or double quotes.</li>
     *     <li>Literal single quotes can occur only inside double quotes, and literal double quotes can occur
     *     only inside single quotes.</li>
     *     <li>One argument may use two quoting styles (single and double) by writing the next opening quote
     *     immediately after the preceding closing quote; consequently, it is possible to include a literal
     *     {@code '} and literal {@code "} in the same argument.</li>
     * </ul>
     * Example:
     * <pre>
     *    --title="Bob's "'"website"' --favicon=bob.ico
     * </pre>
     * evaluates to
     * <pre>
     *    --title=Bob's "website"
     * </pre>
     * and
     * <pre>
     *     --favicon=bob.ico
     * </pre>
     *
     * @param argStr argument string
     */
    public synchronized final void setArgs(String argStr)
    {
        if (argStr.isEmpty()) {
            if (!args.isEmpty()) {
                args.clear();
            }
            return;
        }
        boolean sq = false;
        boolean dq = false;
        int strlen = argStr.length();
        int strPos = 0;
        int argCount = 0;
        int ch;
        String word;
        StringBuilder argBuf = new StringBuilder();

        while (strPos < strlen) {
            ch = argStr.codePointAt(strPos++);
            if (!sq && ch == '"') {
                dq = !dq;
            } else if (!dq && ch == '\'') {
                sq = !sq;
            } else if (!dq && !sq && ch == ' ') {
                if (!argBuf.isEmpty()) {
                    word = argBuf.toString();
                    if (argCount < args.size()) {
                        args.set(argCount++, word);
                    } else {
                        args.add(word);
                        argCount++;
                    }
                    argBuf.setLength(0);
                }
            } else {
                argBuf.appendCodePoint(ch);
            }
        }

        if (!argBuf.isEmpty()) {
            word = argBuf.toString();
            if (argCount < args.size()) {
                args.set(argCount++, word);
            } else {
                args.add(word);
                argCount++;
            }
        }

        if (argCount < (strlen = args.size())) {
            int k = strlen - 1;
            while (k >= argCount) {
                args.remove(k--);
            }
        }
    }
}
