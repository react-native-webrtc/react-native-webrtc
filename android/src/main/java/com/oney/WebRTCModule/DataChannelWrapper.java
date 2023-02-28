package com.oney.WebRTCModule;

import android.util.Base64;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import org.webrtc.DataChannel;

import java.nio.charset.StandardCharsets;

class DataChannelWrapper implements DataChannel.Observer {
    private final String reactTag;
    private final DataChannel mDataChannel;
    private final int peerConnectionId;
    private final WebRTCModule webRTCModule;

    DataChannelWrapper(WebRTCModule webRTCModule, int peerConnectionId, String reactTag, DataChannel dataChannel) {
        this.webRTCModule = webRTCModule;
        this.peerConnectionId = peerConnectionId;
        this.reactTag = reactTag;
        mDataChannel = dataChannel;
    }

    public DataChannel getDataChannel() {
        return mDataChannel;
    }

    public String getReactTag() {
        return reactTag;
    }

    @Nullable
    public String dataChannelStateString(DataChannel.State dataChannelState) {
        switch (dataChannelState) {
            case CONNECTING:
                return "connecting";
            case OPEN:
                return "open";
            case CLOSING:
                return "closing";
            case CLOSED:
                return "closed";
        }
        return null;
    }

    @Override
    public void onBufferedAmountChange(long amount) {
        WritableMap params = Arguments.createMap();
        params.putString("reactTag", reactTag);
        params.putInt("peerConnectionId", peerConnectionId);
        params.putDouble("bufferedAmount", Long.valueOf(amount).doubleValue());

        webRTCModule.sendEvent("dataChannelDidChangeBufferedAmount", params);
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer) {
        WritableMap params = Arguments.createMap();
        params.putString("reactTag", reactTag);
        params.putInt("peerConnectionId", peerConnectionId);

        byte[] bytes;
        if (buffer.data.hasArray()) {
            bytes = buffer.data.array();
        } else {
            bytes = new byte[buffer.data.remaining()];
            buffer.data.get(bytes);
        }

        String type;
        String data;
        if (buffer.binary) {
            type = "binary";
            data = Base64.encodeToString(bytes, Base64.NO_WRAP);
        } else {
            type = "text";
            data = new String(bytes, StandardCharsets.UTF_8);
        }
        params.putString("type", type);
        params.putString("data", data);

        webRTCModule.sendEvent("dataChannelReceiveMessage", params);
    }

    @Override
    public void onStateChange() {
        WritableMap params = Arguments.createMap();
        params.putString("reactTag", reactTag);
        params.putInt("peerConnectionId", peerConnectionId);
        params.putInt("id", mDataChannel.id());
        params.putString("state", dataChannelStateString(mDataChannel.state()));

        webRTCModule.sendEvent("dataChannelStateChanged", params);
    }
}
