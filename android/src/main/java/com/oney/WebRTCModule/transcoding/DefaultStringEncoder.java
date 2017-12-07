package com.oney.WebRTCModule.transcoding;

import org.webrtc.DataChannel;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * Default UTF-8 string encoder.
 *
 * @author aleksanderlech
 */
class DefaultStringEncoder implements OutboundEncoder {

    private static final String CHARSET = "UTF-8";

    @Override
    public DataChannel.Buffer encode(String value) {
        try {
            byte[] stringBytes = value.getBytes(CHARSET);
            return new DataChannel.Buffer(ByteBuffer.wrap(stringBytes), false);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Could not encode text string as UTF-8.", e);
        }
    }
}
