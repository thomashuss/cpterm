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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A converter which delegates to Pandoc.
 */
public class PandocConverter
        extends PipedExternalConverter
{
    private static final Map<String, String> REPLACE = new HashMap<>(2);
    private static final Pattern REPLACE_PATTERN;

    static {
        REPLACE.put("≤", "$\\leq$");
        REPLACE.put("≥", "$\\geq$");
        REPLACE_PATTERN = Pattern.compile(
                REPLACE.keySet().stream().reduce("(", (r, s) -> r.length() == 1 ? r + s : r + "|" + s) + ")");
    }

    PandocConverter()
    {
    }

    /**
     * Create an HTML string which is useful as an input to Pandoc.
     *
     * @param outerHtml HTML pulled from the website
     * @param baseUri   URI the HTML was pulled from
     * @return Pandoc-friendly HTML
     */
    private static String prepareForPandoc(String outerHtml, String baseUri)
    {
        Document doc = ConversionUtils.clean(Jsoup.parse(Objects.requireNonNull(outerHtml), baseUri));
        ConversionUtils.renderSvgElements(doc);
        return replaceWithTex(doc.outerHtml());
    }

    /**
     * Replace some raw characters in HTML with their TeX equivalents.  If the {@code tex_math_dollars}
     * parameter is set on the {@code html} Pandoc filter, this TeX code will be passed directly to TeX.
     *
     * @param html HTML code to search
     * @return HTML code with characters replaced.
     */
    private static String replaceWithTex(String html)
    {
        Matcher matcher = REPLACE_PATTERN.matcher(html);
        if (matcher.find()) {
            StringBuilder buf = new StringBuilder(html.length() + 16);
            int i = 0;
            int pos;
            do {
                pos = matcher.start();
                while (i < pos) {
                    buf.appendCodePoint(html.codePointAt(i++));
                }
                i = matcher.end();
                buf.append(REPLACE.get(matcher.group()));
            } while (matcher.find());
            while (i < html.length()) {
                buf.appendCodePoint(html.codePointAt(i++));
            }
            return buf.toString();
        }
        return html;
    }

    @Override
    protected ProcessBuilder getProcess(Path output)
    {
        String[] cmd = new String[5 + args.size()];
        int i = 1;
        cmd[0] = exePath;
        if (!args.isEmpty()) {
            for (String p : args) {
                cmd[i++] = p;
            }
        }
        cmd[i] = "-f";
        cmd[i + 1] = "html+tex_math_dollars";
        cmd[i + 2] = "-o";
        cmd[i + 3] = output.toString();
        return new ProcessBuilder(cmd);
    }

    @Override
    protected void doConvert(String outerHtml, String baseUri, Path outputFile)
    throws ConversionException
    {
        writeToProcess(prepareForPandoc(outerHtml, baseUri), outputFile);
    }
}
