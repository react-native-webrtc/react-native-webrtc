package com.oney.WebRTCModule;

import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoTrack;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

class PeerConnectionObserver implements PeerConnection.Observer {
    private final static String TAG = WebRTCModule.TAG;

    private final Map<String, DataChannelWrapper> dataChannels;
    private final int id;
    private int transceiverNextId = 0;

    private PeerConnection peerConnection;
    final Map<String, String> remoteStreamIds; // Stream ID -> React tag
    final Map<String, MediaStream> remoteStreams; // React tag -> MediaStream
    final Map<String, MediaStreamTrack> remoteTracks;
    private final VideoTrackAdapter videoTrackAdapters;
    private final WebRTCModule webRTCModule;

    PeerConnectionObserver(WebRTCModule webRTCModule, int id) {
        this.webRTCModule = webRTCModule;
        this.id = id;
        this.dataChannels = new HashMap<>();
        this.remoteStreamIds = new HashMap<>();
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

        peerConnection.close();
    }

    void dispose() {
        Log.d(TAG, "PeerConnection.dispose() for " + id);

        // Remove video track adapters
        for (MediaStreamTrack track : this.remoteTracks.values()) {
            if (track instanceof VideoTrack) {
                videoTrackAdapters.removeAdapter((VideoTrack) track);
            }
        }

        // Remove DataChannel observers
        for (DataChannelWrapper dcw : dataChannels.values()) {
            DataChannel dataChannel = dcw.getDataChannel();
            dataChannel.unregisterObserver();
        }

        // At this point there should be no local MediaStreams in the associated
        // PeerConnection. Call dispose() to free all remaining resources held
        // by the PeerConnection instance (RtpReceivers, RtpSenders, etc.)
        peerConnection.dispose();

        remoteStreamIds.clear();
        remoteStreams.clear();
        remoteTracks.clear();
        dataChannels.clear();
    }

    public synchronized int getNextTransceiverId() {
        return transceiverNextId++;
    }

    RtpTransceiver addTransceiver(MediaStreamTrack.MediaType mediaType, RtpTransceiver.RtpTransceiverInit init) {
        if (peerConnection == null) {
            return null;
        }

        return peerConnection.addTransceiver(mediaType, init);
    }

    RtpTransceiver addTransceiver(MediaStreamTrack track, RtpTransceiver.RtpTransceiverInit init) {
        if (peerConnection == null) {
            return null;
        }

        return peerConnection.addTransceiver(track, init);
    }

    RtpSender getSender(String id) {
        if (this.peerConnection == null) {
            return null;
        }

        for (RtpSender sender : this.peerConnection.getSenders()) {
            if (sender.id().equals(id)) {
                return sender;
            }
        }

        return null;
    }

    RtpTransceiver getTransceiver(String id) {
        if (this.peerConnection == null) {
            return null;
        }

        for (RtpTransceiver transceiver : this.peerConnection.getTransceivers()) {
            if (transceiver.getSender().id().equals(id)) {
                return transceiver;
            }
        }
        return null;
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
        final String reactTag = UUID.randomUUID().toString();
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
        peerConnection.getStats(rtcStatsReport -> { promise.resolve(StringUtils.statsToJSON(rtcStatsReport)); });
    }

    /**
     * @param trackIdentifier sender or receiver id
     * @param streamType "outbound-rtp" for sender or "inbound-rtp" for receiver
     */
    void getFilteredStats(String trackIdentifier, boolean isReceiver, Promise promise) {
        peerConnection.getStats(rtcStatsReport -> {
            Map<String, RTCStats> statsMap = rtcStatsReport.getStatsMap();
            Set<RTCStats> filteredStats = new HashSet<>();
            // Get track stats
            RTCStats trackStats = getTrackStats(trackIdentifier, statsMap);
            if (trackStats == null) {
                Log.w(TAG, "getStats: couldn't find track stats!");
                RTCStatsReport report = new RTCStatsReport((long) rtcStatsReport.getTimestampUs(), new HashMap<>());
                promise.resolve(StringUtils.statsToJSON(report));
                return;
            }

            filteredStats.add(trackStats);
            String trackId = trackStats.getId();

            // Get stream stats
            RTCStats streamStats = getStreamStats(trackId, statsMap);
            if (streamStats != null) {
                filteredStats.add(streamStats);
            }

            // Get streamType stats and associated information
            Set<Long> ssrcs = new HashSet<>();
            Set<String> codecIds = new HashSet<>();

            String streamType;
            if (isReceiver) {
                streamType = "inbound-rtp";
            } else {
                streamType = "outbound-rtp";
            }

            for (RTCStats stats : statsMap.values()) {
                if (stats.getType().equals(streamType) && trackId.equals(stats.getMembers().get("trackId"))) {
                    ssrcs.add((Long) stats.getMembers().get("ssrc"));
                    codecIds.add((String) stats.getMembers().get("codecId"));
                    filteredStats.add(stats);
                }
            }

            // Get candidate information
            RTCStats candidatePairStats = null;
            for (RTCStats stats : statsMap.values()) {
                if (stats.getType().equals("candidate-pair") && stats.getMembers().get("nominated").equals(true)) {
                    candidatePairStats = stats;
                    break;
                }
            }

            String localCandidateId = null;
            String remoteCandidateId = null;
            if (candidatePairStats != null) {
                filteredStats.add(candidatePairStats);
                localCandidateId = (String) candidatePairStats.getMembers().get("localCandidateId");
                remoteCandidateId = (String) candidatePairStats.getMembers().get("remoteCandidateId");
            }

            // Sweep for any remaining stats we want.
            filteredStats.addAll(
                    getExtraStats(trackIdentifier, ssrcs, codecIds, localCandidateId, remoteCandidateId, statsMap));

            Map<String, RTCStats> filteredStatsMap = new HashMap<>();
            for (RTCStats stats : filteredStats) {
                filteredStatsMap.put(stats.getId(), stats);
            }
            RTCStatsReport filteredStatsReport =
                    new RTCStatsReport((long) rtcStatsReport.getTimestampUs(), filteredStatsMap);
            promise.resolve(StringUtils.statsToJSON(filteredStatsReport));
        });
    }

    // Note: trackIdentifier can differ from the internal stats trackId
    // trackIdentifier refers to the sender or receiver id
    @Nullable
    private RTCStats getTrackStats(String trackIdentifier, Map<String, RTCStats> statsMap) {
        for (RTCStats stats : statsMap.values()) {
            if (stats.getType().equals("track") && trackIdentifier.equals(stats.getMembers().get("trackIdentifier"))) {
                return stats;
            }
        }
        return null;
    }

    @Nullable
    private RTCStats getStreamStats(String trackId, Map<String, RTCStats> statsMap) {
        for (RTCStats stats : statsMap.values()) {
            if (stats.getType().equals("stream")
                    && Arrays.asList((String[]) stats.getMembers().get("trackIds")).contains(trackId)) {
                return stats;
            }
        }
        return null;
    }

    // Note: trackIdentifier can differ from the internal stats trackId
    // trackIdentifier refers to the sender or receiver id
    public Set<RTCStats> getExtraStats(String trackIdentifier, Set<Long> ssrcs, Set<String> codecIds,
            @Nullable String localCandidateId, @Nullable String remoteCandidateId, Map<String, RTCStats> statsMap) {
        Set<RTCStats> extraStats = new HashSet<>();
        for (RTCStats stats : statsMap.values()) {
            switch (stats.getType()) {
                case "certificate":
                case "transport":
                    extraStats.add(stats);
                    break;
            }

            if (stats.getId().equals(localCandidateId) || stats.getId().equals(remoteCandidateId)) {
                extraStats.add(stats);
                continue;
            }

            if (ssrcs.contains(stats.getMembers().get("ssrc"))) {
                extraStats.add(stats);
                continue;
            }
            if (trackIdentifier.equals(stats.getMembers().get("trackIdentifier"))) {
                extraStats.add(stats);
                continue;
            }
            if (codecIds.contains(stats.getId())) {
                extraStats.add(stats);
            }
        }

        return extraStats;
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        Log.d(TAG, "onIceCandidate");

        ThreadUtils.runOnExecutor(() -> {
            WritableMap params = Arguments.createMap();
            params.putInt("pcId", id);

            WritableMap candidateParams = Arguments.createMap();
            candidateParams.putInt("sdpMLineIndex", candidate.sdpMLineIndex);
            candidateParams.putString("sdpMid", candidate.sdpMid);
            candidateParams.putString("candidate", candidate.sdp);

            params.putMap("candidate", candidateParams);

            SessionDescription newSdp = peerConnection.getLocalDescription();
            WritableMap newSdpMap = Arguments.createMap();

            // Can happen when doing a rollback.
            if (newSdp != null) {
                newSdpMap.putString("type", newSdp.type.canonicalForm());
                newSdpMap.putString("sdp", newSdp.description);
            }
            params.putMap("sdp", newSdpMap);

            webRTCModule.sendEvent("peerConnectionGotICECandidate", params);
        });
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates) {}

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        ThreadUtils.runOnExecutor(() -> {
            WritableMap params = Arguments.createMap();
            params.putInt("pcId", id);
            params.putString("iceConnectionState", iceConnectionStateString(iceConnectionState));
            webRTCModule.sendEvent("peerConnectionIceConnectionChanged", params);
        });
    }

    @Override
    public void onConnectionChange(PeerConnection.PeerConnectionState peerConnectionState) {
        ThreadUtils.runOnExecutor(() -> {
            WritableMap params = Arguments.createMap();
            params.putInt("pcId", id);
            params.putString("connectionState", peerConnectionStateString(peerConnectionState));

            webRTCModule.sendEvent("peerConnectionStateChanged", params);
        });
    }

    @Override
    public void onIceConnectionReceivingChange(boolean receiving) {}

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.d(TAG, "onIceGatheringChange" + iceGatheringState.name());

        ThreadUtils.runOnExecutor(() -> {
            WritableMap params = Arguments.createMap();
            params.putInt("pcId", id);
            params.putString("iceGatheringState", iceGatheringStateString(iceGatheringState));

            if (iceGatheringState == PeerConnection.IceGatheringState.COMPLETE) {
                SessionDescription newSdp = peerConnection.getLocalDescription();
                WritableMap newSdpMap = Arguments.createMap();

                // Can happen when doing a rollback.
                if (newSdp != null) {
                    newSdpMap.putString("type", newSdp.type.canonicalForm());
                    newSdpMap.putString("sdp", newSdp.description);
                }
                params.putMap("sdp", newSdpMap);
            }
            webRTCModule.sendEvent("peerConnectionIceGatheringChanged", params);
        });
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        ThreadUtils.runOnExecutor(() -> {
            final String reactTag = UUID.randomUUID().toString();
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
            params.putInt("pcId", id);
            params.putMap("dataChannel", info);

            webRTCModule.sendEvent("peerConnectionDidOpenDataChannel", params);
        });
    }

    @Override
    public void onRenegotiationNeeded() {
        ThreadUtils.runOnExecutor(() -> {
            WritableMap params = Arguments.createMap();
            params.putInt("pcId", id);
            webRTCModule.sendEvent("peerConnectionOnRenegotiationNeeded", params);
        });
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        ThreadUtils.runOnExecutor(() -> {
            WritableMap params = Arguments.createMap();
            params.putInt("pcId", id);
            params.putString("signalingState", signalingStateString(signalingState));
            webRTCModule.sendEvent("peerConnectionSignalingStateChanged", params);
        });
    }

    /**
     * Triggered when a new track is signaled by the remote peer, as a result of
     * setRemoteDescription.
     */
    @Override
    public void onAddTrack(final RtpReceiver receiver, final MediaStream[] mediaStreams) {
        Log.d(TAG, "onAddTrack");

        ThreadUtils.runOnExecutor(() -> {
            RtpTransceiver transceiver = null;
            for (RtpTransceiver t : this.peerConnection.getTransceivers()) {
                if (Objects.equals(t.getReceiver().id(), receiver.id())) {
                    transceiver = t;
                    break;
                }
            }

            if (transceiver == null) {
                return;
            }

            final MediaStreamTrack track = receiver.track();

            // We need to fire this event for an existing track sometimes, like
            // when the transceiver direction (on the sending side) switches from
            // sendrecv to recvonly and then back.
            final boolean existingTrack = remoteTracks.containsKey(track.id());

            if (!existingTrack) {
                if (track.kind().equals(MediaStreamTrack.VIDEO_TRACK_KIND)) {
                    videoTrackAdapters.addAdapter((VideoTrack) track);
                }
                remoteTracks.put(track.id(), track);
            }

            WritableMap params = Arguments.createMap();
            WritableArray streams = Arguments.createArray();

            for (MediaStream stream : mediaStreams) {
                // Getting the streamReactTag
                String streamReactTag = remoteStreamIds.get(stream.getId());

                if (streamReactTag == null) {
                    streamReactTag = UUID.randomUUID().toString();
                    remoteStreamIds.put(stream.getId(), streamReactTag);
                }

                // Make sure the stored stream is updated in case we get a new reference.
                remoteStreams.put(streamReactTag, stream);

                streams.pushMap(SerializeUtils.serializeStream(id, streamReactTag, stream));
            }

            params.putArray("streams", streams);
            params.putMap("receiver", SerializeUtils.serializeReceiver(id, receiver));
            params.putInt("transceiverOrder", getNextTransceiverId());
            params.putMap("transceiver", SerializeUtils.serializeTransceiver(id, transceiver));
            params.putInt("pcId", this.id);

            webRTCModule.sendEvent("peerConnectionOnTrack", params);
        });
    }

    /**
     * Triggered when the signaling from SetRemoteDescription indicates that a transceiver
     * will be receiving media from a remote endpoint. This is only called if UNIFIED_PLAN
     * semantics are specified. The transceiver will be disposed automatically.
     */
    @Override
    public void onTrack(final RtpTransceiver transceiver) {}

    /*
     * Triggered when a previously added remote track is removed by the remote
     * peer, as a result of setRemoteDescription.
     */
    @Override
    public void onRemoveTrack(RtpReceiver receiver) {
        ThreadUtils.runOnExecutor(() -> {
            WritableMap params = Arguments.createMap();
            params.putInt("pcId", this.id);
            params.putString("receiverId", receiver.id());

            webRTCModule.sendEvent("peerConnectionOnRemoveTrack", params);
        });
    };

    // This is only added to compile. Plan B is not supported anymore.
    @Override
    public void onRemoveStream(MediaStream stream) {}

    // This is only added to compile. Plan B is not supported anymore.
    @Override
    public void onAddStream(MediaStream stream) {}

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
