package com.oney.WebRTCModule;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.webrtc.*;

/**
 * The implementation of {@code getUserMedia} extracted into a separate file in
 * order to reduce complexity and to (somewhat) separate concerns.
 */
class GetUserMediaImpl {
    /**
     * The {@link Log} tag with which {@code GetUserMediaImpl} is to log.
     */
    private static final String TAG = WebRTCModule.TAG;

    private final CameraEnumerator cameraEnumerator;
    private final ReactApplicationContext reactContext;

    /**
     * The application/library-specific private members of local
     * {@link MediaStreamTrack}s created by {@code GetUserMediaImpl} mapped by
     * track ID.
     */
    private final Map<String, TrackPrivate> tracks = new HashMap<>();

    private final WebRTCModule webRTCModule;

    GetUserMediaImpl(
            WebRTCModule webRTCModule,
            ReactApplicationContext reactContext) {
        this.webRTCModule = webRTCModule;
        this.reactContext = reactContext;

        // NOTE: to support Camera2, the device should:
        //   1. Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        //   2. all camera support level should greater than LEGACY
        //   see: https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics.html#INFO_SUPPORTED_HARDWARE_LEVEL
        if (Camera2Enumerator.isSupported(reactContext)) {
            Log.d(TAG, "Creating video capturer using Camera2 API.");
            cameraEnumerator = new Camera2Enumerator(reactContext);
        } else {
            Log.d(TAG, "Creating video capturer using Camera1 API.");
            cameraEnumerator = new Camera1Enumerator(false);
        }
    }

    private AudioTrack createAudioTrack(ReadableMap constraints) {
        MediaConstraints audioConstraints
            = webRTCModule.parseMediaConstraints(constraints.getMap("audio"));

        Log.d(TAG, "getUserMedia(audio): " + audioConstraints);

        String id = UUID.randomUUID().toString();
        PeerConnectionFactory pcFactory = webRTCModule.mFactory;
        AudioSource audioSource = pcFactory.createAudioSource(audioConstraints);
        AudioTrack track = pcFactory.createAudioTrack(id, audioSource);
        tracks.put(
            id,
            new TrackPrivate(track, audioSource, /* videoCapturer */ null));

        return track;
    }

    private VideoTrack createVideoTrack(ReadableMap constraints) {
        ReadableMap videoConstraintsMap = constraints.getMap("video");

        Log.d(TAG, "getUserMedia(video): " + videoConstraintsMap);

        VideoCaptureController videoCaptureController
            = new VideoCaptureController(cameraEnumerator, videoConstraintsMap);
        VideoCapturer videoCapturer = videoCaptureController.getVideoCapturer();
        if (videoCapturer == null) {
            return null;
        }

        PeerConnectionFactory pcFactory = webRTCModule.mFactory;
        VideoSource videoSource = pcFactory.createVideoSource(videoCapturer);

        String id = UUID.randomUUID().toString();
        VideoTrack track = pcFactory.createVideoTrack(id, videoSource);

        track.setEnabled(true);
        videoCaptureController.startCapture();

        tracks.put(id, new TrackPrivate(track, videoSource, videoCaptureController));

        return track;
    }

    ReadableArray enumerateDevices() {
        WritableArray array = Arguments.createArray();
        String[] devices = cameraEnumerator.getDeviceNames();

        for(int i = 0; i < devices.length; ++i) {
            WritableMap params = Arguments.createMap();
            params.putString("deviceId", "" + i);
            params.putString("groupId", "");
            params.putString("label", devices[i]);
            params.putString("kind", "videoinput");
            array.pushMap(params);
        }

        WritableMap audio = Arguments.createMap();
        audio.putString("deviceId", "audio-1");
        audio.putString("groupId", "");
        audio.putString("label", "Audio");
        audio.putString("kind", "audioinput");
        array.pushMap(audio);

        return array;
    }

    private ReactApplicationContext getReactApplicationContext() {
        return reactContext;
    }

    MediaStreamTrack getTrack(String id) {
        TrackPrivate private_ = tracks.get(id);

        return private_ == null ? null : private_.track;
    }

    /**
     * Implements {@code getUserMedia}. Note that at this point constraints have
     * been normalized and permissions have been granted. The constraints only
     * contain keys for which permissions have already been granted, that is,
     * if audio permission was not granted, there will be no "audio" key in
     * the constraints map.
     */
    void getUserMedia(
            final ReadableMap constraints,
            final Callback successCallback,
            final Callback errorCallback) {
        // TODO: change getUserMedia constraints format to support new syntax
        //   constraint format seems changed, and there is no mandatory any more.
        //   and has a new syntax/attrs to specify resolution
        //   should change `parseConstraints()` according
        //   see: https://www.w3.org/TR/mediacapture-streams/#idl-def-MediaTrackConstraints

        AudioTrack audioTrack = null;
        VideoTrack videoTrack = null;

        if (constraints.hasKey("audio")) {
            audioTrack = createAudioTrack(constraints);
        }

        if (constraints.hasKey("video")) {
            videoTrack = createVideoTrack(constraints);
        }

        if (audioTrack == null && videoTrack == null) {
             // Fail with DOMException with name AbortError as per:
             // https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-getusermedia
             errorCallback.invoke("DOMException","AbortError");
             return;
        }

        String streamId = UUID.randomUUID().toString();
        MediaStream mediaStream
            = webRTCModule.mFactory.createLocalMediaStream(streamId);
        WritableArray tracks = Arguments.createArray();

        for (MediaStreamTrack track : new MediaStreamTrack[]{audioTrack, videoTrack}) {
            if (track == null) {
                continue;
            }

            if (track instanceof AudioTrack) {
                mediaStream.addTrack((AudioTrack) track);
            } else {
                mediaStream.addTrack((VideoTrack) track);
            }

            WritableMap track_ = Arguments.createMap();
            String trackId = track.id();

            track_.putBoolean("enabled", track.enabled());
            track_.putString("id", trackId);
            track_.putString("kind", track.kind());
            track_.putString("label", trackId);
            track_.putString("readyState", track.state().toString());
            track_.putBoolean("remote", false);
            tracks.pushMap(track_);
        }

        Log.d(TAG, "MediaStream id: " + streamId);
        webRTCModule.localStreams.put(streamId, mediaStream);

        successCallback.invoke(streamId, tracks);
    }

    void mediaStreamTrackSetEnabled(String trackId, final boolean enabled) {
        TrackPrivate track = tracks.get(trackId);
        if (track != null && track.videoCaptureController != null) {
            if (enabled) {
                track.videoCaptureController.startCapture();
            } else {
                track.videoCaptureController.stopCapture();
            }
        }
    }

    void mediaStreamTrackStop(String id) {
        MediaStreamTrack track = getTrack(id);
        if (track == null) {
            Log.d(
                TAG,
                "mediaStreamTrackStop() No local MediaStreamTrack with id "
                    + id);
            return;
        }
        track.setEnabled(false);
        removeTrack(id);
    }

    private void removeTrack(String id) {
        TrackPrivate track = tracks.remove(id);
        if (track != null) {
            VideoCaptureController videoCaptureController
                = track.videoCaptureController;
            if (videoCaptureController != null) {
                if (videoCaptureController.stopCapture()) {
                    videoCaptureController.dispose();
                }
            }
            track.mediaSource.dispose();
        }
    }

    void switchCamera(String trackId) {
        TrackPrivate track = tracks.get(trackId);
        if (track != null && track.videoCaptureController != null) {
            track.videoCaptureController.switchCamera();
        }
    }

    /**
     * Application/library-specific private members of local
     * {@code MediaStreamTrack}s created by {@code GetUserMediaImpl}.
     */
    private static class TrackPrivate {
        /**
         * The {@code MediaSource} from which {@link #track} was created.
         */
        public final MediaSource mediaSource;

        public final MediaStreamTrack track;

        /**
         * The {@code VideoCapturer} from which {@link #mediaSource} was created
         * if {@link #track} is a {@link VideoTrack}.
         */
        public final VideoCaptureController videoCaptureController;

        /**
         * Initializes a new {@code TrackPrivate} instance.
         *
         * @param track
         * @param mediaSource the {@code MediaSource} from which the specified
         * {@code code} was created
         * @param videoCapturer the {@code VideoCapturer} from which the
         * specified {@code mediaSource} was created if the specified
         * {@code track} is a {@link VideoTrack}
         */
        public TrackPrivate(
                MediaStreamTrack track,
                MediaSource mediaSource,
                VideoCaptureController videoCaptureController) {
            this.track = track;
            this.mediaSource = mediaSource;
            this.videoCaptureController = videoCaptureController;
        }
    }
}
