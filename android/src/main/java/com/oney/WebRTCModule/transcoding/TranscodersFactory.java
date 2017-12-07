package com.oney.WebRTCModule.transcoding;

import java.util.EnumMap;

/**
 * Provides decoders and encoders for data channels.
 *
 * @author aleksanderlech
 * @see InboundDecoder
 * @see OutboundEncoder
 */
public final class TranscodersFactory {

    private final EnumMap<DataType, OutboundEncoder> encodersMap = new EnumMap<>(DataType.class);
    private final EnumMap<DataType, InboundDecoder> decodersMap = new EnumMap<>(DataType.class);

    public TranscodersFactory() {
        //register defaults

        setupEncoder(DataType.BINARY, new DefaultBinaryEncoder());
        setupEncoder(DataType.TEXT, new DefaultStringEncoder());
        setupDecoder(DataType.BINARY, new DefaultBinaryDecoder());
        setupDecoder(DataType.TEXT, new DefaultStringDecoder());
    }

    public TranscodersFactory setupEncoder(DataType type, OutboundEncoder encoder) {
        encodersMap.put(type, encoder);
        return this;
    }

    public TranscodersFactory setupDecoder(DataType type, InboundDecoder decoder) {
        decodersMap.put(type, decoder);
        return this;
    }

    public OutboundEncoder getEncoder(DataType type) {
        return encodersMap.get(type);
    }

    public InboundDecoder getDecoder(DataType type) {
        return decodersMap.get(type);
    }


}
