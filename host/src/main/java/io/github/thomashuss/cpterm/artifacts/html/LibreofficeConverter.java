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

import java.nio.file.Path;
import java.util.Objects;

/**
 * A convertor which delegates to the LibreOffice Writer command-line API.
 */
public class LibreofficeConverter
        extends TempFileExternalConverter
{
    LibreofficeConverter()
    {
    }

    /**
     * Create an HTML string which is useful as an input to LibreOffice.
     *
     * @param html    HTML pulled from the website
     * @param baseUri URI the HTML was pulled from
     * @return LibreOffice-friendly HTML
     */
    private static String prepareForLibreoffice(String html, String baseUri)
    {
        Document doc = ConversionUtils.clean(Jsoup.parse(Objects.requireNonNull(html), baseUri));
        ConversionUtils.renderSvgElements(doc, true);
        return doc.outerHtml();
    }

    @Override
    protected ProcessBuilder getProcess(Path tempFile, Path outputPath)
    {
        boolean hasConvertTo = args.contains("--convert-to");
        int i = 4;
        String[] cmd = new String[(hasConvertTo ? 7 : 9) + args.size()];

        cmd[0] = exePath;
        cmd[1] = "--headless";
        cmd[2] = "--norestore";
        cmd[3] = "--writer";
        if (!hasConvertTo) {
            String outName = outputPath.toString();
            int idx = outName.lastIndexOf('.') + 1;
            cmd[4] = "--convert-to";
            cmd[5] = idx > 0 && idx < outName.length() ? outName.substring(idx) : "pdf";
            i += 2;
        }
        if (!args.isEmpty()) {
            for (String arg : args) {
                cmd[i++] = arg;
            }
        }
        cmd[i] = "--outdir";
        cmd[i + 1] = outputPath.getParent().toAbsolutePath().toString();
        cmd[i + 2] = tempFile.toAbsolutePath().toString();

        return new ProcessBuilder(cmd);
    }

    @Override
    protected void doConvert(String outerHtml, String baseUri, Path outputFile)
    throws ConversionException
    {
        writeToProcess(prepareForLibreoffice(outerHtml, baseUri), outputFile);
    }
}
