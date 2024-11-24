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

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.slf4j.Slf4jLogger;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.util.XRLog;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Converts HTML to PDF, openly.
 */
public class OpenHtmlToPdfConverter
        implements Converter
{
    private static final Logger logger = LoggerFactory.getLogger(OpenHtmlToPdfConverter.class);
    private static final BatikSVGDrawer drawer = new BatikSVGDrawer();

    static {
        XRLog.setLoggerImpl(new Slf4jLogger());
    }

    OpenHtmlToPdfConverter()
    {
    }

    @Override
    public void convert(String outerHtml, String baseUri, Path outputFile)
    throws ConversionException
    {
        Document jdoc = ConversionUtils.clean(Jsoup.parse(outerHtml));

        org.w3c.dom.Document doc = W3CDom.convert(jdoc);
        try (FileOutputStream fos = new FileOutputStream(outputFile.toFile());
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            new PdfRendererBuilder()
                    .withW3cDocument(doc, baseUri)
                    .toStream(bos)
                    .useSVGDrawer(drawer)
                    .run();
        } catch (IOException e) {
            logger.error("Open HTML to PDF failed", e);
            throw new ConversionException(e);
        }
    }
}
