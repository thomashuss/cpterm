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
import org.apache.batik.transcoder.image.JPEGTranscoder;
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
    private static final JPEGTranscoder jpegTranscoder = new JPEGTranscoder();

    static {
        jpegTranscoder.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, 0.95f);
    }

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
     * Render all SVG drawings in {@code root} to JPEG, replacing the SVG elements with IMG elements pointing to
     * a base64-encoded JPEG.
     *
     * @param root parent of the SVG elements
     */
    static void renderSvgElements(Element root)
    {
        for (Element el : root.getElementsByTag("svg")) {
            if (el.hasAttr("width") && el.hasAttr("height")) {
                Element parent = el.parent();
                if (parent != null && parent.tagName().equals("span")) {
                    parent.removeAttr("style");
                }

                ByteBufferOutputStream os = new ByteBufferOutputStream();
                try {
                    jpegTranscoder.transcode(new TranscoderInput(new StringReader(el.outerHtml())),
                            new TranscoderOutput(os));
                } catch (TranscoderException e) {
                    logger.error("Could not render SVG to JPEG", e);
                }

                Element replacement = new Element("img")
                        .attr("alt", "Converted image")
                        .attr("src", "data:image/jpeg;base64,"
                                + new String(b64.encode(os.toByteBuffer()).array(), StandardCharsets.ISO_8859_1));
                if (el.hasAttr("style")) {
                    replacement.attr("style", el.attr("style"));
                }
                el.replaceWith(replacement);
            } else {
                el.remove();
            }
        }
    }
}
