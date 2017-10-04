package com.oney.WebRTCModule;

import java.nio.charset.Charset;

import android.support.annotation.Nullable;
import android.util.Base64;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import org.webrtc.DataChannel;

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

    @Nullable
    private String dataChannelStateString(DataChannel.State dataChannelState) {
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
    }

    @Override
    public void onStateChange() {
        WritableMap params = Arguments.createMap();
        params.putInt("id", mId);
        params.putInt("peerConnectionId", peerConnectionId);
        params.putString("state", dataChannelStateString(mDataChannel.state()));
        webRTCModule.sendEvent("dataChannelStateChanged", params);
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer) {
        WritableMap params = Arguments.createMap();
        params.putInt("id", mId);
        params.putInt("peerConnectionId", peerConnectionId);

        byte[] bytes;
        if (buffer.data.hasArray()) {
            bytes = buffer.data.array();
        } else {
            bytes = new byte[buffer.data.remaining()];
            buffer.data.get(bytes);
        }

        if (buffer.binary) {
            params.putString("type", "binary");
            params.putString("data", Base64.encodeToString(bytes, Base64.NO_WRAP));
        } else {
            params.putString("type", "text");
            params.putString("data", new String(bytes, Charset.forName("UTF-8")));
        }

        webRTCModule.sendEvent("dataChannelReceiveMessage", params);
    }
}
