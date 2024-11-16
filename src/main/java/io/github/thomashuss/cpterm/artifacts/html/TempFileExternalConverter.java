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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link Converter} which writes the HTML to a temporary file before calling the external converter on that file.
 */
public abstract class TempFileExternalConverter
        extends ExternalConverter
{
    protected synchronized final void writeToProcess(String html, Path output)
    throws ConversionException
    {
        Path temp = output.getParent().resolve(
                output.toString().replaceFirst("\\.[^.]+$", ".html"));
        logger.info("Using temp file {}", temp);
        try {
            Files.writeString(temp, html);
            Process p = getProcess(temp, output).start();
            try (BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                 BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                p.waitFor();
                logError(p.exitValue(), out, err);
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Communication with process failed", e);
            throw new ConversionException(e);
        } finally {
            try {
                Files.delete(temp);
            } catch (IOException e) {
                logger.warn("Temp file deletion failed", e);
            }
        }
    }

    protected abstract ProcessBuilder getProcess(Path tempFile, Path outputPath);
}
