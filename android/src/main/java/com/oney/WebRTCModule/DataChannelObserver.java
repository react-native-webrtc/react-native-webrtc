package com.oney.WebRTCModule;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.oney.WebRTCModule.transcoding.DataType;
import com.oney.WebRTCModule.transcoding.InboundDecoder;

import org.webrtc.DataChannel;

import java.nio.ByteBuffer;

import static com.oney.WebRTCModule.WebRTCModule.TAG;

class DataChannelObserver implements DataChannel.Observer {

    private final int mId;
    private final DataChannel mDataChannel;
    private final int peerConnectionId;
    private final WebRTCModule webRTCModule;

    DataChannelObserver(WebRTCModule webRTCModule, int peerConnectionId, int id, DataChannel dataChannel) {
        this.peerConnectionId = peerConnectionId;
        mId = id;
        mDataChannel = dataChannel;
        this.webRTCModule = webRTCModule;
    }

    @Override
    public void onBufferedAmountChange(long amount) {
    }

    @Override
    public void onStateChange() {
        WritableMap params = Arguments.createMap();
        String state = mDataChannel.state() == null ? null : mDataChannel.state().toString().toLowerCase();

        Log.d(TAG,"DataChanel " + mId + " state change to " + state);

        params.putInt("id", mId);
        params.putInt("peerConnectionId", peerConnectionId);
        params.putString("state", state);
        webRTCModule.sendEvent("dataChannelStateChanged", params);
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer) {

        byte[] bytes = extractBytes(buffer.data);

        InboundDecoder decoder = webRTCModule.getTranscodersFactory().getDecoder(buffer.binary ? DataType.BINARY : DataType.TEXT);
        InboundDecoder.Decoded decoded = decoder.decode(bytes);

        WritableMap params = Arguments.createMap();
        params.putInt("id", mId);
        params.putInt("peerConnectionId", peerConnectionId);
        params.putString("type", decoded.getDataType().toString().toLowerCase());
        params.putString("data", decoded.getData());

        webRTCModule.sendEvent("dataChannelReceiveMessage", params);
    }

    private static byte[] extractBytes(ByteBuffer buffer) {
        byte[] bytes;
        if (buffer.hasArray()) {
            bytes = buffer.array();
        } else {
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
        }
        return bytes;
    }
}
