package com.oney.WebRTCModule;

import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.webrtc.*;

@ReactModule(name = "WebRTCModule")
public class WebRTCModule extends ReactContextBaseJavaModule {
    static final String TAG = WebRTCModule.class.getCanonicalName();

    PeerConnectionFactory mFactory;
    private final SparseArray<PeerConnectionObserver> mPeerConnectionObservers;
    final Map<String, MediaStream> localStreams;

    /**
     * The implementation of {@code getUserMedia} extracted into a separate file
     * in order to reduce complexity and to (somewhat) separate concerns.
     */
    private GetUserMediaImpl getUserMediaImpl;

    public WebRTCModule(ReactApplicationContext reactContext) {
        super(reactContext);

        mPeerConnectionObservers = new SparseArray<>();
        localStreams = new HashMap<>();

        ThreadUtils.runOnExecutor(() -> initAsync());
    }

    /**
     * Invoked asynchronously to initialize this {@code WebRTCModule} instance.
     */
    private void initAsync() {
        ReactApplicationContext reactContext = getReactApplicationContext();

        // Initialize EGL context required for HW acceleration.
        EglBase.Context eglContext = EglUtils.getRootEglBaseContext();

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(reactContext)
                .createInitializationOptions());

        VideoEncoderFactory encoderFactory;
        VideoDecoderFactory decoderFactory;

        if (eglContext != null) {
            encoderFactory
                = new DefaultVideoEncoderFactory(
                    eglContext,
                    /* enableIntelVp8Encoder */ true,
                    /* enableH264HighProfile */ false);
            decoderFactory = new DefaultVideoDecoderFactory(eglContext);
        } else {
            encoderFactory = new SoftwareVideoEncoderFactory();
            decoderFactory = new SoftwareVideoDecoderFactory();
        }

        mFactory
            = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();

        getUserMediaImpl = new GetUserMediaImpl(this, reactContext);
    }

    @Override
    public String getName() {
        return "WebRTCModule";
    }

    private PeerConnection getPeerConnection(int id) {
        PeerConnectionObserver pco = mPeerConnectionObservers.get(id);
        return (pco == null) ? null : pco.getPeerConnection();
    }

    void sendEvent(String eventName, @Nullable WritableMap params) {
        getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    private PeerConnection.IceServer createIceServer(String url) {
        return PeerConnection.IceServer.builder(url).createIceServer();
    }

    private PeerConnection.IceServer createIceServer(String url, String username, String credential) {
        return PeerConnection.IceServer.builder(url)
            .setUsername(username)
            .setPassword(credential)
            .createIceServer();
    }

    private List<PeerConnection.IceServer> createIceServers(ReadableArray iceServersArray) {
        final int size = (iceServersArray == null) ? 0 : iceServersArray.size();
        List<PeerConnection.IceServer> iceServers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ReadableMap iceServerMap = iceServersArray.getMap(i);
            boolean hasUsernameAndCredential = iceServerMap.hasKey("username") && iceServerMap.hasKey("credential");
            if (iceServerMap.hasKey("url")) {
                if (hasUsernameAndCredential) {
                    iceServers.add(createIceServer(iceServerMap.getString("url"), iceServerMap.getString("username"), iceServerMap.getString("credential")));
                } else {
                    iceServers.add(createIceServer(iceServerMap.getString("url")));
                }
            } else if (iceServerMap.hasKey("urls")) {
                switch (iceServerMap.getType("urls")) {
                    case String:
                        if (hasUsernameAndCredential) {
                            iceServers.add(createIceServer(iceServerMap.getString("urls"), iceServerMap.getString("username"), iceServerMap.getString("credential")));
                        } else {
                            iceServers.add(createIceServer(iceServerMap.getString("urls")));
                        }
                        break;
                    case Array:
                        ReadableArray urls = iceServerMap.getArray("urls");
                        for (int j = 0; j < urls.size(); j++) {
                            String url = urls.getString(j);
                            if (hasUsernameAndCredential) {
                                iceServers.add(createIceServer(url,iceServerMap.getString("username"), iceServerMap.getString("credential")));
                            } else {
                                iceServers.add(createIceServer(url));
                            }
                        }
                        break;
                }
            }
        }
        return iceServers;
    }

    private PeerConnection.RTCConfiguration parseRTCConfiguration(ReadableMap map) {
        ReadableArray iceServersArray = null;
        if (map != null) {
            iceServersArray = map.getArray("iceServers");
        }
        List<PeerConnection.IceServer> iceServers = createIceServers(iceServersArray);
        PeerConnection.RTCConfiguration conf = new PeerConnection.RTCConfiguration(iceServers);
        if (map == null) {
            return conf;
        }

        // iceTransportPolicy (public api)
        if (map.hasKey("iceTransportPolicy")
                && map.getType("iceTransportPolicy") == ReadableType.String) {
            final String v = map.getString("iceTransportPolicy");
            if (v != null) {
                switch (v) {
                case "all": // public
                    conf.iceTransportsType = PeerConnection.IceTransportsType.ALL;
                    break;
                case "relay": // public
                    conf.iceTransportsType = PeerConnection.IceTransportsType.RELAY;
                    break;
                case "nohost":
                    conf.iceTransportsType = PeerConnection.IceTransportsType.NOHOST;
                    break;
                case "none":
                    conf.iceTransportsType = PeerConnection.IceTransportsType.NONE;
                    break;
                }
            }
        }

        // bundlePolicy (public api)
        if (map.hasKey("bundlePolicy")
                && map.getType("bundlePolicy") == ReadableType.String) {
            final String v = map.getString("bundlePolicy");
            if (v != null) {
                switch (v) {
                case "balanced": // public
                    conf.bundlePolicy = PeerConnection.BundlePolicy.BALANCED;
                    break;
                case "max-compat": // public
                    conf.bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT;
                    break;
                case "max-bundle": // public
                    conf.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
                    break;
                }
            }
        }

        // rtcpMuxPolicy (public api)
        if (map.hasKey("rtcpMuxPolicy")
                && map.getType("rtcpMuxPolicy") == ReadableType.String) {
            final String v = map.getString("rtcpMuxPolicy");
            if (v != null) {
                switch (v) {
                case "negotiate": // public
                    conf.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.NEGOTIATE;
                    break;
                case "require": // public
                    conf.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
                    break;
                }
            }
        }

        // FIXME: peerIdentity of type DOMString (public api)
        // FIXME: certificates of type sequence<RTCCertificate> (public api)

        // iceCandidatePoolSize of type unsigned short, defaulting to 0
        if (map.hasKey("iceCandidatePoolSize")
                && map.getType("iceCandidatePoolSize") == ReadableType.Number) {
            final int v = map.getInt("iceCandidatePoolSize");
            if (v > 0) {
                conf.iceCandidatePoolSize = v;
            }
        }

        // === below is private api in webrtc ===

        // tcpCandidatePolicy (private api)
        if (map.hasKey("tcpCandidatePolicy")
                && map.getType("tcpCandidatePolicy") == ReadableType.String) {
            final String v = map.getString("tcpCandidatePolicy");
            if (v != null) {
                switch (v) {
                case "enabled":
                    conf.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED;
                    break;
                case "disabled":
                    conf.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
                    break;
                }
            }
        }

        // candidateNetworkPolicy (private api)
        if (map.hasKey("candidateNetworkPolicy")
                && map.getType("candidateNetworkPolicy") == ReadableType.String) {
            final String v = map.getString("candidateNetworkPolicy");
            if (v != null) {
                switch (v) {
                case "all":
                    conf.candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.ALL;
                    break;
                case "low_cost":
                    conf.candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.LOW_COST;
                    break;
                }
            }
        }

        // KeyType (private api)
        if (map.hasKey("keyType")
                && map.getType("keyType") == ReadableType.String) {
            final String v = map.getString("keyType");
            if (v != null) {
                switch (v) {
                case "RSA":
                    conf.keyType = PeerConnection.KeyType.RSA;
                    break;
                case "ECDSA":
                    conf.keyType = PeerConnection.KeyType.ECDSA;
                    break;
                }
            }
        }

        // continualGatheringPolicy (private api)
        if (map.hasKey("continualGatheringPolicy")
                && map.getType("continualGatheringPolicy") == ReadableType.String) {
            final String v = map.getString("continualGatheringPolicy");
            if (v != null) {
                switch (v) {
                case "gather_once":
                    conf.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE;
                    break;
                case "gather_continually":
                    conf.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
                    break;
                }
            }
        }

        // audioJitterBufferMaxPackets (private api)
        if (map.hasKey("audioJitterBufferMaxPackets")
                && map.getType("audioJitterBufferMaxPackets") == ReadableType.Number) {
            final int v = map.getInt("audioJitterBufferMaxPackets");
            if (v > 0) {
                conf.audioJitterBufferMaxPackets = v;
            }
        }

        // iceConnectionReceivingTimeout (private api)
        if (map.hasKey("iceConnectionReceivingTimeout")
                && map.getType("iceConnectionReceivingTimeout") == ReadableType.Number) {
            final int v = map.getInt("iceConnectionReceivingTimeout");
            conf.iceConnectionReceivingTimeout = v;
        }

        // iceBackupCandidatePairPingInterval (private api)
        if (map.hasKey("iceBackupCandidatePairPingInterval")
                && map.getType("iceBackupCandidatePairPingInterval") == ReadableType.Number) {
            final int v = map.getInt("iceBackupCandidatePairPingInterval");
            conf.iceBackupCandidatePairPingInterval = v;
        }

        // audioJitterBufferFastAccelerate (private api)
        if (map.hasKey("audioJitterBufferFastAccelerate")
                && map.getType("audioJitterBufferFastAccelerate") == ReadableType.Boolean) {
            final boolean v = map.getBoolean("audioJitterBufferFastAccelerate");
            conf.audioJitterBufferFastAccelerate = v;
        }

        // pruneTurnPorts (private api)
        if (map.hasKey("pruneTurnPorts")
                && map.getType("pruneTurnPorts") == ReadableType.Boolean) {
            final boolean v = map.getBoolean("pruneTurnPorts");
            conf.pruneTurnPorts = v;
        }

        // presumeWritableWhenFullyRelayed (private api)
        if (map.hasKey("presumeWritableWhenFullyRelayed")
                && map.getType("presumeWritableWhenFullyRelayed") == ReadableType.Boolean) {
            final boolean v = map.getBoolean("presumeWritableWhenFullyRelayed");
            conf.presumeWritableWhenFullyRelayed = v;
        }

        return conf;
    }

    @ReactMethod
    public void peerConnectionInit(ReadableMap configuration, int id) {
        PeerConnection.RTCConfiguration rtcConfiguration
            = parseRTCConfiguration(configuration);

        ThreadUtils.runOnExecutor(() ->
            peerConnectionInitAsync(rtcConfiguration, id));
    }

    private void peerConnectionInitAsync(
            PeerConnection.RTCConfiguration configuration,
            int id) {
        PeerConnectionObserver observer = new PeerConnectionObserver(this, id);
        PeerConnection peerConnection
            = mFactory.createPeerConnection(configuration, observer);

        observer.setPeerConnection(peerConnection);
        mPeerConnectionObservers.put(id, observer);
    }

    MediaStream getStreamForReactTag(String streamReactTag) {
        MediaStream stream = localStreams.get(streamReactTag);

        if (stream == null) {
            for (int i = 0, size = mPeerConnectionObservers.size();
                    i < size;
                    i++) {
                PeerConnectionObserver pco
                    = mPeerConnectionObservers.valueAt(i);
                stream = pco.remoteStreams.get(streamReactTag);
                if (stream != null) {
                    break;
                }
            }
        }

        return stream;
    }

    private MediaStreamTrack getTrack(String trackId) {
        MediaStreamTrack track = getLocalTrack(trackId);

        if (track == null) {
            for (int i = 0, size = mPeerConnectionObservers.size();
                    i < size;
                    i++) {
                PeerConnectionObserver pco
                    = mPeerConnectionObservers.valueAt(i);
                track = pco.remoteTracks.get(trackId);
                if (track != null) {
                    break;
                }
            }
        }

        return track;
    }

    MediaStreamTrack getLocalTrack(String trackId) {
        return getUserMediaImpl.getTrack(trackId);
    }

    private static MediaStreamTrack getLocalTrack(
            MediaStream localStream,
            String trackId) {
        for (AudioTrack track : localStream.audioTracks) {
            if (track.id().equals(trackId)) {
                return track;
            }
        }
        for (VideoTrack track : localStream.videoTracks) {
            if (track.id().equals(trackId)) {
                return track;
            }
        }
        return null;
    }

    /**
     * Parses a constraint set specified in the form of a JavaScript object into
     * a specific <tt>List</tt> of <tt>MediaConstraints.KeyValuePair</tt>s.
     *
     * @param src The constraint set in the form of a JavaScript object to
     * parse.
     * @param dst The <tt>List</tt> of <tt>MediaConstraints.KeyValuePair</tt>s
     * into which the specified <tt>src</tt> is to be parsed.
     */
    private void parseConstraints(
            ReadableMap src,
            List<MediaConstraints.KeyValuePair> dst) {
        ReadableMapKeySetIterator keyIterator = src.keySetIterator();

        while (keyIterator.hasNextKey()) {
            String key = keyIterator.nextKey();
            String value = ReactBridgeUtil.getMapStrValue(src, key);

            dst.add(new MediaConstraints.KeyValuePair(key, value));
        }
    }

    /**
     * Parses "GUM" constraints and RtcOfferAnswerOptions described by a
     * specific <tt>ReadableMap</tt>.
     *
     * @param constraints A <tt>ReadableMap</tt> which represents a JavaScript
     * object specifying the constraints to be parsed into a
     * <tt>MediaConstraints</tt> instance.
     * @return A new <tt>MediaConstraints</tt> instance initialized with the
     * mandatory and optional constraint keys and values specified by
     * <tt>constraints</tt>.
     */
    MediaConstraints parseMediaConstraints(ReadableMap constraints) {
        MediaConstraints mediaConstraints = new MediaConstraints();
        if (constraints != null) {
            parseConstraints(constraints, mediaConstraints.mandatory);
        }

        return mediaConstraints;
    }

    @ReactMethod
    public void getUserMedia(ReadableMap constraints,
                             Callback    successCallback,
                             Callback    errorCallback) {
        ThreadUtils.runOnExecutor(() ->
            getUserMediaImpl.getUserMedia(constraints, successCallback, errorCallback));
    }

    @ReactMethod
    public void mediaStreamRelease(String id) {
        ThreadUtils.runOnExecutor(() -> mediaStreamReleaseAsync(id));
    }

    private void mediaStreamReleaseAsync(String id) {
        MediaStream stream = localStreams.get(id);
        if (stream == null) {
            Log.d(TAG, "mediaStreamRelease() stream is null");
        } else {
            // XXX Copy the lists of audio and video tracks because we'll be
            // incrementally modifying them. Though a while loop with isEmpty()
            // is generally a clearer approach (employed by MediaStream), we'll
            // be searching through our own lists and these may (or may not) get
            // out of sync with MediaStream's lists which raises the risk of
            // entering infinite loops.
            List<MediaStreamTrack> tracks
                = new ArrayList<>(
                    stream.audioTracks.size() + stream.videoTracks.size());

            tracks.addAll(stream.audioTracks);
            tracks.addAll(stream.videoTracks);
            for (MediaStreamTrack track : tracks) {
                 mediaStreamTrackRelease(id, track.id());
            }

            localStreams.remove(id);

            // MediaStream.dispose() may be called without an exception only if
            // it's no longer added to any PeerConnection.
            for (int i = 0, size = mPeerConnectionObservers.size();
                    i < size;
                    i++) {
                mPeerConnectionObservers.valueAt(i).removeStream(stream);
            }

            stream.dispose();
        }
    }

    @ReactMethod
    public void enumerateDevices(Callback callback) {
        ThreadUtils.runOnExecutor(() ->
            callback.invoke(getUserMediaImpl.enumerateDevices()));
    }

    @ReactMethod
    public void mediaStreamTrackRelease(String streamId, String trackId) {
        ThreadUtils.runOnExecutor(() ->
            mediaStreamTrackReleaseAsync(streamId, trackId));
    }

    private void mediaStreamTrackReleaseAsync(String streamId, String trackId) {
        MediaStream stream = localStreams.get(streamId);
        if (stream == null) {
            Log.d(TAG, "mediaStreamTrackRelease() stream is null");
            return;
        }
        MediaStreamTrack track = getLocalTrack(trackId);
        if (track == null) {
            // XXX The specified trackId may have already been stopped by
            // mediaStreamTrackStop().
            track = getLocalTrack(stream, trackId);
            if (track == null) {
                Log.d(
                    TAG,
                    "mediaStreamTrackRelease() No local MediaStreamTrack with id "
                        + trackId);
                return;
            }
        } else {
            mediaStreamTrackStop(trackId);
        }

        String kind = track.kind();
        if ("audio".equals(kind)) {
            stream.removeTrack((AudioTrack)track);
        } else if ("video".equals(kind)) {
            stream.removeTrack((VideoTrack)track);
        }
        track.dispose();
    }

    @ReactMethod
    public void mediaStreamTrackSetEnabled(String id, boolean enabled) {
        ThreadUtils.runOnExecutor(() ->
            mediaStreamTrackSetEnabledAsync(id, enabled));
    }

    private void mediaStreamTrackSetEnabledAsync(String id, boolean enabled) {
        MediaStreamTrack track = getTrack(id);
        if (track == null) {
            Log.d(TAG, "mediaStreamTrackSetEnabled() track is null");
            return;
        } else if (track.enabled() == enabled) {
            return;
        }
        track.setEnabled(enabled);
        getUserMediaImpl.mediaStreamTrackSetEnabled(id, enabled);
    }

    @ReactMethod
    public void mediaStreamTrackStop(String trackId) {
        getUserMediaImpl.mediaStreamTrackStop(trackId);
    }

    @ReactMethod
    public void mediaStreamTrackSwitchCamera(String id) {
        MediaStreamTrack track = getLocalTrack(id);
        if (track != null) {
            getUserMediaImpl.switchCamera(id);
        }
    }

    @ReactMethod
    public void peerConnectionSetConfiguration(ReadableMap configuration,
                                               int id) {
        ThreadUtils.runOnExecutor(() ->
            peerConnectionSetConfigurationAsync(configuration, id));
    }

    private void peerConnectionSetConfigurationAsync(ReadableMap configuration,
                                                     int id) {
        PeerConnection peerConnection = getPeerConnection(id);
        if (peerConnection == null) {
            Log.d(TAG, "peerConnectionSetConfiguration() peerConnection is null");
            return;
        }
        peerConnection.setConfiguration(parseRTCConfiguration(configuration));
    }

    @ReactMethod
    public void peerConnectionAddStream(String streamId, int id) {
        ThreadUtils.runOnExecutor(() ->
            peerConnectionAddStreamAsync(streamId, id));
    }

    private void peerConnectionAddStreamAsync(String streamId, int id) {
        MediaStream mediaStream = localStreams.get(streamId);
        if (mediaStream == null) {
            Log.d(TAG, "peerConnectionAddStream() mediaStream is null");
            return;
        }
        PeerConnectionObserver pco = mPeerConnectionObservers.get(id);
        if (pco == null || !pco.addStream(mediaStream)) {
            Log.e(TAG, "peerConnectionAddStream() failed");
        }
    }

    @ReactMethod
    public void peerConnectionRemoveStream(String streamId, int id) {
        ThreadUtils.runOnExecutor(() ->
            peerConnectionRemoveStreamAsync(streamId, id));
    }

    private void peerConnectionRemoveStreamAsync(String streamId, int id) {
        MediaStream mediaStream = localStreams.get(streamId);
        if (mediaStream == null) {
            Log.d(TAG, "peerConnectionRemoveStream() mediaStream is null");
            return;
        }
        PeerConnectionObserver pco = mPeerConnectionObservers.get(id);
        if (pco == null || !pco.removeStream(mediaStream)) {
            Log.e(TAG, "peerConnectionRemoveStream() failed");
        }
    }

    @ReactMethod
    public void peerConnectionAddTrack(String trackId, ReadableArray streamIds,
                                       int id, Promise promise) {
        ThreadUtils.runOnExecutor(() ->
            peerConnectionAddTrackAsync(trackId, streamIds, id, promise));
    }

    private void peerConnectionAddTrackAsync(String trackId, ReadableArray streamIds,
                                             int id, Promise promise) {
        PeerConnectionObserver pco = mPeerConnectionObservers.get(id);
        if (pco == null) {
            Log.e(TAG, "peerConnectionAddTrack() failed, peerconnection not found");
            promise.reject("NotFoundError", "peerconnection not found");
            return;
        }
        ArrayList<String> streamIdsList = new ArrayList<String>();
        for (int i=0; i<streamIds.size(); i++) {
            streamIdsList.add(streamIds.getString(i));
        }
        WritableMap rtpSender = pco.addTrack(trackId, streamIdsList);
        if (rtpSender == null) {
            Log.e(TAG, "peerConnectionAddTrack() failed to add track");
            promise.reject("PeerConnectionError", "failed to add the track");
            return;
        }
        promise.resolve(rtpSender);
    }

    @ReactMethod
    public void peerConnectionRemoveTrack(String rtpSenderId, int id) {
        ThreadUtils.runOnExecutor(() ->
            peerConnectionRemoveTrackAsync(rtpSenderId, id));
    }

    private void peerConnectionRemoveTrackAsync(String rtpSenderId, int id) {
        PeerConnectionObserver pco = mPeerConnectionObservers.get(id);
        if (pco == null || !pco.removeTrack(rtpSenderId)) {
            Log.e(TAG, "peerConnectionRemoveTrack() failed");
        }
    }

    @ReactMethod
    public void peerConnectionCreateOffer(int id,
                                          ReadableMap options,
                                          Callback callback) {
        ThreadUtils.runOnExecutor(() ->
            peerConnectionCreateOfferAsync(id, options, callback));
    }

    private void peerConnectionCreateOfferAsync(int id,
                                                ReadableMap options,
                                                final Callback callback) {
        PeerConnection peerConnection = getPeerConnection(id);

        if (peerConnection != null) {
            peerConnection.createOffer(new SdpObserver() {
                @Override
                public void onCreateFailure(String s) {
                    callback.invoke(false, s);
                }

                @Override
                public void onCreateSuccess(SessionDescription sdp) {
                    WritableMap params = Arguments.createMap();
                    params.putString("sdp", sdp.description);
                    params.putString("type", sdp.type.canonicalForm());
                    callback.invoke(true, params);
                }

                @Override
                public void onSetFailure(String s) {}

                @Override
                public void onSetSuccess() {}
            }, parseMediaConstraints(options));
        } else {
            Log.d(TAG, "peerConnectionCreateOffer() peerConnection is null");
            callback.invoke(false, "peerConnection is null");
        }
    }

    @ReactMethod
    public void peerConnectionCreateAnswer(int id,
                                           ReadableMap options,
                                           Callback callback) {
        ThreadUtils.runOnExecutor(() ->
            peerConnectionCreateAnswerAsync(id, options, callback));
    }

    private void peerConnectionCreateAnswerAsync(int id,
                                                 ReadableMap options,
                                                 final Callback callback) {
        PeerConnection peerConnection = getPeerConnection(id);

        if (peerConnection != null) {
            peerConnection.createAnswer(new SdpObserver() {
                @Override
                public void onCreateFailure(String s) {
                    callback.invoke(false, s);
                }

                @Override
                public void onCreateSuccess(SessionDescription sdp) {
                    WritableMap params = Arguments.createMap();
                    params.putString("sdp", sdp.description);
                    params.putString("type", sdp.type.canonicalForm());
                    callback.invoke(true, params);
                }

                @Override
                public void onSetFailure(String s) {}

                @Override
                public void onSetSuccess() {}
            }, parseMediaConstraints(options));
        } else {
            Log.d(TAG, "peerConnectionCreateAnswer() peerConnection is null");
            callback.invoke(false, "peerConnection is null");
        }
    }

    @ReactMethod
    public void peerConnectionSetLocalDescription(ReadableMap sdpMap,
                                                  int id,
                                                  Callback callback) {
        ThreadUtils.runOnExecutor(() ->
            peerConnectionSetLocalDescriptionAsync(sdpMap, id, callback));
    }

    private void peerConnectionSetLocalDescriptionAsync(ReadableMap sdpMap,
                                                        int id,
                                                        final Callback callback) {
        PeerConnection peerConnection = getPeerConnection(id);

        Log.d(TAG, "peerConnectionSetLocalDescription() start");
        if (peerConnection != null) {
            SessionDescription sdp = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(sdpMap.getString("type")),
                sdpMap.getString("sdp")
            );

            peerConnection.setLocalDescription(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sdp) {
                }

                @Override
                public void onSetSuccess() {
                    callback.invoke(true);
                }

                @Override
                public void onCreateFailure(String s) {
                }

                @Override
                public void onSetFailure(String s) {
                    callback.invoke(false, s);
                }
            }, sdp);
        } else {
            Log.d(TAG, "peerConnectionSetLocalDescription() peerConnection is null");
            callback.invoke(false, "peerConnection is null");
        }
        Log.d(TAG, "peerConnectionSetLocalDescription() end");
    }

    @ReactMethod
    public void peerConnectionSetRemoteDescription(ReadableMap sdpMap,
                                                   int id,
                                                   Callback callback) {
        ThreadUtils.runOnExecutor(() ->
            peerConnectionSetRemoteDescriptionAsync(sdpMap, id, callback));
    }

    private void peerConnectionSetRemoteDescriptionAsync(ReadableMap sdpMap,
                                                         int id,
                                                         final Callback callback) {
        PeerConnection peerConnection = getPeerConnection(id);

        Log.d(TAG, "peerConnectionSetRemoteDescription() start");
        if (peerConnection != null) {
            SessionDescription sdp = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(sdpMap.getString("type")),
                sdpMap.getString("sdp")
            );

            peerConnection.setRemoteDescription(new SdpObserver() {
                @Override
                public void onCreateSuccess(final SessionDescription sdp) {
                }

                @Override
                public void onSetSuccess() {
                    callback.invoke(true);
                }

                @Override
                public void onCreateFailure(String s) {
                }

                @Override
                public void onSetFailure(String s) {
                    callback.invoke(false, s);
                }
            }, sdp);
        } else {
            Log.d(TAG, "peerConnectionSetRemoteDescription() peerConnection is null");
            callback.invoke(false, "peerConnection is null");
        }
        Log.d(TAG, "peerConnectionSetRemoteDescription() end");
    }

    @ReactMethod
    public void peerConnectionAddICECandidate(ReadableMap candidateMap,
                                              int id,
                                              Callback callback) {
        ThreadUtils.runOnExecutor(() ->
            peerConnectionAddICECandidateAsync(candidateMap, id, callback));
    }

    private void peerConnectionAddICECandidateAsync(ReadableMap candidateMap,
                                                    int id,
                                                    Callback callback) {
        boolean result = false;
        PeerConnection peerConnection = getPeerConnection(id);
        Log.d(TAG, "peerConnectionAddICECandidate() start");
        if (peerConnection != null) {
            IceCandidate candidate = new IceCandidate(
                candidateMap.getString("sdpMid"),
                candidateMap.getInt("sdpMLineIndex"),
                candidateMap.getString("candidate")
            );
            result = peerConnection.addIceCandidate(candidate);
        } else {
            Log.d(TAG, "peerConnectionAddICECandidate() peerConnection is null");
        }
        callback.invoke(result);
        Log.d(TAG, "peerConnectionAddICECandidate() end");
    }

    @ReactMethod
    public void peerConnectionGetStats(String trackId, int id, Callback cb) {
        ThreadUtils.runOnExecutor(() ->
            peerConnectionGetStatsAsync(trackId, id, cb));
    }

    private void peerConnectionGetStatsAsync(String trackId,
                                             int id,
                                             Callback cb) {
        PeerConnectionObserver pco = mPeerConnectionObservers.get(id);
        if (pco == null || pco.getPeerConnection() == null) {
            Log.d(TAG, "peerConnectionGetStats() peerConnection is null");
            cb.invoke(false, "PeerConnection ID not found");
        } else {
            pco.getStats(trackId, cb);
        }
    }

    @ReactMethod
    public void peerConnectionClose(int id) {
        ThreadUtils.runOnExecutor(() -> peerConnectionCloseAsync(id));
    }

    private void peerConnectionCloseAsync(int id) {
        PeerConnectionObserver pco = mPeerConnectionObservers.get(id);
        if (pco == null || pco.getPeerConnection() == null) {
            Log.d(TAG, "peerConnectionClose() peerConnection is null");
        } else {
            pco.close();
            mPeerConnectionObservers.remove(id);
        }
    }

    @ReactMethod
    public void createDataChannel(int peerConnectionId,
                                  String label,
                                  ReadableMap config) {
        ThreadUtils.runOnExecutor(() ->
            createDataChannelAsync(peerConnectionId, label, config));
    }

    private void createDataChannelAsync(int peerConnectionId,
                                        String label,
                                        ReadableMap config) {
        // Forward to PeerConnectionObserver which deals with DataChannels
        // because DataChannel is owned by PeerConnection.
        PeerConnectionObserver pco
            = mPeerConnectionObservers.get(peerConnectionId);
        if (pco == null || pco.getPeerConnection() == null) {
            Log.d(TAG, "createDataChannel() peerConnection is null");
        } else {
            pco.createDataChannel(label, config);
        }
    }

    @ReactMethod
    public void dataChannelClose(int peerConnectionId, int dataChannelId) {
        ThreadUtils.runOnExecutor(() ->
            dataChannelCloseAsync(peerConnectionId, dataChannelId));
    }

    private void dataChannelCloseAsync(int peerConnectionId,
                                       int dataChannelId) {
        // Forward to PeerConnectionObserver which deals with DataChannels
        // because DataChannel is owned by PeerConnection.
        PeerConnectionObserver pco
            = mPeerConnectionObservers.get(peerConnectionId);
        if (pco == null || pco.getPeerConnection() == null) {
            Log.d(TAG, "dataChannelClose() peerConnection is null");
        } else {
            pco.dataChannelClose(dataChannelId);
        }
    }

    @ReactMethod
    public void dataChannelSend(int peerConnectionId,
                                int dataChannelId,
                                String data,
                                String type) {
        ThreadUtils.runOnExecutor(() ->
            dataChannelSendAsync(peerConnectionId, dataChannelId, data, type));
    }

    private void dataChannelSendAsync(int peerConnectionId,
                                      int dataChannelId,
                                      String data,
                                      String type) {
        // Forward to PeerConnectionObserver which deals with DataChannels
        // because DataChannel is owned by PeerConnection.
        PeerConnectionObserver pco
            = mPeerConnectionObservers.get(peerConnectionId);
        if (pco == null || pco.getPeerConnection() == null) {
            Log.d(TAG, "dataChannelSend() peerConnection is null");
        } else {
            pco.dataChannelSend(dataChannelId, data, type);
        }
    }
}
