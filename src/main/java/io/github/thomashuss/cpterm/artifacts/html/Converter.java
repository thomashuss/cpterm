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

import java.nio.file.Path;

/**
 * Converts HTML to some document format.
 */
public interface Converter
{
    OpenHtmlToPdfConverter OPEN_HTML_TO_PDF = new OpenHtmlToPdfConverter();
    PandocConverter PANDOC = new PandocConverter();
    LibreofficeConverter LIBREOFFICE = new LibreofficeConverter();
    RawHtmlConverter RAW_HTML = new RawHtmlConverter();

    /**
     * Convert the document given by {@code outerHtml} to a file at {@code outputFile}.  The resulting file
     * type is not defined generally; it may be dependent on the {@code outputFile}
     * or the particular {@link Converter} implementation.
     *
     * @param outerHtml  HTML code of the document to convert
     * @param baseUri    URI the {@code outerHtml} was pulled from
     * @param outputFile file to write converted document to
     * @throws ConversionException on any blocking error; all relevant information is logged
     */
    void convert(String outerHtml, String baseUri, Path outputFile)
    throws ConversionException;
}
