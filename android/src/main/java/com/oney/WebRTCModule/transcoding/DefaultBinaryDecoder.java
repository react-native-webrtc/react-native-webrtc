package com.oney.WebRTCModule.transcoding;

import android.util.Base64;

/**
 * Default Base64 decoder.
 *
 * @author aleksanderlech
 */
class DefaultBinaryDecoder implements InboundDecoder {

    @Override
    public Decoded decode(byte[] encoded) {
        return new Decoded(DataType.BINARY, Base64.encodeToString(encoded, Base64.NO_WRAP));
    }
}
