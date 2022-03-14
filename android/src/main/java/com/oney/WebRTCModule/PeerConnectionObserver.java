package com.oney.WebRTCModule;

import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoTrack;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class PeerConnectionObserver implements PeerConnection.Observer {
    private final static String TAG = WebRTCModule.TAG;

    private final Map<String, DataChannelWrapper> dataChannels;
    private final int id;
    private PeerConnection peerConnection;
    final List<MediaStream> localStreams;
    final Map<String, MediaStream> remoteStreams;
    final Map<String, MediaStreamTrack> remoteTracks;
    private final VideoTrackAdapter videoTrackAdapters;
    private final WebRTCModule webRTCModule;

    PeerConnectionObserver(WebRTCModule webRTCModule, int id) {
        this.webRTCModule = webRTCModule;
        this.id = id;
        this.dataChannels = new HashMap<>();
        this.localStreams = new ArrayList<>();
        this.remoteStreams = new HashMap<>();
        this.remoteTracks = new HashMap<>();
        this.videoTrackAdapters = new VideoTrackAdapter(webRTCModule, id);
    }

    /**
     * Adds a specific local <tt>MediaStream</tt> to the associated
     * <tt>PeerConnection</tt>.
     *
     * @param localStream the local <tt>MediaStream</tt> to add to the
     *                    associated <tt>PeerConnection</tt>
     * @return <tt>true</tt> if the specified <tt>localStream</tt> was added to
     * the associated <tt>PeerConnection</tt>; otherwise, <tt>false</tt>
     */
    boolean addStream(MediaStream localStream) {
        if (peerConnection != null && peerConnection.addStream(localStream)) {
            localStreams.add(localStream);

            return true;
        }

        return false;
    }

    /**
     * Removes a specific local <tt>MediaStream</tt> from the associated
     * <tt>PeerConnection</tt>.
     *
     * @param localStream the local <tt>MediaStream</tt> from the associated
     *                    <tt>PeerConnection</tt>
     * @return <tt>true</tt> if removing the specified <tt>mediaStream</tt> from
     * this instance resulted in a modification of its internal list of local
     * <tt>MediaStream</tt>s; otherwise, <tt>false</tt>
     */
    boolean removeStream(MediaStream localStream) {
        if (peerConnection != null) {
            peerConnection.removeStream(localStream);
        }

        return localStreams.remove(localStream);
    }

    PeerConnection getPeerConnection() {
        return peerConnection;
    }

    void setPeerConnection(PeerConnection peerConnection) {
        this.peerConnection = peerConnection;
    }

    void close() {
        Log.d(TAG, "PeerConnection.close() for " + id);

        // Close the PeerConnection first to stop any events.
        peerConnection.close();

        // PeerConnection.dispose() calls MediaStream.dispose() on all local
        // MediaStreams added to it and the app may crash if a local MediaStream
        // is added to multiple PeerConnections. In order to reduce the risks of
        // an app crash, remove all local MediaStreams from the associated
        // PeerConnection so that it doesn't attempt to dispose of them.
        for (MediaStream localStream : new ArrayList<>(localStreams)) {
            removeStream(localStream);
        }

        // Remove video track adapters
        for (MediaStream stream : remoteStreams.values()) {
            for (VideoTrack videoTrack : stream.videoTracks) {
                videoTrackAdapters.removeAdapter(videoTrack);
            }
        }

        // Remove DataChannel observers
        for (DataChannelWrapper dcw : dataChannels.values()) {
            DataChannel dataChannel = dcw.getDataChannel();
            dataChannel.close();
            dataChannel.unregisterObserver();
        }

        // At this point there should be no local MediaStreams in the associated
        // PeerConnection. Call dispose() to free all remaining resources held
        // by the PeerConnection instance (RtpReceivers, RtpSenders, etc.)
        peerConnection.dispose();

        remoteStreams.clear();
        remoteTracks.clear();
        dataChannels.clear();
    }

    WritableMap createDataChannel(String label, ReadableMap config) {
        DataChannel.Init init = new DataChannel.Init();
        if (config != null) {
            if (config.hasKey("id")) {
                init.id = config.getInt("id");
            }
            if (config.hasKey("ordered")) {
                init.ordered = config.getBoolean("ordered");
            }
            if (config.hasKey("maxRetransmitTime")) {
                init.maxRetransmitTimeMs = config.getInt("maxRetransmitTime");
            }
            if (config.hasKey("maxRetransmits")) {
                init.maxRetransmits = config.getInt("maxRetransmits");
            }
            if (config.hasKey("protocol")) {
                init.protocol = config.getString("protocol");
            }
            if (config.hasKey("negotiated")) {
                init.negotiated = config.getBoolean("negotiated");
            }
        }
        DataChannel dataChannel = peerConnection.createDataChannel(label, init);
        if (dataChannel == null) {
            return null;
        }
        final String reactTag  = UUID.randomUUID().toString();
        DataChannelWrapper dcw = new DataChannelWrapper(webRTCModule, id, reactTag, dataChannel);
        dataChannels.put(reactTag, dcw);
        dataChannel.registerObserver(dcw);

        WritableMap info = Arguments.createMap();
        info.putInt("peerConnectionId", id);
        info.putString("reactTag", reactTag);
        info.putString("label", dataChannel.label());
        info.putInt("id", dataChannel.id());
        info.putBoolean("ordered", init.ordered);
        info.putInt("maxPacketLifeTime", init.maxRetransmitTimeMs);
        info.putInt("maxRetransmits", init.maxRetransmits);
        info.putString("protocol", init.protocol);
        info.putBoolean("negotiated", init.negotiated);
        info.putString("readyState", dcw.dataChannelStateString(dataChannel.state()));
        return info;
    }

    void dataChannelClose(String reactTag) {
        DataChannelWrapper dcw = dataChannels.get(reactTag);
        if (dcw == null) {
            Log.d(TAG, "dataChannelClose() dataChannel is null");
            return;
        }

        DataChannel dataChannel = dcw.getDataChannel();
        dataChannel.close();
    }

    void dataChannelDispose(String reactTag) {
        DataChannelWrapper dcw = dataChannels.get(reactTag);
        if (dcw == null) {
            Log.d(TAG, "dataChannelDispose() dataChannel is null");
            return;
        }

        DataChannel dataChannel = dcw.getDataChannel();
        dataChannel.unregisterObserver();
        dataChannels.remove(reactTag);
    }

    void dataChannelSend(String reactTag, String data, String type) {
        DataChannelWrapper dcw = dataChannels.get(reactTag);
        if (dcw == null) {
            Log.d(TAG, "dataChannelSend() dataChannel is null");
            return;
        }

        byte[] byteArray;
        if (type.equals("text")) {
            byteArray = data.getBytes(StandardCharsets.UTF_8);
        } else if (type.equals("binary")) {
            byteArray = Base64.decode(data, Base64.NO_WRAP);
        } else {
            Log.e(TAG, "Unsupported data type: " + type);
            return;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
        DataChannel.Buffer buffer = new DataChannel.Buffer(byteBuffer, type.equals("binary"));
        dcw.getDataChannel().send(buffer);
    }

    void getStats(Promise promise) {
        peerConnection.getStats(rtcStatsReport -> {
            promise.resolve(StringUtils.statsToJSON(rtcStatsReport));
        });
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        Log.d(TAG, "onIceCandidate");
        WritableMap params = Arguments.createMap();
        params.putInt("id", id);
        WritableMap candidateParams = Arguments.createMap();
        candidateParams.putInt("sdpMLineIndex", candidate.sdpMLineIndex);
        candidateParams.putString("sdpMid", candidate.sdpMid);
        candidateParams.putString("candidate", candidate.sdp);
        params.putMap("candidate", candidateParams);
        SessionDescription newSdp = peerConnection.getLocalDescription();
        WritableMap newSdpMap = Arguments.createMap();
        newSdpMap.putString("type", newSdp.type.canonicalForm());
        newSdpMap.putString("sdp", newSdp.description);
        params.putMap("sdp", newSdpMap);

        webRTCModule.sendEvent("peerConnectionGotICECandidate", params);
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
        Log.d(TAG, "onIceCandidatesRemoved");
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        WritableMap params = Arguments.createMap();
        params.putInt("id", id);
        params.putString("iceConnectionState", iceConnectionStateString(iceConnectionState));
        webRTCModule.sendEvent("peerConnectionIceConnectionChanged", params);
    }

    @Override
    public void onConnectionChange(PeerConnection.PeerConnectionState peerConnectionState) {
        WritableMap params = Arguments.createMap();
        params.putInt("id", id);
        params.putString("connectionState", peerConnectionStateString(peerConnectionState));

        webRTCModule.sendEvent("peerConnectionStateChanged", params);
    }

    @Override
    public void onIceConnectionReceivingChange(boolean var1) {
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.d(TAG, "onIceGatheringChange" + iceGatheringState.name());
        WritableMap params = Arguments.createMap();
        params.putInt("id", id);
        params.putString("iceGatheringState", iceGatheringStateString(iceGatheringState));
        if (iceGatheringState == PeerConnection.IceGatheringState.COMPLETE) {
            SessionDescription newSdp = peerConnection.getLocalDescription();
            WritableMap newSdpMap = Arguments.createMap();
            newSdpMap.putString("type", newSdp.type.canonicalForm());
            newSdpMap.putString("sdp", newSdp.description);
            params.putMap("sdp", newSdpMap);
        }
        webRTCModule.sendEvent("peerConnectionIceGatheringChanged", params);
    }

    private String getReactTagForStream(MediaStream mediaStream) {
        for (Iterator<Map.Entry<String, MediaStream>> i
             = remoteStreams.entrySet().iterator();
             i.hasNext(); ) {
            Map.Entry<String, MediaStream> e = i.next();
            if (e.getValue().equals(mediaStream)) {
                return e.getKey();
            }
        }
        return null;
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        String streamReactTag = null;
        String streamId = mediaStream.getId();
        // The native WebRTC implementation has a special concept of a default
        // MediaStream instance with the label default that the implementation
        // reuses.
        if ("default".equals(streamId)) {
            for (Map.Entry<String, MediaStream> e : remoteStreams.entrySet()) {
                if (e.getValue().equals(mediaStream)) {
                    streamReactTag = e.getKey();
                    break;
                }
            }
        }

        if (streamReactTag == null) {
            streamReactTag = UUID.randomUUID().toString();
            remoteStreams.put(streamReactTag, mediaStream);
        }

        WritableMap params = Arguments.createMap();
        params.putInt("id", id);
        params.putString("streamId", streamId);
        params.putString("streamReactTag", streamReactTag);

        WritableArray tracks = Arguments.createArray();

        for (int i = 0; i < mediaStream.videoTracks.size(); i++) {
            VideoTrack track = mediaStream.videoTracks.get(i);
            String trackId = track.id();

            remoteTracks.put(trackId, track);

            WritableMap trackInfo = Arguments.createMap();
            trackInfo.putString("id", trackId);
            trackInfo.putString("label", "Video");
            trackInfo.putString("kind", track.kind());
            trackInfo.putBoolean("enabled", track.enabled());
            trackInfo.putString("readyState", track.state().toString());
            trackInfo.putBoolean("remote", true);
            tracks.pushMap(trackInfo);

            videoTrackAdapters.addAdapter(track);
        }
        for (int i = 0; i < mediaStream.audioTracks.size(); i++) {
            AudioTrack track = mediaStream.audioTracks.get(i);
            String trackId = track.id();

            remoteTracks.put(trackId, track);

            WritableMap trackInfo = Arguments.createMap();
            trackInfo.putString("id", trackId);
            trackInfo.putString("label", "Audio");
            trackInfo.putString("kind", track.kind());
            trackInfo.putBoolean("enabled", track.enabled());
            trackInfo.putString("readyState", track.state().toString());
            trackInfo.putBoolean("remote", true);
            tracks.pushMap(trackInfo);
        }
        params.putArray("tracks", tracks);

        SessionDescription newSdp = peerConnection.getRemoteDescription();
        WritableMap newSdpMap = Arguments.createMap();
        newSdpMap.putString("type", newSdp.type.canonicalForm());
        newSdpMap.putString("sdp", newSdp.description);
        params.putMap("sdp", newSdpMap);

        webRTCModule.sendEvent("peerConnectionAddedStream", params);
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        String streamReactTag = getReactTagForStream(mediaStream);
        if (streamReactTag == null) {
            Log.w(TAG, "onRemoveStream - no remote stream for id: " + mediaStream.getId());
            return;
        }

        for (VideoTrack track : mediaStream.videoTracks) {
            this.videoTrackAdapters.removeAdapter(track);
            this.remoteTracks.remove(track.id());
        }
        for (AudioTrack track : mediaStream.audioTracks) {
            this.remoteTracks.remove(track.id());
        }

        this.remoteStreams.remove(streamReactTag);

        WritableMap params = Arguments.createMap();
        params.putInt("id", id);
        params.putString("streamId", streamReactTag);

        SessionDescription newSdp = peerConnection.getRemoteDescription();
        WritableMap newSdpMap = Arguments.createMap();
        newSdpMap.putString("type", newSdp.type.canonicalForm());
        newSdpMap.putString("sdp", newSdp.description);
        params.putMap("sdp", newSdpMap);

        webRTCModule.sendEvent("peerConnectionRemovedStream", params);
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        final String reactTag  = UUID.randomUUID().toString();
        DataChannelWrapper dcw = new DataChannelWrapper(webRTCModule, id, reactTag, dataChannel);
        dataChannels.put(reactTag, dcw);
        dataChannel.registerObserver(dcw);

        WritableMap info = Arguments.createMap();
        info.putInt("peerConnectionId", id);
        info.putString("reactTag", reactTag);
        info.putString("label", dataChannel.label());
        info.putInt("id", dataChannel.id());

        // TODO: These values are not gettable from a DataChannel instance.
        info.putBoolean("ordered", true);
        info.putInt("maxPacketLifeTime", -1);
        info.putInt("maxRetransmits", -1);
        info.putString("protocol", "");

        info.putBoolean("negotiated", false);
        info.putString("readyState", dcw.dataChannelStateString(dataChannel.state()));

        WritableMap params = Arguments.createMap();
        params.putInt("id", id);
        params.putMap("dataChannel", info);

        webRTCModule.sendEvent("peerConnectionDidOpenDataChannel", params);
    }

    @Override
    public void onRenegotiationNeeded() {
        WritableMap params = Arguments.createMap();
        params.putInt("id", id);
        webRTCModule.sendEvent("peerConnectionOnRenegotiationNeeded", params);
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        WritableMap params = Arguments.createMap();
        params.putInt("id", id);
        params.putString("signalingState", signalingStateString(signalingState));
        webRTCModule.sendEvent("peerConnectionSignalingStateChanged", params);
    }

    @Override
    public void onAddTrack(final RtpReceiver receiver, final MediaStream[] mediaStreams) {
        Log.d(TAG, "onAddTrack");
    }

    @Nullable
    private String peerConnectionStateString(PeerConnection.PeerConnectionState peerConnectionState) {
        switch (peerConnectionState) {
            case NEW:
                return "new";
            case CONNECTING:
                return "connecting";
            case CONNECTED:
                return "connected";
            case DISCONNECTED:
                return "disconnected";
            case FAILED:
                return "failed";
            case CLOSED:
                return "closed";
        }
        return null;
    }

    @Nullable
    private String iceConnectionStateString(PeerConnection.IceConnectionState iceConnectionState) {
        switch (iceConnectionState) {
            case NEW:
                return "new";
            case CHECKING:
                return "checking";
            case CONNECTED:
                return "connected";
            case COMPLETED:
                return "completed";
            case FAILED:
                return "failed";
            case DISCONNECTED:
                return "disconnected";
            case CLOSED:
                return "closed";
        }
        return null;
    }

    @Nullable
    private String iceGatheringStateString(PeerConnection.IceGatheringState iceGatheringState) {
        switch (iceGatheringState) {
            case NEW:
                return "new";
            case GATHERING:
                return "gathering";
            case COMPLETE:
                return "complete";
        }
        return null;
    }

    @Nullable
    private String signalingStateString(PeerConnection.SignalingState signalingState) {
        switch (signalingState) {
            case STABLE:
                return "stable";
            case HAVE_LOCAL_OFFER:
                return "have-local-offer";
            case HAVE_LOCAL_PRANSWER:
                return "have-local-pranswer";
            case HAVE_REMOTE_OFFER:
                return "have-remote-offer";
            case HAVE_REMOTE_PRANSWER:
                return "have-remote-pranswer";
            case CLOSED:
                return "closed";
        }
        return null;
    }
}
