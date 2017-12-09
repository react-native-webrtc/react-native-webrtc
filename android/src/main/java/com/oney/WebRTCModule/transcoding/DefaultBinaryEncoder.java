package com.oney.WebRTCModule.transcoding;

import android.util.Base64;

import org.webrtc.DataChannel;

import java.nio.ByteBuffer;

/**
 * Default Base64 encoder.
 *
 * @author aleksanderlech
 */
class DefaultBinaryEncoder implements OutboundEncoder {

    @Override
    public DataChannel.Buffer encode(String value) {
        byte[] encoded = Base64.decode(value, Base64.NO_WRAP);

        return new DataChannel.Buffer(ByteBuffer.wrap(encoded), true);
    }
}
