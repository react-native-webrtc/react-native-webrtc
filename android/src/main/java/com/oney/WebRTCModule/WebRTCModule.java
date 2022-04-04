package com.oney.WebRTCModule;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.webrtc.*;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

@ReactModule(name = "WebRTCModule")
public class WebRTCModule extends ReactContextBaseJavaModule {
    static final String TAG = WebRTCModule.class.getCanonicalName();

    PeerConnectionFactory mFactory;
    private final SparseArray<PeerConnectionObserver> mPeerConnectionObservers;
    final Map<String, MediaStream> localStreams;

    private final GetUserMediaImpl getUserMediaImpl;

    public static class Options {
        private VideoEncoderFactory videoEncoderFactory = null;
        private VideoDecoderFactory videoDecoderFactory = null;
        private AudioDeviceModule audioDeviceModule = null;
        private Loggable injectableLogger = null;
        private Logging.Severity loggingSeverity = null;

        public Options() {}

        public void setAudioDeviceModule(AudioDeviceModule audioDeviceModule) {
            this.audioDeviceModule = audioDeviceModule;
        }

        public void setVideoDecoderFactory(VideoDecoderFactory videoDecoderFactory) {
            this.videoDecoderFactory = videoDecoderFactory;
        }

        public void setVideoEncoderFactory(VideoEncoderFactory videoEncoderFactory) {
            this.videoEncoderFactory = videoEncoderFactory;
        }

        public void setInjectableLogger(Loggable logger) {
            this.injectableLogger = logger;
        }

        public void setLoggingSeverity(Logging.Severity severity) {
            this.loggingSeverity = severity;
        }
    }

    public WebRTCModule(ReactApplicationContext reactContext) {
        this(reactContext, null);
    }

    public WebRTCModule(ReactApplicationContext reactContext, Options options) {
        super(reactContext);

        mPeerConnectionObservers = new SparseArray<>();
        localStreams = new HashMap<>();

        AudioDeviceModule adm = null;
        VideoEncoderFactory encoderFactory = null;
        VideoDecoderFactory decoderFactory = null;
        Loggable injectableLogger = null;
        Logging.Severity loggingSeverity = null;

        if (options != null) {
            adm = options.audioDeviceModule;
            encoderFactory = options.videoEncoderFactory;
            decoderFactory = options.videoDecoderFactory;
            injectableLogger = options.injectableLogger;
            loggingSeverity = options.loggingSeverity;
        }

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(reactContext)
                .setNativeLibraryLoader(new LibraryLoader())
                .setInjectableLogger(injectableLogger, loggingSeverity)
                .createInitializationOptions());

        if (encoderFactory == null || decoderFactory == null) {
            // Initialize EGL context required for HW acceleration.
            EglBase.Context eglContext = EglUtils.getRootEglBaseContext();

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
        }

        if (adm == null) {
            adm = JavaAudioDeviceModule.builder(reactContext).createAudioDeviceModule();
        }

        mFactory
            = PeerConnectionFactory.builder()
                .setAudioDeviceModule(adm)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();

        getUserMediaImpl = new GetUserMediaImpl(this, reactContext);
    }

    @NonNull
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
        if (map != null && map.hasKey("iceServers")) {
            iceServersArray = map.getArray("iceServers");
        }
        List<PeerConnection.IceServer> iceServers = createIceServers(iceServersArray);
        PeerConnection.RTCConfiguration conf = new PeerConnection.RTCConfiguration(iceServers);

        // Required for perfect negotiation.
        conf.enableImplicitRollback = true;

        // Plan B, just a little longer.
        conf.sdpSemantics = PeerConnection.SdpSemantics.PLAN_B;

        if (map == null) {
            return conf;
        }

        // iceTransportPolicy (public api)
        if (map.hasKey("iceTransportPolicy") && map.getType("iceTransportPolicy") == ReadableType.String) {
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

    @ReactMethod(isBlockingSynchronousMethod = true)
    public void peerConnectionInit(ReadableMap configuration, int id) {
        PeerConnection.RTCConfiguration rtcConfiguration = parseRTCConfiguration(configuration);

        try {
            ThreadUtils.submitToExecutor(() -> {
                PeerConnectionObserver observer = new PeerConnectionObserver(this, id);
                PeerConnection peerConnection = mFactory.createPeerConnection(rtcConfiguration, observer);
                observer.setPeerConnection(peerConnection);
                mPeerConnectionObservers.put(id, observer);
            }).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    MediaStream getStreamForReactTag(String streamReactTag) {
        // This function _only_ gets called from WebRTCView, in the UI thread.
        // Hence make sure we run this code in the executor or we run at the risk
        // of being out of sync.
        try {
            return (MediaStream) ThreadUtils.submitToExecutor((Callable<Object>) () -> {
                MediaStream stream = localStreams.get(streamReactTag);

                if (stream != null) {
                    return stream;
                }

                for (int i = 0, size = mPeerConnectionObservers.size(); i < size; i++) {
                    PeerConnectionObserver pco = mPeerConnectionObservers.valueAt(i);
                    stream = pco.remoteStreams.get(streamReactTag);
                    if (stream != null) {
                        return stream;
                    }
                }

                return null;
            }).get();
        } catch (ExecutionException | InterruptedException e) {
            return null;
        }
    }

    private MediaStreamTrack getTrack(String trackId) {
        MediaStreamTrack track = getLocalTrack(trackId);

        if (track == null) {
            for (int i = 0, size = mPeerConnectionObservers.size(); i < size; i++) {
                PeerConnectionObserver pco = mPeerConnectionObservers.valueAt(i);
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

    /**
     * Turns an "options" <tt>ReadableMap</tt> into a <tt>MediaConstraints</tt> object.
     *
     * @param options A <tt>ReadableMap</tt> which represents a JavaScript
     * object specifying the options to be parsed into a
     * <tt>MediaConstraints</tt> instance.
     * @return A new <tt>MediaConstraints</tt> instance initialized with the
     * mandatory keys and values specified by <tt>options</tt>.
     */
    MediaConstraints constraintsForOptions(ReadableMap options) {
        MediaConstraints mediaConstraints = new MediaConstraints();
        ReadableMapKeySetIterator keyIterator = options.keySetIterator();

        while (keyIterator.hasNextKey()) {
            String key = keyIterator.nextKey();
            String value = ReactBridgeUtil.getMapStrValue(options, key);

            mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(key, value));
        }

        return mediaConstraints;
    }

    @ReactMethod
    public void getDisplayMedia(Promise promise) {
        ThreadUtils.runOnExecutor(() -> getUserMediaImpl.getDisplayMedia(promise));
    }

    @ReactMethod
    public void getUserMedia(ReadableMap constraints,
                             Callback    successCallback,
                             Callback    errorCallback) {
        ThreadUtils.runOnExecutor(() ->
            getUserMediaImpl.getUserMedia(constraints, successCallback, errorCallback));
    }

    @ReactMethod
    public void enumerateDevices(Callback callback) {
        ThreadUtils.runOnExecutor(() ->
            callback.invoke(getUserMediaImpl.enumerateDevices()));
    }

    @ReactMethod
    public void mediaStreamCreate(String id) {
        ThreadUtils.runOnExecutor(() -> {
            MediaStream mediaStream = mFactory.createLocalMediaStream(id);
            localStreams.put(id, mediaStream);
        });
    }

    @ReactMethod
    public void mediaStreamAddTrack(String streamId, String trackId) {
        ThreadUtils.runOnExecutor(() -> {
            MediaStream stream = localStreams.get(streamId);
            MediaStreamTrack track = getTrack(trackId);

            if (stream == null || track == null) {
                Log.d(TAG, "mediaStreamAddTrack() stream || track is null");
                return;
            }

            String kind = track.kind();
            if ("audio".equals(kind)) {
                stream.addTrack((AudioTrack)track);
            } else if ("video".equals(kind)) {
                stream.addTrack((VideoTrack)track);
            }
        });
    }

    @ReactMethod
    public void mediaStreamRemoveTrack(String streamId, String trackId) {
        ThreadUtils.runOnExecutor(() -> {
            MediaStream stream = localStreams.get(streamId);
            MediaStreamTrack track = getTrack(trackId);

            if (stream == null || track == null) {
                Log.d(TAG, "mediaStreamRemoveTrack() stream || track is null");
                return;
            }

            String kind = track.kind();
            if ("audio".equals(kind)) {
                stream.removeTrack((AudioTrack)track);
            } else if ("video".equals(kind)) {
                stream.removeTrack((VideoTrack)track);
            }
        });
    }

    @ReactMethod
    public void mediaStreamRelease(String id) {
        ThreadUtils.runOnExecutor(() -> {
            MediaStream stream = localStreams.get(id);
            if (stream == null) {
                Log.d(TAG, "mediaStreamRelease() stream is null");
                return;
            }

            localStreams.remove(id);

            // MediaStream.dispose() may be called without an exception only if
            // it's no longer added to any PeerConnection.
            for (int i = 0, size = mPeerConnectionObservers.size(); i < size; i++) {
                mPeerConnectionObservers.valueAt(i).removeStream(stream);
            }

            stream.dispose();
        });
    }

    @ReactMethod
    public void mediaStreamTrackRelease(String id) {
        ThreadUtils.runOnExecutor(() -> {
            MediaStreamTrack track = getLocalTrack(id);
            if (track == null) {
                Log.d(TAG, "mediaStreamTrackRelease() track is null");
                return;
            }
            track.setEnabled(false);
            getUserMediaImpl.disposeTrack(id);
        });
    }

    @ReactMethod
    public void mediaStreamTrackSetEnabled(String id, boolean enabled) {
        ThreadUtils.runOnExecutor(() -> {
            MediaStreamTrack track = getTrack(id);
            if (track == null) {
                Log.d(TAG, "mediaStreamTrackSetEnabled() track is null");
                return;
            } else if (track.enabled() == enabled) {
                return;
            }
            track.setEnabled(enabled);
            getUserMediaImpl.mediaStreamTrackSetEnabled(id, enabled);
        });
    }

    @ReactMethod
    public void mediaStreamTrackSwitchCamera(String id) {
        ThreadUtils.runOnExecutor(() -> {
            MediaStreamTrack track = getLocalTrack(id);
            if (track != null) {
                getUserMediaImpl.switchCamera(id);
            }
        });
    }

    @ReactMethod
    public void peerConnectionSetConfiguration(ReadableMap configuration,
                                               int id) {
        ThreadUtils.runOnExecutor(() -> {
            PeerConnection peerConnection = getPeerConnection(id);
            if (peerConnection == null) {
                Log.d(TAG, "peerConnectionSetConfiguration() peerConnection is null");
                return;
            }
            peerConnection.setConfiguration(parseRTCConfiguration(configuration));
        });
    }

    @ReactMethod
    public void peerConnectionAddStream(String streamId, int id) {
        ThreadUtils.runOnExecutor(() -> {
            MediaStream mediaStream = localStreams.get(streamId);
            if (mediaStream == null) {
                Log.d(TAG, "peerConnectionAddStream() mediaStream is null");
                return;
            }
            PeerConnectionObserver pco = mPeerConnectionObservers.get(id);
            if (pco == null || !pco.addStream(mediaStream)) {
                Log.e(TAG, "peerConnectionAddStream() failed");
            }
        });
    }

    @ReactMethod
    public void peerConnectionRemoveStream(String streamId, int id) {
        ThreadUtils.runOnExecutor(() -> {
            MediaStream mediaStream = localStreams.get(streamId);
            if (mediaStream == null) {
                Log.d(TAG, "peerConnectionRemoveStream() mediaStream is null");
                return;
            }
            PeerConnectionObserver pco = mPeerConnectionObservers.get(id);
            if (pco == null || !pco.removeStream(mediaStream)) {
                Log.e(TAG, "peerConnectionRemoveStream() failed");
            }
        });
    }

    @ReactMethod
    public void peerConnectionCreateOffer(int id,
                                          ReadableMap options,
                                          Callback callback) {
        ThreadUtils.runOnExecutor(() -> {
            PeerConnection peerConnection = getPeerConnection(id);

            if (peerConnection == null) {
                Log.d(TAG, "peerConnectionCreateOffer() peerConnection is null");
                callback.invoke(false, "peerConnection is null");
                return;
            }

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
            }, constraintsForOptions(options));
        });
    }

    @ReactMethod
    public void peerConnectionCreateAnswer(int id,
                                           ReadableMap options,
                                           Callback callback) {
        ThreadUtils.runOnExecutor(() -> {
            PeerConnection peerConnection = getPeerConnection(id);

            if (peerConnection == null) {
                Log.d(TAG, "peerConnectionCreateAnswer() peerConnection is null");
                callback.invoke(false, "peerConnection is null");
                return;
            }

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
            }, constraintsForOptions(options));
        });
    }

    @ReactMethod
    public void peerConnectionSetLocalDescription(int pcId,
                                                  ReadableMap desc,
                                                  Promise promise) {
        ThreadUtils.runOnExecutor(() -> {
            PeerConnection peerConnection = getPeerConnection(pcId);
            if (peerConnection == null) {
                Log.d(TAG, "peerConnectionSetLocalDescription() peerConnection is null");
                promise.reject(new Exception("PeerConnection not found"));
                return;
            }

            final SdpObserver observer = new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sdp) {
                }

                @Override
                public void onSetSuccess() {
                    SessionDescription newSdp = peerConnection.getLocalDescription();
                    WritableMap newSdpMap = Arguments.createMap();
                    newSdpMap.putString("type", newSdp.type.canonicalForm());
                    newSdpMap.putString("sdp", newSdp.description);
                    promise.resolve(newSdpMap);
                }

                @Override
                public void onCreateFailure(String s) {
                }

                @Override
                public void onSetFailure(String s) {
                    promise.reject("E_OPERATION_ERROR", s);
                }
            };

            if (desc != null) {
                SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(Objects.requireNonNull(desc.getString("type"))),
                    desc.getString("sdp")
                );

                peerConnection.setLocalDescription(observer, sdp);
            } else {
                peerConnection.setLocalDescription(observer);
            }
        });
    }

    @ReactMethod
    public void peerConnectionSetRemoteDescription(ReadableMap sdpMap,
                                                   int id,
                                                   Callback callback) {
        ThreadUtils.runOnExecutor(() -> {
            PeerConnection peerConnection = getPeerConnection(id);
            if (peerConnection == null) {
                Log.d(TAG, "peerConnectionSetRemoteDescription() peerConnection is null");
                callback.invoke(false, "peerConnection is null");
                return;
            }

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
                    SessionDescription newSdp = peerConnection.getRemoteDescription();
                    WritableMap newSdpMap = Arguments.createMap();
                    newSdpMap.putString("type", newSdp.type.canonicalForm());
                    newSdpMap.putString("sdp", newSdp.description);
                    callback.invoke(true, newSdpMap);
                }

                @Override
                public void onCreateFailure(String s) {
                }

                @Override
                public void onSetFailure(String s) {
                    callback.invoke(false, s);
                }
            }, sdp);
        });
    }

    @ReactMethod
    public void peerConnectionAddICECandidate(int pcId,
                                              ReadableMap candidateMap,
                                              Promise promise) {
        ThreadUtils.runOnExecutor(() -> {
            PeerConnection peerConnection = getPeerConnection(pcId);
            if (peerConnection == null) {
                Log.d(TAG, "peerConnectionAddICECandidate() peerConnection is null");
                promise.reject(new Exception("PeerConnection not found"));
                return;
            }

            if (!(candidateMap.hasKey("sdpMid") && candidateMap.hasKey("sdpMLineIndex") && candidateMap.hasKey("sdpMid"))) {
                promise.reject("E_TYPE_ERROR", "Invalid argument");
                return;
            }

            IceCandidate candidate = new IceCandidate(
                candidateMap.getString("sdpMid"),
                candidateMap.getInt("sdpMLineIndex"),
                candidateMap.getString("candidate")
            );

            peerConnection.addIceCandidate(candidate, new AddIceObserver() {
                @Override
                public void onAddSuccess() {
                    WritableMap newSdpMap = Arguments.createMap();
                    SessionDescription newSdp = peerConnection.getRemoteDescription();
                    newSdpMap.putString("type", newSdp.type.canonicalForm());
                    newSdpMap.putString("sdp", newSdp.description);
                    promise.resolve(newSdpMap);
                }

                @Override
                public void onAddFailure(String s) {
                    promise.reject("E_OPERATION_ERROR", s);
                }
            });
        });
    }

    @ReactMethod
    public void peerConnectionGetStats(int peerConnectionId, Promise promise) {
        ThreadUtils.runOnExecutor(() -> {
            PeerConnectionObserver pco = mPeerConnectionObservers.get(peerConnectionId);
            if (pco == null || pco.getPeerConnection() == null) {
                Log.d(TAG, "peerConnectionGetStats() peerConnection is null");
                promise.reject(new Exception("PeerConnection ID not found"));
            } else {
                pco.getStats(promise);
            }
        });
    }

    @ReactMethod
    public void peerConnectionClose(int id) {
        ThreadUtils.runOnExecutor(() -> {
            PeerConnectionObserver pco = mPeerConnectionObservers.get(id);
            if (pco == null || pco.getPeerConnection() == null) {
                Log.d(TAG, "peerConnectionClose() peerConnection is null");
            } else {
                pco.close();
                mPeerConnectionObservers.remove(id);
            }
        });
    }

    @ReactMethod
    public void peerConnectionRestartIce(int pcId) {
        ThreadUtils.runOnExecutor(() -> {
            PeerConnection peerConnection = getPeerConnection(pcId);
            if (peerConnection == null) {
                Log.w(TAG, "peerConnectionRestartIce() peerConnection is null");
                return;
            }

            peerConnection.restartIce();
        });
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap createDataChannel(int peerConnectionId, String label, ReadableMap config) {
        try {
            return (WritableMap) ThreadUtils.submitToExecutor((Callable<Object>) () -> {
                PeerConnectionObserver pco = mPeerConnectionObservers.get(peerConnectionId);
                if (pco == null || pco.getPeerConnection() == null) {
                    Log.d(TAG, "createDataChannel() peerConnection is null");
                    return null;
                } else {
                    return pco.createDataChannel(label, config);
                }
            }).get();
        } catch (ExecutionException | InterruptedException e) {
            return null;
        }
    }

    @ReactMethod
    public void dataChannelClose(int peerConnectionId, String reactTag) {
        ThreadUtils.runOnExecutor(() -> {
            // Forward to PeerConnectionObserver which deals with DataChannels
            // because DataChannel is owned by PeerConnection.
            PeerConnectionObserver pco = mPeerConnectionObservers.get(peerConnectionId);
            if (pco == null || pco.getPeerConnection() == null) {
                Log.d(TAG, "dataChannelClose() peerConnection is null");
                return;
            }

            pco.dataChannelClose(reactTag);
        });
    }

    @ReactMethod
    public void dataChannelDispose(int peerConnectionId, String reactTag) {
        ThreadUtils.runOnExecutor(() -> {
            PeerConnectionObserver pco = mPeerConnectionObservers.get(peerConnectionId);
            if (pco == null || pco.getPeerConnection() == null) {
                Log.d(TAG, "dataChannelDispose() peerConnection is null");
                return;
            }

            pco.dataChannelDispose(reactTag);
        });
    }

    @ReactMethod
    public void dataChannelSend(int peerConnectionId,
                                String reactTag,
                                String data,
                                String type) {
        ThreadUtils.runOnExecutor(() -> {
            // Forward to PeerConnectionObserver which deals with DataChannels
            // because DataChannel is owned by PeerConnection.
            PeerConnectionObserver pco = mPeerConnectionObservers.get(peerConnectionId);
            if (pco == null || pco.getPeerConnection() == null) {
                Log.d(TAG, "dataChannelSend() peerConnection is null");
                return;
            }

            pco.dataChannelSend(reactTag, data, type);
        });
    }

    @ReactMethod
    public void addListener(String eventName) {
        // Keep: Required for RN built in Event Emitter Calls.
    }

    @ReactMethod
    public void removeListeners(Integer count) {
        // Keep: Required for RN built in Event Emitter Calls.
    }
}
