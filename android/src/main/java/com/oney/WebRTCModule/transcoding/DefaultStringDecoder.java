package com.oney.WebRTCModule.transcoding;

import java.nio.charset.Charset;

/**
 * Default UTF-8 string decoder.
 *
 * @author aleksanderlech
 */
class DefaultStringDecoder implements InboundDecoder {

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    @Override
    public Decoded decode(byte[] encoded) {
        return new Decoded(DataType.TEXT, new String(encoded, DEFAULT_CHARSET));
    }
}
