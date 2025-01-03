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

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.util.SVGConstants;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods shared by several convertors.
 */
class ConversionUtils
{
    private static final Logger logger = LoggerFactory.getLogger(ConversionUtils.class);
    private static final Cleaner cleaner = new Cleaner(Safelist.relaxed()
            .addTags("svg")
            .addAttributes(":all", "style")
            .addAttributes("svg", "width", "height", "viewBox", "xmlns")
            .addEnforcedAttribute("svg", "xmlns", SVGConstants.SVG_NAMESPACE_URI)
            .addProtocols("img", "src", "data"));
    private static final Base64.Encoder b64 = Base64.getEncoder();
    private static final PNGTranscoderDimensions pngTranscoder = new PNGTranscoderDimensions();
    private static final int PNG_SCALAR = 4;
    private static final Pattern FLOAT_P = Pattern.compile("[0-9.]+");

    /**
     * Clean the {@link Document} using a Jsoup {@link Cleaner}, but leave SVG elements unchanged.
     *
     * @param d document to clean
     * @return cleaned document
     */
    static Document clean(Document d)
    {
        Elements svgs = d.getElementsByTag("svg");
        if (!svgs.isEmpty()) {
            ArrayList<Elements> svgChildren = new ArrayList<>(svgs.size());
            for (Element svg : svgs) {
                Elements c = svg.children();
                if (svg.hasAttr("width") && svg.hasAttr("height") && !c.isEmpty()) {
                    svgChildren.add(c);
                } else {
                    svg.remove();
                }
            }
            d = cleaner.clean(d);
            svgs = d.getElementsByTag("svg");
            int i = 0;
            for (Element svg : svgs) {
                svg.appendChildren(svgChildren.get(i++));
            }
        } else {
            d = cleaner.clean(d);
        }
        return d;
    }

    /**
     * Render all SVG drawings in {@code root} to PNG, replacing the SVG elements with IMG elements pointing to
     * a base64-encoded PNG.
     *
     * @param root  parent of the SVG elements
     * @param scale should PNGs be scaled to reduce risk of blur, and should width and height attributes
     *              be added to the new {@code img} tag to explicitly show it at original size?
     */
    static void renderSvgElements(Element root, boolean scale)
    {
        Elements svgs = root.getElementsByTag("svg");
        if (!svgs.isEmpty()) {
            Matcher m = scale ? FLOAT_P.matcher("") : null;
            ByteBufferOutputStream os = new ByteBufferOutputStream();
            TranscoderInput transcodeIn = new TranscoderInput();
            TranscoderOutput transcodeOut = new TranscoderOutput(os);
            for (Element el : svgs) {
                if (el.hasAttr("width") && el.hasAttr("height")) {
                    Element parent = el.parent();
                    if (parent != null && parent.tagName().equals("span")) {
                        parent.removeAttr("style");
                    }

                    String width = el.attr("width");
                    String height = el.attr("height");
                    boolean scaled = scale && !width.endsWith("%") && !width.endsWith("vw") && !width.endsWith("vh")
                            && !width.endsWith("vmin") && !width.endsWith("vmax")
                            && !height.endsWith("%") && !height.endsWith("vw") && !height.endsWith("vh")
                            && !height.endsWith("vmin") && !height.endsWith("vmax");
                    if (scaled) {
                        // SVG dimensions are in a pixel-based unit, so we should be able to
                        // multiply that value by a scalar prior to rendering, and divide by
                        // pixels of the rendered image to display a scaled image at its
                        // preferred size.
                        m.reset(width);
                        if (m.find()) {
                            el.attr("width", m.replaceFirst(String.valueOf(Float.parseFloat(m.group()) * PNG_SCALAR)));
                        }
                        m.reset(height);
                        if (m.find()) {
                            el.attr("height", m.replaceFirst(String.valueOf(Float.parseFloat(m.group()) * PNG_SCALAR)));
                        }
                    }

                    transcodeIn.setReader(new StringReader(el.outerHtml()));
                    try {
                        pngTranscoder.transcode(transcodeIn, transcodeOut);
                    } catch (TranscoderException e) {
                        logger.error("Could not render SVG to PNG", e);
                        continue;
                    }

                    Element replacement = new Element("img")
                            .attr("alt", "Converted image")
                            .attr("src", "data:image/png;base64,"
                                    + new String(b64.encode(os.toByteBuffer()).array(), StandardCharsets.ISO_8859_1));
                    os.reset();
                    if (scaled) {
                        replacement.attr("width", (pngTranscoder.getWidth() / PNG_SCALAR) + "px")
                                .attr("height", (pngTranscoder.getHeight() / PNG_SCALAR) + "px");
                    }
                    el.replaceWith(replacement);
                } else {
                    el.remove();
                }
            }
        }
    }
}
