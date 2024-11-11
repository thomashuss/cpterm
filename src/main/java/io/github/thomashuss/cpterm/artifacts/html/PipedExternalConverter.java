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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;

/**
 * An {@link ExternalConverter} which writes to the subprocess's standard input.
 */
public abstract class PipedExternalConverter
        extends ExternalConverter
{
    protected synchronized final void writeToProcess(String html, Path output)
    throws ConversionException
    {
        try {
            Process p = getProcess(output).start();
            try (BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                 BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
                writer.write(html);
                writer.close();
                p.waitFor();
                logError(p.exitValue(), out, err);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Communication with process failed", e);
            throw new ConversionException(e);
        }
    }

    /**
     * Build a process which will output to {@code output} when the input file is written to its standard input.
     *
     * @param output output file
     * @return process to run
     */
    protected abstract ProcessBuilder getProcess(Path output);
}
