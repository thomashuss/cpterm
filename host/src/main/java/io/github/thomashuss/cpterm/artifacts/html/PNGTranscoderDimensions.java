package io.github.thomashuss.cpterm.artifacts.html;

import org.apache.batik.transcoder.image.PNGTranscoder;

class PNGTranscoderDimensions
        extends PNGTranscoder
{
    float getWidth()
    {
        return width;
    }

    float getHeight()
    {
        return height;
    }
}
