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
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
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


    private PeerConnection.SdpSemantics sdpSemantics; 
    
    private final Map<String, DataChannelWrapper> dataChannels;
    private final int id;
    private PeerConnection peerConnection;
    final List<MediaStream> localStreams;
    final Map<String, MediaStream> remoteStreams;
    final Map<String, MediaStreamTrack> remoteTracks;
    private final VideoTrackAdapter videoTrackAdapters;
    private final WebRTCModule webRTCModule;

    PeerConnectionObserver(WebRTCModule webRTCModule, int id, PeerConnection.SdpSemantics semantics) {
        this.webRTCModule = webRTCModule;
        this.sdpSemantics = semantics;
        this.id = id;
        this.dataChannels = new HashMap<>();
        this.localStreams = new ArrayList<>();
        this.remoteStreams = new HashMap<>();
        this.remoteTracks = new HashMap<>();
        this.videoTrackAdapters = new VideoTrackAdapter(webRTCModule, id);
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

    RtpTransceiver addTransceiver(MediaStreamTrack.MediaType mediaType, RtpTransceiver.RtpTransceiverInit init) {
        if (sdpSemantics != PeerConnection.SdpSemantics.UNIFIED_PLAN) {
            throw new Error("Cannot add transceiver: Invalid plan");
        }

        if (peerConnection == null) {
            throw new Error("Cannot Add transceiver: peer connection is null");
        }

        return peerConnection.addTransceiver(mediaType, init);
    }

    RtpTransceiver addTransceiver(MediaStreamTrack track, RtpTransceiver.RtpTransceiverInit init) {
        if (sdpSemantics != PeerConnection.SdpSemantics.UNIFIED_PLAN) {
            throw new Error("Cannot add transceiver: Invalid plan");
        }

        if (peerConnection == null) {
            throw new Error("Cannot Add transceiver: peer connection is null");
        }

        return peerConnection.addTransceiver(track, init);
    }

    String resolveTransceiverId(RtpTransceiver transceiver) {
        return transceiver.getSender().id();
    }

    RtpTransceiver getTransceiver(String id) {
        if (this.peerConnection == null) {
            throw new Error("Peer connection is null");
        }

        for(RtpTransceiver transceiver: this.peerConnection.getTransceivers()) {
            if (transceiver.getSender().id().equals(id)) {
                return transceiver;
            }
        }
        throw new Error("Unable to find transceiver");
    }

    public ReadableMap getTransceiversMap() {
        if (this.peerConnection == null) {
            throw new Error("Peer connection is null");
        }

        List<RtpTransceiver> transceivers = this.peerConnection.getTransceivers();
        WritableMap transceiversMap = Arguments.createMap();
        WritableArray transceiversArray = Arguments.createArray();
        for(RtpTransceiver transceiver: transceivers) {
            transceiversArray.pushMap(serializeTransceiver(transceiver));
        }
        transceiversMap.putArray("transceivers", transceiversArray);
        return transceiversMap;
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

        // Get The timestamp so that we can reorder the transceivers on the JS layer
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();

        MediaStreamTrack track = receiver.track();

        if (track == null || sdpSemantics != PeerConnection.SdpSemantics.UNIFIED_PLAN) {
            return;
        }
        if(track.kind().equals(MediaStreamTrack.VIDEO_TRACK_KIND)){
            videoTrackAdapters.addAdapter((VideoTrack) track);
        }
        remoteTracks.put(track.id(), track);


        WritableMap params = Arguments.createMap();
        WritableMap eventInfo = Arguments.createMap();
        WritableArray streams = Arguments.createArray();

        for (MediaStream stream : mediaStreams) {
            // Getting the streamReactTag 
            String streamReactTag = null;
            for (Map.Entry<String, MediaStream> e : remoteStreams.entrySet()) {
                if (e.getValue().equals(stream)) {
                    streamReactTag = e.getKey();
                    break;
                }
            }
            if (streamReactTag == null) {
                streamReactTag = UUID.randomUUID().toString();
                remoteStreams.put(streamReactTag, stream);
            }
            streams.pushMap(serializeStream(streamReactTag, stream));
        }

        eventInfo.putArray("streams", streams);
        eventInfo.putMap("track", serializeTrack(receiver.track()));
        eventInfo.putMap("receiver", serializeReceiver(receiver));
        eventInfo.putString("timestamp", ts);
        // Getting the transceiver object associated with the receiver for the event
        List<RtpTransceiver> transceivers = peerConnection.getTransceivers();
        for( RtpTransceiver transceiver : transceivers ) {
            if(transceiver.getReceiver() != null && receiver.id().equals(transceiver.getReceiver().id())) {
                eventInfo.putMap("transceiver", serializeTransceiver(transceiver));
                break;
            }
        }
        params.putInt("id", this.id);
        params.putMap("info", eventInfo);

        webRTCModule.sendEvent("peerConnectionOnTrack", params);
    }

    @Override
    public void onTrack(final RtpTransceiver transceiver) {
        Log.d(TAG, "onTrack");

        RtpReceiver receiver = transceiver.getReceiver();
        MediaStreamTrack track = receiver.track();

        // Get The timestamp so that we can reorder the transceivers on the JS layer
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();

        if (track == null || sdpSemantics != PeerConnection.SdpSemantics.UNIFIED_PLAN) {
            return;
        }
        if(track.kind().equals(MediaStreamTrack.VIDEO_TRACK_KIND)){
            videoTrackAdapters.addAdapter((VideoTrack) track);
        }
        remoteTracks.put(track.id(), track);

        WritableMap params = Arguments.createMap();
        WritableMap eventInfo = Arguments.createMap();
        WritableArray streams = Arguments.createArray();
        
        eventInfo.putArray("streams", streams);
        eventInfo.putMap("track", serializeTrack(receiver.track()));
        eventInfo.putMap("receiver", serializeReceiver(receiver));
        eventInfo.putMap("transceiver", serializeTransceiver(transceiver));
        eventInfo.putString("timestamp", ts);

        // Getting the transceiver object associated with the receiver for the event
        params.putInt("id", this.id);
        params.putMap("info", eventInfo);

        webRTCModule.sendEvent("peerConnectionOnTrack", params);
    }

    // This is only added to compile. Plan B is not supported anymore.
    @Override
    public void onRemoveStream(MediaStream stream) {

    }

    // This is only added to compile. Plan B is not supported anymore.
    @Override
    public void onAddStream(MediaStream stream) {

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

    /**
     * Serialization and Parsing helpers
     */

    private WritableMap serializeStream(String streamReactTag, MediaStream stream) {

        Log.d(TAG, "streamReactTag = " + streamReactTag);
        WritableMap params = Arguments.createMap();
        params.putInt("id", id);
        params.putString("streamId", stream.getId());
        params.putString("streamReactTag", streamReactTag);

        WritableArray tracks = Arguments.createArray();

        for (VideoTrack track: stream.videoTracks) {
            tracks.pushMap(serializeTrack(track));
        }
        for (AudioTrack track: stream.audioTracks) {
            tracks.pushMap(serializeTrack(track));
        }

        params.putArray("tracks", tracks);

        return params;
    }

    public String serializeDirection(RtpTransceiver.RtpTransceiverDirection src) {
        switch(src) {
            case INACTIVE:
                return "inactive";
            case RECV_ONLY:
                return "recvonly";
            case SEND_ONLY:
                return "sendonly";
            case SEND_RECV:
                return "sendrecv";
            default:
                throw new Error("Invalid direction");
        }
    }

    private ReadableMap serializeTrack(MediaStreamTrack track) {
        WritableMap trackInfo = Arguments.createMap();
        trackInfo.putString("id", track.id());
        trackInfo.putInt("peerConnectionId", this.id);
        if (track.kind().equals("video")) {
            trackInfo.putString("label", "Video");
        } else if (track.kind().equals("audio")) {
            trackInfo.putString("label", "Aideo");
        } else {
            throw new Error("Unknown kind: " + track.kind());
        }
        trackInfo.putString("kind", track.kind());
        trackInfo.putBoolean("enabled", track.enabled());
        trackInfo.putString("readyState", track.state().toString());
        trackInfo.putBoolean("remote", true);
        return trackInfo;
    }

    /**
     * This method is currently missing serializing DtmfSender
     * and transport.
     * // TODO: Add transport and dtmf fields to the serialized sender to match the web APIs
     */
    private ReadableMap serializeSender(RtpSender sender) {
        WritableMap res = Arguments.createMap();
        res.putString("id", sender.id());
        res.putInt("peerConnectionId", this.id);
        if (sender.track() != null)
            res.putMap("track", serializeTrack(sender.track()));
        return res;
    }

    private ReadableMap serializeReceiver(RtpReceiver receiver) {
        WritableMap res = Arguments.createMap();
        res.putString("id", receiver.id());
        res.putInt("peerConnectionId", this.id);
        res.putMap("track", serializeTrack(receiver.track()));
        return res;
    }

    public ReadableMap serializeTransceiver(RtpTransceiver transceiver) {
        WritableMap res = Arguments.createMap();
        res.putString("id", transceiver.getSender().id());
        res.putInt("peerConnectionId", this.id);
        String mid = transceiver.getMid();
        if (mid != null) {
            res.putString("mid", mid);
        }
        res.putString("direction", serializeDirection(transceiver.getDirection()));
        RtpTransceiver.RtpTransceiverDirection currentDirection = transceiver.getCurrentDirection();
        if (currentDirection != null) {
            res.putString("currentDirection", serializeDirection(transceiver.getCurrentDirection()));
        }
        res.putBoolean("isStopped", transceiver.isStopped());
        res.putMap("receiver", serializeReceiver(transceiver.getReceiver()));
        res.putMap("sender", serializeSender(transceiver.getSender()));
        return res;
    }
}
