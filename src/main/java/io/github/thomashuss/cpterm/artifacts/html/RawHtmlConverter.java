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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RawHtmlConverter
        implements Converter
{
    private static final Logger logger = LoggerFactory.getLogger(RawHtmlConverter.class);
    private boolean renderSvg;

    RawHtmlConverter()
    {
    }

    public void setRenderSvg(boolean renderSvg)
    {
        this.renderSvg = renderSvg;
    }

    @Override
    public void convert(String outerHtml, String baseUri, Path outputFile)
    throws ConversionException
    {
        Document d = ConversionUtils.clean(Jsoup.parse(outerHtml, baseUri));
        if (renderSvg) {
            ConversionUtils.renderSvgElements(d);
        }
        try {
            Files.writeString(outputFile, d.outerHtml());
        } catch (IOException e) {
            logger.error("Unable to write to file", e);
            throw new ConversionException(e);
        }
    }
}
