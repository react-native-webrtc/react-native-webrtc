package com.oney.WebRTCModule;

import android.app.Application;

import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import android.util.Base64;
import android.util.SparseArray;
import android.hardware.Camera;
import android.content.Context;
import android.app.Activity;

import android.opengl.EGLContext;
import android.util.Log;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;

import org.webrtc.*;

public class WebRTCModule extends ReactContextBaseJavaModule {
    final static String TAG = WebRTCModule.class.getCanonicalName();

    private static final String LANGUAGE =  "language";

    private final PeerConnectionFactory mFactory;
    private final SparseArray<PeerConnectionObserver> mPeerConnectionObservers;
    public final Map<String, MediaStream> mMediaStreams;
    public final Map<String, MediaStreamTrack> mMediaStreamTracks;
    private final Map<String, VideoCapturer> mVideoCapturers;

    public WebRTCModule(ReactApplicationContext reactContext) {
        super(reactContext);

        mPeerConnectionObservers = new SparseArray<PeerConnectionObserver>();
        mMediaStreams = new HashMap<String, MediaStream>();
        mMediaStreamTracks = new HashMap<String, MediaStreamTrack>();
        mVideoCapturers = new HashMap<String, VideoCapturer>();

        PeerConnectionFactory.initializeAndroidGlobals(reactContext, true, true, true);
        mFactory = new PeerConnectionFactory();
    }

    @Override
    public String getName() {
        return "WebRTCModule";
    }

    private String getCurrentLanguage(){
        Locale current = getReactApplicationContext().getResources().getConfiguration().locale;
        return current.getLanguage();
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(LANGUAGE, getCurrentLanguage());
        return constants;
    }

    @ReactMethod
    public void getLanguage(Callback callback){
        String language = getCurrentLanguage();
        System.out.println("The current language is "+language);
        callback.invoke(null, language);
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

    private List<PeerConnection.IceServer> createIceServers(ReadableArray iceServersArray) {
        final int size = (iceServersArray == null) ? 0 : iceServersArray.size();
        List<PeerConnection.IceServer> iceServers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ReadableMap iceServerMap = iceServersArray.getMap(i);
            boolean hasUsernameAndCredential = iceServerMap.hasKey("username") && iceServerMap.hasKey("credential");
            if (iceServerMap.hasKey("url")) {
                if (hasUsernameAndCredential) {
                    iceServers.add(new PeerConnection.IceServer(iceServerMap.getString("url"), iceServerMap.getString("username"), iceServerMap.getString("credential")));
                } else {
                    iceServers.add(new PeerConnection.IceServer(iceServerMap.getString("url")));
                }
            } else if (iceServerMap.hasKey("urls")) {
                switch (iceServerMap.getType("urls")) {
                    case String:
                        if (hasUsernameAndCredential) {
                            iceServers.add(new PeerConnection.IceServer(iceServerMap.getString("urls"), iceServerMap.getString("username"), iceServerMap.getString("credential")));
                        } else {
                            iceServers.add(new PeerConnection.IceServer(iceServerMap.getString("urls")));
                        }
                        break;
                    case Array:
                        ReadableArray urls = iceServerMap.getArray("urls");
                        for (int j = 0; j < urls.size(); j++) {
                            String url = urls.getString(j);
                            if (hasUsernameAndCredential) {
                                iceServers.add(new PeerConnection.IceServer(url,iceServerMap.getString("username"), iceServerMap.getString("credential")));
                            } else {
                                iceServers.add(new PeerConnection.IceServer(url));
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
    public void peerConnectionInit(
            ReadableMap configuration,
            ReadableMap constraints,
            int id) {
        PeerConnectionObserver observer = new PeerConnectionObserver(this, id);
        PeerConnection peerConnection
            = mFactory.createPeerConnection(
                     parseRTCConfiguration(configuration),
                     parseMediaConstraints(constraints),
                     observer);

        observer.setPeerConnection(peerConnection);
        mPeerConnectionObservers.put(id, observer);
    }

    private String getNextStreamUUID() {
        String uuid;

        do {
            uuid = UUID.randomUUID().toString();
        } while (mMediaStreams.containsKey(uuid));

        return uuid;
    }

    private String getNextTrackUUID() {
        String uuid;

        do {
            uuid = UUID.randomUUID().toString();
        } while (mMediaStreamTracks.containsKey(uuid));

        return uuid;
    }

    /**
     * Includes default constraints set for the audio media type.
     * @param audioConstraints <tt>MediaConstraints</tt> instance to be filled
     * with the default constraints for audio media type.
     */
    private void addDefaultAudioConstraints(MediaConstraints audioConstraints) {
        audioConstraints.optional.add(
            new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        audioConstraints.optional.add(
            new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        audioConstraints.optional.add(
            new MediaConstraints.KeyValuePair("echoCancellation", "true"));
        audioConstraints.optional.add(
            new MediaConstraints.KeyValuePair("googEchoCancellation2", "true"));
        audioConstraints.optional.add(
            new MediaConstraints.KeyValuePair(
                    "googDAEchoCancellation", "true"));
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
     * Parses mandatory and optional "GUM" constraints described by a specific
     * <tt>ReadableMap</tt>.
     *
     * @param constraints A <tt>ReadableMap</tt> which represents a JavaScript
     * object specifying the constraints to be parsed into a
     * <tt>MediaConstraints</tt> instance.
     * @return A new <tt>MediaConstraints</tt> instance initialized with the
     * mandatory and optional constraint keys and values specified by
     * <tt>constraints</tt>.
     */
    private MediaConstraints parseMediaConstraints(ReadableMap constraints) {
        MediaConstraints mediaConstraints = new MediaConstraints();

        if (constraints.hasKey("mandatory")
                && constraints.getType("mandatory") == ReadableType.Map) {
            parseConstraints(
                    constraints.getMap("mandatory"),
                    mediaConstraints.mandatory);
        } else {
            Log.d(TAG, "mandatory constraints are not a map");
        }

        if (constraints.hasKey("optional")
                && constraints.getType("optional") == ReadableType.Array) {
            ReadableArray optional = constraints.getArray("optional");

            for (int i = 0, size = optional.size(); i < size; i++) {
                if (optional.getType(i) == ReadableType.Map) {
                    parseConstraints(
                            optional.getMap(i),
                            mediaConstraints.optional);
                }
            }
        } else {
            Log.d(TAG, "optional constraints are not an array");
        }

        return mediaConstraints;
    }

    /**
     * Retreives "sourceId" constraint value.
     * @param mediaConstraints a <tt>ReadableMap</tt> which represents "GUM"
     * constraints argument
     * @return String value of "sourceId" optional "GUM" constraint or
     * <tt>null</tt> if not specified in the given map.
     */
    private String getSourceIdConstraint(ReadableMap mediaConstraints) {
        if (mediaConstraints.hasKey("optional")
                && mediaConstraints.getType("optional") == ReadableType.Array) {
            ReadableArray optional = mediaConstraints.getArray("optional");

            for (int i = 0, size = optional.size(); i < size; i++) {
                if (optional.getType(i) == ReadableType.Map) {
                    ReadableMap option = optional.getMap(i);

                    if (option.hasKey("sourceId")
                            && option.getType("sourceId") == ReadableType.String) {
                        return option.getString("sourceId");
                    }
                }
            }
        }
        return null;
    }

    @ReactMethod
    public void getUserMedia(ReadableMap constraints,
                             Callback    successCallback,
                             Callback    errorCallback) {
        AudioTrack audioTrack = null;
        VideoTrack videoTrack = null;
        WritableArray tracks = Arguments.createArray();

        // NOTE: we don't need videoConstraints for now since createVideoSource doesn't accept
        //   videoConstraints, we should extract resolution and pass to startCapture

        // TODO: change getUserMedia constraints format to support new syntax 
        //   constraint format seems changed, and there is no mandatory any more.
        //   and has a new sytax/attrs to specify resolution
        //   should change `parseConstraints()` according
        //   see: https://www.w3.org/TR/mediacapture-streams/#idl-def-MediaTrackConstraints
        if (constraints.hasKey("video")) {
            ReadableType type = constraints.getType("video");
            VideoSource videoSource = null;
            //MediaConstraints videoConstraints = new MediaConstraints();
            ReadableMap videoConstraintsManatory = null;
            ReadableMap video = null;
            boolean enableVideo = true;
            String sourceId = null;
            String facingMode = null;
            String trackId = null;
            switch (type) {
            case Boolean:
                if (constraints.getBoolean("video")) {
                    // use default value for video resolution
                    WritableMap defaultVideoMandatory = new WritableNativeMap();
                    defaultVideoMandatory.putInt("minWidth", 1280);
                    defaultVideoMandatory.putInt("minHeight", 720);
                    defaultVideoMandatory.putInt("minFrameRate", 30);
                    videoConstraintsManatory = (ReadableMap) defaultVideoMandatory;
                } else {
                    enableVideo = false;
                }
                break;
            case Map:
                video = constraints.getMap("video");
                if (video.hasKey("mandatory") && 
                        video.getType("mandatory") == ReadableType.Map) {
                    videoConstraintsManatory = video.getMap("mandatory");
                }

                // video resolution is mandatory
                if (videoConstraintsManatory == null) {
                    errorCallback.invoke(null, "video mandatory constraints not found");
                    return;
                }
                
                //videoConstraints = parseConstraints(video);
                sourceId = getSourceIdConstraint(video);
                facingMode
                    = ReactBridgeUtil.getMapStrValue(video, "facingMode");
                break;
            default:
                errorCallback.invoke(null, "invalid type of video constraints");
                return;
            }

            if (enableVideo) {
                Log.i(TAG, "getUserMedia(video): video: " + video
                        + ", videoConstraintsManatory: " + videoConstraintsManatory
                        + ", sourceId: " + sourceId);

                Context context = (Context) getReactApplicationContext();
                final boolean isFacing = (facingMode != null && facingMode.equals("environment"))
                    ? false : true;
                VideoCapturer videoCapturer = null;

                // NOTE: to support Camera2, the device should:
                //   1. Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                //   2. all camera support level should greater than LEGACY
                //   see: https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics.html#INFO_SUPPORTED_HARDWARE_LEVEL
                if (Camera2Enumerator.isSupported(context)) {
                    Log.d(TAG, "Creating video capturer using Camera2 API.");
                    videoCapturer = createVideoCapturer(
                        new Camera2Enumerator(context), isFacing, sourceId);
                } else {
                    Log.d(TAG, "Creating video capturer using Camera1 API.");
                    final boolean captureToTexture = false;
                    videoCapturer = createVideoCapturer(
                        new Camera1Enumerator(captureToTexture), isFacing, sourceId);
                }

                if (videoCapturer != null) {

                    final int videoWidth = videoConstraintsManatory.getInt("minWidth");
                    final int videoHeight = videoConstraintsManatory.getInt("minHeight");
                    final int videoFps = videoConstraintsManatory.getInt("minFrameRate");

                    videoSource = mFactory.createVideoSource(videoCapturer);
                    videoCapturer.startCapture(videoWidth, videoHeight, videoFps);

                    trackId = getNextTrackUUID();

                    mVideoCapturers.put(trackId, videoCapturer);

                    if (videoSource != null) {
                        videoTrack = mFactory.createVideoTrack(trackId, videoSource);
                        if (videoTrack != null) {
                            mMediaStreamTracks.put(trackId, videoTrack);

                            WritableMap trackInfo = Arguments.createMap();
                            trackInfo.putString("id", trackId);
                            trackInfo.putString("label", "Video");
                            trackInfo.putString("kind", videoTrack.kind());
                            trackInfo.putBoolean("enabled", videoTrack.enabled());
                            trackInfo.putString(
                                "readyState", videoTrack.state().toString());
                            trackInfo.putBoolean("remote", false);
                            tracks.pushMap(trackInfo);
                        }
                    }
                }

                // return error if videoTrack did not create successfully
                if (videoTrack == null) {
                    // FIXME The following does not follow the getUserMedia()
                    // algorithm specified by
                    // https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-getusermedia
                    // with respect to distinguishing the various causes of failure.
                    if (videoCapturer != null) {
                        removeVideoCapturer(trackId);
                    }
                    errorCallback.invoke(/* type */ null, "Failed to obtain video");
                    return;
                }
            }
        }

        if (constraints.hasKey("audio")) {
            MediaConstraints audioConstraints = new MediaConstraints();
            ReadableType type = constraints.getType("audio");
            switch (type) {
                case Boolean:
                    if (constraints.getBoolean("audio")) {
                        addDefaultAudioConstraints(audioConstraints);
                    } else {
                        audioConstraints = null;
                    }
                    break;
                case Map:
                    audioConstraints
                        = parseMediaConstraints(constraints.getMap("audio"));
                    break;
                default:
                    audioConstraints = null;
                    break;
            }

            if (audioConstraints != null) {
                Log.i(TAG, "getUserMedia(audio): " + audioConstraints);

                AudioSource audioSource
                    = mFactory.createAudioSource(audioConstraints);

                if (audioSource != null) {
                    String trackId = getNextTrackUUID();
                    audioTrack
                        = mFactory.createAudioTrack(trackId, audioSource);
                    if (audioTrack != null) {
                        mMediaStreamTracks.put(trackId, audioTrack);

                        WritableMap trackInfo = Arguments.createMap();
                        trackInfo.putString("id", trackId);
                        trackInfo.putString("label", "Audio");
                        trackInfo.putString("kind", audioTrack.kind());
                        trackInfo.putBoolean("enabled", audioTrack.enabled());
                        trackInfo.putString("readyState",
                            audioTrack.state().toString());
                        trackInfo.putBoolean("remote", false);
                        tracks.pushMap(trackInfo);
                    }
                }
            }
            if (audioTrack == null && audioConstraints != null) {
                // FIXME The following does not follow the getUserMedia()
                // algorithm specified by
                // https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-getusermedia
                // with respect to distinguishing the various causes of failure.
                errorCallback.invoke(/* type */ null, "Failed to obtain audio");
                return;
            }
        }

        // According to step 2 of the getUserMedia() algorithm,
        // requestedMediaTypes is the set of media types in constraints with
        // either a dictionary value or a value of "true".
        // According to step 3 of the getUserMedia() algorithm, if
        // requestedMediaTypes is the empty set, the method invocation fails
        // with a TypeError.
        if (audioTrack == null && videoTrack == null) {
            // XXX The JavaScript counterpart of the getUserMedia()
            // implementation should have recognized the case here before
            // calling into the native counterpart and should have failed the
            // method invocation already (in the manner described above).
            // Anyway, repeat the logic here just in case.
            errorCallback.invoke(
                "TypeError",
                "constraints requests no media types");
            return;
        }

        String streamId = getNextStreamUUID();
        MediaStream mediaStream = mFactory.createLocalMediaStream(streamId);
        if (mediaStream == null) {
            // FIXME The following does not follow the getUserMedia() algorithm
            // specified by
            // https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-getusermedia
            // with respect to distinguishing the various causes of failure.
            errorCallback.invoke(
                /* type */ null,
                "Failed to create new media stream");
            return;
        }

        if (audioTrack != null)
            mediaStream.addTrack(audioTrack);
        if (videoTrack != null)
            mediaStream.addTrack(videoTrack);

        Log.d(TAG, "mMediaStreamId: " + streamId);
        mMediaStreams.put(streamId, mediaStream);

        successCallback.invoke(streamId, tracks);
    }
    @ReactMethod
    public void mediaStreamTrackGetSources(Callback callback){
        WritableArray array = Arguments.createArray();
        String[] names = new String[Camera.getNumberOfCameras()];

        for(int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            WritableMap info = getCameraInfo(i);
            if (info != null) {
                array.pushMap(info);
            }
        }

        WritableMap audio = Arguments.createMap();
        audio.putString("label", "Audio");
        audio.putString("id", "audio-1");
        audio.putString("facing", "");
        audio.putString("kind", "audio");

        array.pushMap(audio);
        callback.invoke(array);
    }

    @ReactMethod
    public void mediaStreamTrackStop(final String id) {
        // Is this functionality equivalent to `mediaStreamTrackRelease()` ?
        // if so, we should merge this two and remove track from stream as well.
        MediaStreamTrack track = mMediaStreamTracks.get(id);
        if (track == null) {
            Log.d(TAG, "mediaStreamTrackStop() track is null");
            return;
        }
        track.setEnabled(false);
        if (track.kind().equals("video")) {
            removeVideoCapturer(id);
        }
        mMediaStreamTracks.remove(id);
        // What exactly does `detached` mean in doc?
        // see: https://www.w3.org/TR/mediacapture-streams/#track-detached
    }

    @ReactMethod
    public void mediaStreamTrackSetEnabled(final String id, final boolean enabled) {
        MediaStreamTrack track = mMediaStreamTracks.get(id);
        if (track == null) {
            Log.d(TAG, "mediaStreamTrackSetEnabled() track is null");
            return;
        } else if (track.enabled() == enabled) {
            return;
        }
        track.setEnabled(enabled);
    }

    @ReactMethod
    public void mediaStreamTrackSwitchCamera(final String id) {
        MediaStreamTrack track = mMediaStreamTracks.get(id);
        if (track != null) {
            VideoCapturer videoCapturer = mVideoCapturers.get(id);
            if (videoCapturer != null) {
                CameraVideoCapturer cameraVideoCapturer
                    = (CameraVideoCapturer) videoCapturer;
                cameraVideoCapturer.switchCamera(null);
            }
        }
    }

    @ReactMethod
    public void mediaStreamTrackRelease(final String streamId, final String _trackId) {
        MediaStream stream = mMediaStreams.get(streamId);
        if (stream == null) {
            Log.d(TAG, "mediaStreamTrackRelease() stream is null");
            return;
        }
        MediaStreamTrack track = mMediaStreamTracks.get(_trackId);
        if (track == null) {
            Log.d(TAG, "mediaStreamTrackRelease() track is null");
            return;
        }
        track.setEnabled(false); // should we do this?
        mMediaStreamTracks.remove(_trackId);
        if (track.kind().equals("audio")) {
            stream.removeTrack((AudioTrack)track);
        } else if (track.kind().equals("video")) {
            stream.removeTrack((VideoTrack)track);
            removeVideoCapturer(_trackId);
        }
    }

    public WritableMap getCameraInfo(int index) {
        CameraInfo info = new CameraInfo();

        try {
            Camera.getCameraInfo(index, info);
        } catch (Exception e) {
            Logging.e("CameraEnumerationAndroid", "getCameraInfo failed on index " + index, e);
            return null;
        }
        WritableMap params = Arguments.createMap();
        String facing = info.facing == 1 ? "front" : "back";
        params.putString("label", "Camera " + index + ", Facing " + facing + ", Orientation " + info.orientation);
        params.putString("id", "" + index);
        params.putString("facing", facing);
        params.putString("kind", "video");

        return params;
    }

    /**
     * Create video capturer via given facing mode
     * @param enumerator a <tt>CameraEnumerator</tt> provided by webrtc
     *        it can be Camera1Enumerator or Camera2Enumerator
     * @param isFacing 'user' mapped with 'front' is true (default)
     *                 'environment' mapped with 'back' is false
     * @param sourceId (String) use this sourceId and ignore facing mode if specified.
     * @return VideoCapturer can invoke with <tt>startCapture</tt>/<tt>stopCapture</tt>
     *         <tt>null</tt> if not matched camera with specified facing mode.
     */
    private VideoCapturer createVideoCapturer(CameraEnumerator enumerator, boolean isFacing,
            String sourceId) {
        VideoCapturer videoCapturer = null;

        // if sourceId given, use specified sourceId first
        final String[] deviceNames = enumerator.getDeviceNames();
        if (sourceId != null) {
            for (String name : deviceNames) {
                if (name.equals(sourceId)) {
                    videoCapturer = enumerator.createCapturer(name, new CameraEventsHandler());
                    if (videoCapturer != null) {
                        Log.d(TAG, "create user specified camera " + name + " succeeded");
                        return videoCapturer;
                    } else {
                        Log.d(TAG, "create user specified camera " + name + " failed");
                        break; // fallback to facing mode
                    }
                }
            }
        }

        // otherwise, use facing mode
        String facingStr = isFacing ? "front" : "back";
        for (String name : deviceNames) {
            if (enumerator.isFrontFacing(name) == isFacing) {
                videoCapturer = enumerator.createCapturer(name, new CameraEventsHandler());
                if (videoCapturer != null) {
                    Log.d(TAG, "Create " + facingStr + " camera " + name + " succeeded");
                    return videoCapturer;
                } else {
                    Log.d(TAG, "Create " + facingStr + " camera " + name + " failed");
                }
            }
        }

        // should we fallback to available camera automatically?
        return null;
    }

    private MediaConstraints defaultConstraints() {
        MediaConstraints constraints = new MediaConstraints();
        // TODO video media
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        return constraints;
    }

    private void removeVideoCapturer(String id) {
        VideoCapturer videoCapturer = mVideoCapturers.get(id);
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                Log.e(TAG, "removeVideoCapturer() Failed to stop video capturer");
            }
            mVideoCapturers.remove(id);
        }
    }

    @ReactMethod
    public void peerConnectionSetConfiguration(ReadableMap configuration, final int id) {
        PeerConnection peerConnection = getPeerConnection(id);
        if (peerConnection == null) {
            Log.d(TAG, "peerConnectionSetConfiguration() peerConnection is null");
            return;
        }
        peerConnection.setConfiguration(parseRTCConfiguration(configuration));
    }

    String onAddStream(MediaStream mediaStream) {
        String id = mediaStream.label();
        String reactTag = null;
        // The native WebRTC implementation has a special concept of a default
        // MediaStream instance with the label default that the implementation
        // reuses.
        if ("default".equals(id)) {
            for (Map.Entry<String, MediaStream> e : mMediaStreams.entrySet()) {
                if (e.getValue().equals(mediaStream)) {
                    reactTag = e.getKey();
                    break;
                }
            }
        }
        if (reactTag == null) {
            reactTag = getNextStreamUUID();
        }
        if (!mMediaStreams.containsKey(reactTag)) {
            mMediaStreams.put(reactTag, mediaStream);
        }
        return reactTag;
    }

    @ReactMethod
    public void peerConnectionAddStream(final String streamId, final int id){
        MediaStream mediaStream = mMediaStreams.get(streamId);
        if (mediaStream == null) {
            Log.d(TAG, "peerConnectionAddStream() mediaStream is null");
            return;
        }
        PeerConnection peerConnection = getPeerConnection(id);
        if (peerConnection != null) {
            boolean result = peerConnection.addStream(mediaStream);
            Log.d(TAG, "addStream" + result);
        } else {
            Log.d(TAG, "peerConnectionAddStream() peerConnection is null");
        }
    }

    String onRemoveStream(MediaStream mediaStream) {
        if (mediaStream == null) {
            return null;
        }
        for (VideoTrack track : mediaStream.videoTracks) {
            mMediaStreamTracks.remove(track.id());
            removeVideoCapturer(track.id());
        }
        for (AudioTrack track : mediaStream.audioTracks) {
            mMediaStreamTracks.remove(track.id());
        }
        String reactTag = null;
        for (Iterator<Map.Entry<String, MediaStream>> i
                    = mMediaStreams.entrySet().iterator();
                i.hasNext();) {
            Map.Entry<String, MediaStream> e = i.next();
            if (e.getValue().equals(mediaStream)) {
                reactTag = e.getKey();
                i.remove();
                break;
            }
        }
        return reactTag;
    }

    @ReactMethod
    public void peerConnectionRemoveStream(final String streamId, final int id){
        MediaStream mediaStream = mMediaStreams.get(streamId);
        if (mediaStream == null) {
            Log.d(TAG, "peerConnectionRemoveStream() mediaStream is null");
            return;
        }
        PeerConnection peerConnection = getPeerConnection(id);
        if (peerConnection != null) {
            peerConnection.removeStream(mediaStream);
        } else {
            Log.d(TAG, "peerConnectionRemoveStream() peerConnection is null");
        }
    }

    @ReactMethod
    public void peerConnectionCreateOffer(
            int id,
            ReadableMap constraints,
            final Callback callback) {
        PeerConnection peerConnection = getPeerConnection(id);

        if (peerConnection != null) {
            peerConnection.createOffer(new SdpObserver() {
                @Override
                public void onCreateFailure(String s) {
                    callback.invoke(false, s);
                }

                @Override
                public void onCreateSuccess(final SessionDescription sdp) {
                    WritableMap params = Arguments.createMap();
                    params.putString("sdp", sdp.description);
                    params.putString("type", sdp.type.canonicalForm());
                    callback.invoke(true, params);
                }

                @Override
                public void onSetFailure(String s) {}

                @Override
                public void onSetSuccess() {}
            }, parseMediaConstraints(constraints));
        } else {
            Log.d(TAG, "peerConnectionCreateOffer() peerConnection is null");
            callback.invoke(false, "peerConnection is null");
        }
    }

    @ReactMethod
    public void peerConnectionCreateAnswer(
            int id,
            ReadableMap constraints,
            final Callback callback) {
        PeerConnection peerConnection = getPeerConnection(id);

        if (peerConnection != null) {
            peerConnection.createAnswer(new SdpObserver() {
                @Override
                public void onCreateFailure(String s) {
                    callback.invoke(false, s);
                }

                @Override
                public void onCreateSuccess(final SessionDescription sdp) {
                    WritableMap params = Arguments.createMap();
                    params.putString("sdp", sdp.description);
                    params.putString("type", sdp.type.canonicalForm());
                    callback.invoke(true, params);
                }

                @Override
                public void onSetFailure(String s) {}

                @Override
                public void onSetSuccess() {}
            }, parseMediaConstraints(constraints));
        } else {
            Log.d(TAG, "peerConnectionCreateAnswer() peerConnection is null");
            callback.invoke(false, "peerConnection is null");
        }
    }

    @ReactMethod
    public void peerConnectionSetLocalDescription(ReadableMap sdpMap, final int id, final Callback callback) {
        PeerConnection peerConnection = getPeerConnection(id);

        Log.d(TAG, "peerConnectionSetLocalDescription() start");
        if (peerConnection != null) {
            SessionDescription sdp = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(sdpMap.getString("type")),
                sdpMap.getString("sdp")
            );

            peerConnection.setLocalDescription(new SdpObserver() {
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
            Log.d(TAG, "peerConnectionSetLocalDescription() peerConnection is null");
            callback.invoke(false, "peerConnection is null");
        }
        Log.d(TAG, "peerConnectionSetLocalDescription() end");
    }
    @ReactMethod
    public void peerConnectionSetRemoteDescription(final ReadableMap sdpMap, final int id, final Callback callback) {
        PeerConnection peerConnection = getPeerConnection(id);
        // final String d = sdpMap.getString("type");

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
    public void peerConnectionAddICECandidate(ReadableMap candidateMap, final int id, final Callback callback) {
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
        PeerConnectionObserver pco = mPeerConnectionObservers.get(id);
        if (pco == null || pco.getPeerConnection() == null) {
            Log.d(TAG, "peerConnectionGetStats() peerConnection is null");
        } else {
            pco.getStats(trackId, cb);
        }
    }

    @ReactMethod
    public void peerConnectionClose(final int id) {
        PeerConnectionObserver pco = mPeerConnectionObservers.get(id);
        if (pco == null || pco.getPeerConnection() == null) {
            Log.d(TAG, "peerConnectionClose() peerConnection is null");
        } else {
            pco.close();
            mPeerConnectionObservers.remove(id);
        }
    }

    @ReactMethod
    public void mediaStreamRelease(final String id) {
        MediaStream mediaStream = mMediaStreams.get(id);
        if (mediaStream != null) {
            for (VideoTrack track : mediaStream.videoTracks) {
                mMediaStreamTracks.remove(track);
                removeVideoCapturer(track.id());
            }
            for (AudioTrack track : mediaStream.audioTracks) {
                mMediaStreamTracks.remove(track);
            }

            mMediaStreams.remove(id);
        } else {
            Log.d(TAG, "mediaStreamRelease() mediaStream is null");
        }
    }

    @ReactMethod
    public void createDataChannel(final int peerConnectionId, String label, ReadableMap config) {
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
    public void dataChannelSend(int peerConnectionId, int dataChannelId, String data, String type) {
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

    @ReactMethod
    public void dataChannelClose(int peerConnectionId, int dataChannelId) {
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
}
