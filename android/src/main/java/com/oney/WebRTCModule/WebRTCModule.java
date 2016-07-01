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
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.net.URISyntaxException;
import java.util.LinkedList;

import android.util.Base64;
import android.util.SparseArray;
import android.hardware.Camera;
import android.media.AudioManager;
import android.content.Context;
import android.view.Window;
import android.view.WindowManager;
import android.app.Activity;
import android.os.PowerManager;

import android.opengl.EGLContext;
import android.util.Log;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;

import org.webrtc.*;

public class WebRTCModule extends ReactContextBaseJavaModule {
    private final static String TAG = WebRTCModule.class.getCanonicalName();

    private static final String LANGUAGE =  "language";
    private PeerConnectionFactory mFactory;
    private int mMediaStreamId = 0;
    private int mMediaStreamTrackId = 0;
    private final SparseArray<PeerConnection> mPeerConnections;
    public final SparseArray<MediaStream> mMediaStreams;
    public final SparseArray<MediaStreamTrack> mMediaStreamTracks;
    private final SparseArray<DataChannel> mDataChannels;
    private MediaConstraints pcConstraints = new MediaConstraints();
    VideoSource videoSource;

    public WebRTCModule(ReactApplicationContext reactContext) {
        super(reactContext);

        mPeerConnections = new SparseArray<PeerConnection>();
        mMediaStreams = new SparseArray<MediaStream>();
        mMediaStreamTracks = new SparseArray<MediaStreamTrack>();
        mDataChannels = new SparseArray<DataChannel>();

        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

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
    private void sendEvent(String eventName, @Nullable WritableMap params) {
        getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    private List<PeerConnection.IceServer> createIceServers(ReadableArray iceServersArray) {
        LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
        for (int i = 0; i < iceServersArray.size(); i++) {
            ReadableMap iceServerMap = iceServersArray.getMap(i);
            boolean hasUsernameAndCredential = iceServerMap.hasKey("username") && iceServerMap.hasKey("credential");
            if (iceServerMap.hasKey("url")) {
                if (hasUsernameAndCredential) {
                    iceServers.add(new PeerConnection.IceServer(iceServerMap.getString("url"),iceServerMap.getString("username"), iceServerMap.getString("credential")));
                } else {
                    iceServers.add(new PeerConnection.IceServer(iceServerMap.getString("url")));
                }
            } else if (iceServerMap.hasKey("urls")) {
                switch (iceServerMap.getType("urls")) {
                    case String:
                        if (hasUsernameAndCredential) {
                            iceServers.add(new PeerConnection.IceServer(iceServerMap.getString("urls"),iceServerMap.getString("username"), iceServerMap.getString("credential")));
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

    @ReactMethod
    public void peerConnectionInit(ReadableMap configuration, final int id){
        List<PeerConnection.IceServer> iceServers = createIceServers(configuration.getArray("iceServers"));

        PeerConnection peerConnection = mFactory.createPeerConnection(iceServers, pcConstraints, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                WritableMap params = Arguments.createMap();
                params.putInt("id", id);
                params.putString("signalingState", signalingStateString(signalingState));
                sendEvent("peerConnectionSignalingStateChanged", params);
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

                WritableMap params = Arguments.createMap();
                params.putInt("id", id);
                params.putString("iceConnectionState", iceConnectionStateString(iceConnectionState));

                sendEvent("peerConnectionIceConnectionChanged", params);
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(TAG, "onIceGatheringChangedwe" + iceGatheringState.name());
                WritableMap params = Arguments.createMap();
                params.putInt("id", id);
                params.putString("iceGatheringState", iceGatheringStateString(iceGatheringState));
                sendEvent("peerConnectionIceGatheringChanged", params);
            }

            @Override
            public void onIceCandidate(final IceCandidate candidate) {
                Log.d(TAG, "onIceCandidatewqerqwrsdfsd");
                WritableMap params = Arguments.createMap();
                params.putInt("id", id);
                WritableMap candidateParams = Arguments.createMap();
                candidateParams.putInt("sdpMLineIndex", candidate.sdpMLineIndex);
                candidateParams.putString("sdpMid", candidate.sdpMid);
                candidateParams.putString("candidate", candidate.sdp);
                params.putMap("candidate", candidateParams);

                sendEvent("peerConnectionGotICECandidate", params);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                mMediaStreamId++;
                mMediaStreams.put(mMediaStreamId, mediaStream);
                WritableMap params = Arguments.createMap();
                params.putInt("id", id);
                params.putInt("streamId", mMediaStreamId);

                WritableArray tracks = Arguments.createArray();

                for (int i = 0; i < mediaStream.videoTracks.size(); i++) {
                    VideoTrack track = mediaStream.videoTracks.get(i);

                    int mediaStreamTrackId = mMediaStreamTrackId++;
                    WritableMap trackInfo = Arguments.createMap();
                    trackInfo.putString("id", mediaStreamTrackId + "");
                    trackInfo.putString("label", "Video");
                    trackInfo.putString("kind", track.kind());
                    trackInfo.putBoolean("enabled", track.enabled());
                    trackInfo.putString("readyState", track.state().toString());
                    trackInfo.putBoolean("remote", true);
                    tracks.pushMap(trackInfo);
                }
                for (int i = 0; i < mediaStream.audioTracks.size(); i++) {
                    AudioTrack track = mediaStream.audioTracks.get(i);

                    int mediaStreamTrackId = mMediaStreamTrackId++;
                    WritableMap trackInfo = Arguments.createMap();
                    trackInfo.putString("id", mediaStreamTrackId + "");
                    trackInfo.putString("label", "Audio");
                    trackInfo.putString("kind", track.kind());
                    trackInfo.putBoolean("enabled", track.enabled());
                    trackInfo.putString("readyState", track.state().toString());
                    trackInfo.putBoolean("remote", true);
                    tracks.pushMap(trackInfo);
                }
                params.putArray("tracks", tracks);

                sendEvent("peerConnectionAddedStream", params);
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                if (mediaStream != null) {
                    int trackIndex;
                    int streamIndex;
                    for (int i = 0; i < mediaStream.videoTracks.size(); i++) {
                        VideoTrack track = mediaStream.videoTracks.get(i);
                        trackIndex = mMediaStreamTracks.indexOfValue(track);
                        while (trackIndex >= 0) {
                            mMediaStreamTracks.removeAt(trackIndex);
                            trackIndex = mMediaStreamTracks.indexOfValue(track);
                        }
                    }
                    for (int i = 0; i < mediaStream.audioTracks.size(); i++) {
                        AudioTrack track = mediaStream.audioTracks.get(i);
                        trackIndex = mMediaStreamTracks.indexOfValue(track);
                        while (trackIndex >= 0) {
                            mMediaStreamTracks.removeAt(trackIndex);
                            trackIndex = mMediaStreamTracks.indexOfValue(track);
                        }
                    }
                    streamIndex = mMediaStreams.indexOfValue(mediaStream);
                    if (streamIndex >= 0) {
                        mMediaStreams.removeAt(streamIndex);
                    }
                }
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                // XXX Unfortunately, the Java WebRTC API doesn't expose the id
                // of the underlying C++/native DataChannel (even though the
                // WebRTC standard defines the DataChannel.id property). As a
                // workaround, generated an id which will surely not clash with
                // the ids of the remotely-opened (and standard-compliant
                // locally-opened) DataChannels.
                int dataChannelId = -1;
                // The RTCDataChannel.id space is limited to unsigned short by
                // the standard:
                // https://www.w3.org/TR/webrtc/#dom-datachannel-id.
                // Additionally, 65535 is reserved due to SCTP INIT and
                // INIT-ACK chunks only allowing a maximum of 65535 streams to
                // be negotiated (as defined by the WebRTC Data Channel
                // Establishment Protocol).
                for (int i = 65536; i <= Integer.MAX_VALUE; ++i) {
                    if (null == mDataChannels.get(i, null)) {
                        dataChannelId = i;
                        break;
                    }
                }
                if (-1 == dataChannelId) {
                  return;
                }

                dataChannel.registerObserver(new DataChannelObserver(dataChannelId, dataChannel));

                WritableMap dataChannelParams = Arguments.createMap();
                dataChannelParams.putInt("id", dataChannelId);
                dataChannelParams.putString("label", dataChannel.label());
                WritableMap params = Arguments.createMap();
                params.putInt("id", id);
                params.putMap("dataChannel", dataChannelParams);

                mDataChannels.put(dataChannelId, dataChannel);

                sendEvent("peerConnectionDidOpenDataChannel", params);
            }

            @Override
            public void onRenegotiationNeeded() {
                WritableMap params = Arguments.createMap();
                params.putInt("id", id);
                sendEvent("peerConnectionOnRenegotiationNeeded", params);
            }

            @Override
            public void onIceConnectionReceivingChange(boolean var1) {

            }
        });
        mPeerConnections.put(id, peerConnection);
    }
    @ReactMethod
    public void getUserMedia(ReadableMap constraints, Callback callback){
        MediaStream mediaStream = mFactory.createLocalMediaStream("ARDAMS");

        WritableArray tracks = Arguments.createArray();
        if (constraints.hasKey("video")) {
            ReadableType type = constraints.getType("video");
            VideoSource videoSource = null;
            MediaConstraints videoConstraints = new MediaConstraints();
            switch (type) {
                case Boolean:
                    boolean useVideo = constraints.getBoolean("video");
                    if (useVideo) {
                        String name = CameraEnumerationAndroid.getNameOfFrontFacingDevice();

                        VideoCapturerAndroid v = VideoCapturerAndroid.create(name, new VideoCapturerAndroid.CameraErrorHandler() {
                            @Override
                            public void onCameraError(String s) {

                            }
                        });
                        videoSource = mFactory.createVideoSource(v, videoConstraints);
                    }
                    break;
                case Map:
                    ReadableMap useVideoMap = constraints.getMap("video");
                    if (useVideoMap.hasKey("optional")) {
                        if (useVideoMap.getType("optional") == ReadableType.Array) {
                            ReadableArray options = useVideoMap.getArray("optional");
                            for (int i = 0; i < options.size(); i++) {
                                if (options.getType(i) == ReadableType.Map) {
                                    ReadableMap option = options.getMap(i);
                                    if (option.hasKey("sourceId") && option.getType("sourceId") == ReadableType.String) {
                                        videoSource = mFactory.createVideoSource(getVideoCapturerById(Integer.parseInt(option.getString("sourceId"))), videoConstraints);
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
            // videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(100)));
            // videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(100)));
            // videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(10)));
            // videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(10)));

            if (videoSource != null) {
                VideoTrack videoTrack = mFactory.createVideoTrack("ARDAMSv0", videoSource);

                int mediaStreamTrackId = mMediaStreamTrackId++;
                mMediaStreamTracks.put(mediaStreamTrackId, videoTrack);

                WritableMap trackInfo = Arguments.createMap();
                trackInfo.putString("id", mediaStreamTrackId + "");
                trackInfo.putString("label", "Video");
                trackInfo.putString("kind", videoTrack.kind());
                trackInfo.putBoolean("enabled", videoTrack.enabled());
                trackInfo.putString("readyState", videoTrack.state().toString());
                trackInfo.putBoolean("remote", false);
                tracks.pushMap(trackInfo);

                mediaStream.addTrack(videoTrack);
            }
        }
        boolean useAudio = constraints.getBoolean("audio");
        if (useAudio) {
            MediaConstraints audioConstarints = new MediaConstraints();
            audioConstarints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
            audioConstarints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
            audioConstarints.mandatory.add(new MediaConstraints.KeyValuePair("echoCancellation", "true"));
            audioConstarints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation2", "true"));
            audioConstarints.mandatory.add(new MediaConstraints.KeyValuePair("googDAEchoCancellation", "true"));

            AudioSource audioSource = mFactory.createAudioSource(audioConstarints);

            AudioTrack audioTrack = mFactory.createAudioTrack("ARDAMSa0", audioSource);

            int mediaStreamTrackId = mMediaStreamTrackId++;
            mMediaStreamTracks.put(mediaStreamTrackId, audioTrack);

            WritableMap trackInfo = Arguments.createMap();
            trackInfo.putString("id", mediaStreamTrackId + "");
            trackInfo.putString("label", "Audio");
            trackInfo.putString("kind", audioTrack.kind());
            trackInfo.putBoolean("enabled", audioTrack.enabled());
            trackInfo.putString("readyState", audioTrack.state().toString());
            trackInfo.putBoolean("remote", false);
            tracks.pushMap(trackInfo);

            mediaStream.addTrack(audioTrack);
        }

        Log.d(TAG, "mMediaStreamId: " + mMediaStreamId);
        mMediaStreamId++;
        mMediaStreams.put(mMediaStreamId, mediaStream);

        callback.invoke(mMediaStreamId, tracks);
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
        final int trackId = Integer.parseInt(id);
        MediaStreamTrack track = mMediaStreamTracks.get(trackId, null);
        if (track == null) {
            Log.d(TAG, "mediaStreamTrackStop() track is null");
            return;
        }
        track.setEnabled(false);
        track.setState(MediaStreamTrack.State.ENDED);
        mMediaStreamTracks.remove(trackId);
        // what exaclty `detached` means in doc?
        // see: https://www.w3.org/TR/mediacapture-streams/#track-detached
    }

    @ReactMethod
    public void mediaStreamTrackSetEnabled(final String id, final boolean enabled) {
        final int trackId = Integer.parseInt(id);
        MediaStreamTrack track = mMediaStreamTracks.get(trackId, null);
        if (track == null) {
            Log.d(TAG, "mediaStreamTrackSetEnabled() track is null");
            return;
        } else if (track.enabled() == enabled) {
            return;
        }
        track.setEnabled(enabled);
    }

    @ReactMethod
    public void mediaStreamTrackRelease(final int streamId, final String _trackId) {
        // TODO: need to normalize streamId as a string ( spec wanted )
        //final int streamId = Integer.parseInt(_streamId);
        final int trackId = Integer.parseInt(_trackId);
        MediaStream stream = mMediaStreams.get(streamId, null);
        if (stream == null) {
            Log.d(TAG, "mediaStreamTrackRelease() stream is null");
            return;
        }
        MediaStreamTrack track = mMediaStreamTracks.get(trackId, null);
        if (track == null) {
            Log.d(TAG, "mediaStreamTrackRelease() track is null");
            return;
        }
        track.setEnabled(false); // should we do this?
        track.setState(MediaStreamTrack.State.ENDED); // should we do this?
        mMediaStreamTracks.remove(trackId);
        if (track.kind().equals("audio")) {
            stream.removeTrack((AudioTrack)track);
        } else if (track.kind().equals("video")) {
            stream.removeTrack((VideoTrack)track);
        }
    }

    public WritableMap getCameraInfo(int index) {
        CameraInfo info = new CameraInfo();

        try {
            Camera.getCameraInfo(index, info);
        } catch (Exception var3) {
            Logging.e("CameraEnumerationAndroid", "getCameraInfo failed on index " + index, var3);
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

    private VideoCapturer getVideoCapturerById(int id) {
        String name = CameraEnumerationAndroid.getDeviceName(id);
        if (name == null) {
            name = CameraEnumerationAndroid.getNameOfFrontFacingDevice();
        }

        return VideoCapturerAndroid.create(name, new VideoCapturerAndroid.CameraErrorHandler() {
            @Override
            public void onCameraError(String s) {

            }
        });
    }
    private MediaConstraints defaultConstraints() {
        MediaConstraints constraints = new MediaConstraints();
        // TODO video media
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        return constraints;
    }
    @ReactMethod
    public void peerConnectionAddStream(final int streamId, final int id){
        MediaStream mediaStream = mMediaStreams.get(streamId, null);
        if (mediaStream == null) {
            Log.d(TAG, "peerConnectionAddStream() mediaStream is null");
            return;
        }
        PeerConnection peerConnection = mPeerConnections.get(id, null);
        if (peerConnection != null) {
            boolean result = peerConnection.addStream(mediaStream);
            Log.d(TAG, "addStream" + result);
        } else {
            Log.d(TAG, "peerConnectionAddStream() peerConnection is null");
        }
    }
    @ReactMethod
    public void peerConnectionRemoveStream(final int streamId, final int id){
        MediaStream mediaStream = mMediaStreams.get(streamId, null);
        if (mediaStream == null) {
            Log.d(TAG, "peerConnectionRemoveStream() mediaStream is null");
            return;
        }
        PeerConnection peerConnection = mPeerConnections.get(id, null);
        if (peerConnection != null) {
            peerConnection.removeStream(mediaStream);
        } else {
            Log.d(TAG, "peerConnectionRemoveStream() peerConnection is null");
        }
    }

    @ReactMethod
    public void peerConnectionCreateOffer(final int id, final Callback callback) {
        PeerConnection peerConnection = mPeerConnections.get(id, null);

        // MediaConstraints constraints = new MediaConstraints();
        // constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        // constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));

        Log.d(TAG, "RTCPeerConnectionCreateOfferWithObjectID start");
        if (peerConnection != null) {
            peerConnection.createOffer(new SdpObserver() {
                @Override
                public void onCreateSuccess(final SessionDescription sdp) {
                    WritableMap params = Arguments.createMap();
                    params.putString("type", sdp.type.canonicalForm());
                    params.putString("sdp", sdp.description);
                    callback.invoke(true, params);
                }
                @Override
                public void onSetSuccess() {}

                @Override
                public void onCreateFailure(String s) {
                    callback.invoke(false, s);
                }

                @Override
                public void onSetFailure(String s) {}
            }, pcConstraints);
        } else {
            Log.d(TAG, "peerConnectionCreateOffer() peerConnection is null");
            callback.invoke(false, "peerConnection is null");
        }
        Log.d(TAG, "RTCPeerConnectionCreateOfferWithObjectID end");
    }

    @ReactMethod
    public void peerConnectionCreateAnswer(final int id, final Callback callback) {
        PeerConnection peerConnection = mPeerConnections.get(id, null);

        // MediaConstraints constraints = new MediaConstraints();
        // constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        // constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));

        Log.d(TAG, "RTCPeerConnectionCreateAnswerWithObjectID start");
        if (peerConnection != null) {
            peerConnection.createAnswer(new SdpObserver() {
                @Override
                public void onCreateSuccess(final SessionDescription sdp) {
                    WritableMap params = Arguments.createMap();
                    params.putString("type", sdp.type.canonicalForm());
                    params.putString("sdp", sdp.description);
                    callback.invoke(true, params);
                }

                @Override
                public void onSetSuccess() {
                }

                @Override
                public void onCreateFailure(String s) {
                    callback.invoke(false, s);
                }

                @Override
                public void onSetFailure(String s) {
                }
            }, pcConstraints);
        } else {
            Log.d(TAG, "peerConnectionCreateAnswer() peerConnection is null");
            callback.invoke(false, "peerConnection is null");
        }
        Log.d(TAG, "RTCPeerConnectionCreateAnswerWithObjectID end");
    }

    @ReactMethod
    public void peerConnectionSetLocalDescription(ReadableMap sdpMap, final int id, final Callback callback) {
        PeerConnection peerConnection = mPeerConnections.get(id, null);

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
        PeerConnection peerConnection = mPeerConnections.get(id, null);
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
        PeerConnection peerConnection = mPeerConnections.get(id, null);
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
    public void peerConnectionClose(final int id) {
        PeerConnection peerConnection = mPeerConnections.get(id, null);
        if (peerConnection != null) {
            peerConnection.close();
            mPeerConnections.remove(id);
        } else {
            Log.d(TAG, "peerConnectionClose() peerConnection is null");
        }
        resetAudio();
    }
    @ReactMethod
    public void mediaStreamRelease(final int id) {
        MediaStream mediaStream = mMediaStreams.get(id, null);
        if (mediaStream != null) {
            int trackIndex;
            for (int i = 0; i < mediaStream.videoTracks.size(); i++) {
                VideoTrack track = mediaStream.videoTracks.get(i);
                trackIndex = mMediaStreamTracks.indexOfValue(track);
                while (trackIndex >= 0) {
                    mMediaStreamTracks.removeAt(trackIndex);
                    trackIndex = mMediaStreamTracks.indexOfValue(track);
                }
            }
            for (int i = 0; i < mediaStream.audioTracks.size(); i++) {
                AudioTrack track = mediaStream.audioTracks.get(i);
                trackIndex = mMediaStreamTracks.indexOfValue(track);
                while (trackIndex >= 0) {
                    mMediaStreamTracks.removeAt(trackIndex);
                    trackIndex = mMediaStreamTracks.indexOfValue(track);
                }
            }

            mMediaStreams.remove(id);
        } else {
            Log.d(TAG, "mediaStreamRelease() mediaStream is null");
        }
    }
    private void resetAudio() {
        AudioManager audioManager = (AudioManager)getReactApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(true);
        audioManager.setMode(AudioManager.MODE_NORMAL);
    }
    @ReactMethod
    public void setAudioOutput(String output) {
        AudioManager audioManager = (AudioManager)getReactApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        audioManager.setSpeakerphoneOn(output.equals("speaker"));
    }
    @ReactMethod
    public void setKeepScreenOn(final boolean isOn) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            public void run() {
                Window window = getCurrentActivity().getWindow();
                if (isOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });
    }

    @ReactMethod
    public void setProximityScreenOff(boolean enabled) {
        // TODO
        /*
        PowerManager powerManager = (PowerManager)getReactApplicationContext().getSystemService(Context.POWER_SERVICE);
        if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG);
            wakeLock.setReferenceCounted(false);
        } else {
        }*/
    }

    @ReactMethod
    public void createDataChannel(final int peerConnectionId, String label, ReadableMap config) {
        PeerConnection peerConnection = mPeerConnections.get(peerConnectionId, null);
        if (peerConnection != null) {
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
            // XXX RTP data channels are not defined by the WebRTC standard,
            // have been deprecated in Chromium, and Google have decided (in
            // 2015) to no longer support them (in the face of multiple
            // reported issues of breakages).
            int dataChannelId = init.id;
            if (-1 != dataChannelId) {
                dataChannel.registerObserver(new DataChannelObserver(dataChannelId, dataChannel));
                mDataChannels.put(dataChannelId, dataChannel);
            }
        } else {
            Log.d(TAG, "createDataChannel() peerConnection is null");
        }
    }

    @ReactMethod
    public void dataChannelSend(final int dataChannelId, String data, String type) {
        DataChannel dataChannel = mDataChannels.get(dataChannelId, null);
        if (dataChannel != null) {
            byte[] byteArray;
            if (type.equals("text")) {
                try {
                    byteArray = data.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.d(TAG, "Could not encode text string as UTF-8.");
                    return;
                }
            } else if (type.equals("binary")) {
                byteArray = Base64.decode(data, Base64.NO_WRAP);
            } else {
                Log.e(TAG, "Unsupported data type: " + type);
                return;
            }
            ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
            DataChannel.Buffer buffer = new DataChannel.Buffer(byteBuffer, type.equals("binary"));
            dataChannel.send(buffer);
        } else {
            Log.d(TAG, "dataChannelSend() dataChannel is null");
        }
    }

    @ReactMethod
    public void dataChannelClose(final int dataChannelId) {
        DataChannel dataChannel = mDataChannels.get(dataChannelId, null);
        if (dataChannel != null) {
            dataChannel.close();
            mDataChannels.remove(dataChannelId);
        } else {
            Log.d(TAG, "dataChannelClose() dataChannel is null");
        }
    }

    @Nullable
    public String iceConnectionStateString(PeerConnection.IceConnectionState iceConnectionState) {
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
    public String signalingStateString(PeerConnection.SignalingState signalingState) {
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

    @Nullable
    public String iceGatheringStateString(PeerConnection.IceGatheringState iceGatheringState) {
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

    class DataChannelObserver implements DataChannel.Observer {

        private final int mId;
        private final DataChannel mDataChannel;

        DataChannelObserver(int id, DataChannel dataChannel) {
            mId = id;
            mDataChannel = dataChannel;
        }

        @Override
        public void onBufferedAmountChange(long amount) {
        }

        @Override
        public void onStateChange() {
            WritableMap params = Arguments.createMap();
            params.putInt("id", mId);
            params.putString("state", dataChannelStateString(mDataChannel.state()));
            sendEvent("dataChannelStateChanged", params);
        }

        @Override
        public void onMessage(DataChannel.Buffer buffer) {
            WritableMap params = Arguments.createMap();
            params.putInt("id", mId);

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

            sendEvent("dataChannelReceiveMessage", params);
        }
    }
}
