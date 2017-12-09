package com.oney.WebRTCModule.transcoding;

/**
 * Decodes a raw byte content coming from the Data Channel into {@link String}.
 * Implement this interface if you want to provide custom inbound messages decoding method.
 *
 * @author aleksanderlech
 * @see TranscodersFactory#setupDecoder(DataType, InboundDecoder)
 */
public interface InboundDecoder {

    class Decoded {
        DataType dataType;
        String data;

        public Decoded(DataType dataType, String data) {
            this.dataType = dataType;
            this.data = data;
        }

        public DataType getDataType() {
            return dataType;
        }

        public String getData() {
            return data;
        }
    }


    /**
     * Decodes given bytes array into {@link String}.
     *
     * @param encoded raw inbound channel bytes
     * @return Decoded string content
     */
    Decoded decode(byte[] encoded);

}

