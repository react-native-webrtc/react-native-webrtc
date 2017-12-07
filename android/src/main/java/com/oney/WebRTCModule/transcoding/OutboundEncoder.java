package com.oney.WebRTCModule.transcoding;

import org.webrtc.DataChannel;

/**
 * Encodes outgoing channel messages.
 *
 * @author aleksanderlech
 * @see TranscodersFactory#setupDecoder(DataType, InboundDecoder)
 */
public interface OutboundEncoder {

    /**
     * Encodes the given value into bytes wrapped by the DataChannel.Buffer.
     *
     * @param value a value to be encoded
     * @return DataChannel.Buffer
     */
    DataChannel.Buffer encode(String value);

}

