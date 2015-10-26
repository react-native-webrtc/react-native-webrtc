package com.oney.WebRTCModule;

import android.app.Application;

import android.support.annotation.Nullable;

import java.io.IOException;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.net.URISyntaxException;
import java.util.LinkedList;
import android.util.SparseArray;
import android.hardware.Camera;

import android.opengl.EGLContext;
import android.util.Log;

import org.webrtc.*;

/**
 * Created by stefano on 20/09/15.
 */
public class WebRTCModule extends ReactContextBaseJavaModule {
    private final static String TAG = WebRTCModule.class.getCanonicalName();

    private static final String LANGUAGE =  "language";
    private PeerConnectionFactory mFactory;
    private ReactContext mReactContext;
    private int mMediaStreamId = 0;
    private final SparseArray<PeerConnection> mPeerConnections;
    public final SparseArray<MediaStream> mMediaStreams;
    private MediaConstraints pcConstraints = new MediaConstraints();

    public WebRTCModule(ReactApplicationContext reactContext) {
        super(reactContext);
        Log.d(TAG, "create ReactApplicationContext");
        mReactContext = reactContext;

        mPeerConnections = new SparseArray<PeerConnection>();
        mMediaStreams = new SparseArray<MediaStream>();

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
        mReactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    @ReactMethod
    public void peerConnectionInit(ReadableMap configuration, final int id){

        Log.d(TAG, "PeerConnectionInitfasf");
        LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
        ReadableArray iceServersArray = configuration.getArray("iceServers");
        for (int i = 0; i < iceServersArray.size(); i++) {
            ReadableMap iceServerMap = iceServersArray.getMap(i);
            iceServers.add(new PeerConnection.IceServer(iceServerMap.getString("url")));
        }

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
                sendEvent("peerConnectionIceGatheringChange", params);
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

              sendEvent("peerConnectionAddedStream", params);
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {

            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {}

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
        if (true) {
            MediaConstraints videoConstraints = new MediaConstraints();
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(100)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(100)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(30)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(30)));

            VideoSource videoSource = mFactory.createVideoSource(getVideoCapturer(), videoConstraints);
            mediaStream.addTrack(mFactory.createVideoTrack("ARDAMSv0", videoSource));
        }
        AudioSource audioSource = mFactory.createAudioSource(new MediaConstraints());
        mediaStream.addTrack(mFactory.createAudioTrack("ARDAMSa0", audioSource));

        Log.d(TAG, "mMediaStreamId: " + mMediaStreamId);
        mMediaStreamId++;
        mMediaStreams.put(mMediaStreamId, mediaStream);

        callback.invoke(mMediaStreamId);
    }
    private VideoCapturer getVideoCapturer() {
        return VideoCapturerAndroid.create(CameraEnumerationAndroid.getNameOfFrontFacingDevice(), new VideoCapturerAndroid.CameraErrorHandler() {
            @Override
            public void onCameraError(String s) {

            }
        });
        // String frontCameraDeviceName = VideoCapturerAndroid.getDeviceNames()[0];
        // return VideoCapturerAndroid.create(frontCameraDeviceName);
    }
    private String getDeviceNames() {
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            String existingDevice = CameraEnumerationAndroid.getDeviceName(i);
            if (existingDevice != null) {
                return existingDevice;
            }
        }
        return "";
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
        MediaStream mediaStream = mMediaStreams.get(streamId);
        PeerConnection peerConnection = mPeerConnections.get(id);
        boolean result = peerConnection.addStream(mediaStream);
        Log.d(TAG, "addStream" + result);
    }

    @ReactMethod
    public void peerConnectionCreateOffer(final int id, final Callback callback) {
        PeerConnection peerConnection = mPeerConnections.get(id);

        // MediaConstraints constraints = new MediaConstraints();
        // constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        // constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));

        Log.d(TAG, "RTCPeerConnectionCreateOfferWithObjectID start");
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
        Log.d(TAG, "RTCPeerConnectionCreateOfferWithObjectID end");
    }

    @ReactMethod
    public void peerConnectionCreateAnswer(final int id, final Callback callback) {
        PeerConnection peerConnection = mPeerConnections.get(id);

        // MediaConstraints constraints = new MediaConstraints();
        // constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        // constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));

        peerConnection.createAnswer(new SdpObserver() {
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
    }

    @ReactMethod
    public void peerConnectionSetLocalDescription(ReadableMap sdpMap, final int id, final Callback callback) {
        PeerConnection peerConnection = mPeerConnections.get(id);

        SessionDescription sdp = new SessionDescription(
            SessionDescription.Type.fromCanonicalForm(sdpMap.getString("type")),
            sdpMap.getString("sdp")
        );

        peerConnection.setLocalDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(final SessionDescription sdp) {}
            @Override
            public void onSetSuccess() {
                callback.invoke(true);
            }
            @Override
            public void onCreateFailure(String s) {}
            @Override
            public void onSetFailure(String s) {
                callback.invoke(false, s);
            }
        }, sdp);
    }
    @ReactMethod
    public void peerConnectionSetRemoteDescription(final ReadableMap sdpMap, final int id, final Callback callback) {
        Log.d(TAG, "RTCPeerConnectionSetRemoteDescriptionWithSessionDescriptionwerew");
        PeerConnection peerConnection = mPeerConnections.get(id);
        // final String d = sdpMap.getString("type");

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
    }
    @ReactMethod
    public void peerConnectionAddICECandidate(ReadableMap candidateMap, final int id, final Callback callback) {
        PeerConnection peerConnection = mPeerConnections.get(id);
        IceCandidate candidate = new IceCandidate(
            candidateMap.getString("sdpMid"),
            candidateMap.getInt("sdpMLineIndex"),
            candidateMap.getString("candidate")
        );
        boolean result = peerConnection.addIceCandidate(candidate);
        callback.invoke(result);
    }
    @ReactMethod
    public void peerConnectionClose(final int id) {
        PeerConnection peerConnection = mPeerConnections.get(id);
        peerConnection.close();
        mPeerConnections.remove(id);
    }

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
            default:
                return "";
        }
    }
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
            default:
                return "";
        }
    }
    public String iceGatheringStateString(PeerConnection.IceGatheringState iceGatheringState) {
        switch (iceGatheringState) {
            case NEW:
                return "new";
            case GATHERING:
                return "gathering";
            case COMPLETE:
                return "complete";
            default:
                return "";
        }
    }
}
