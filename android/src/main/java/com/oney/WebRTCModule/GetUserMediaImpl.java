package com.oney.WebRTCModule;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.ArrayList;
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

    private static final int PERMISSION_REQUEST_CODE = (int) (Math.random() * Short.MAX_VALUE);

    private final CameraEnumerator cameraEnumerator;
    private final ReactApplicationContext reactContext;

    /**
     * The application/library-specific private members of local
     * {@link MediaStreamTrack}s created by {@code GetUserMediaImpl} mapped by
     * track ID.
     */
    private final Map<String, TrackPrivate> tracks = new HashMap<>();

    private final WebRTCModule webRTCModule;

    private Promise displayMediaPromise;
    private Intent mediaProjectionPermissionResultData;

    GetUserMediaImpl(WebRTCModule webRTCModule, ReactApplicationContext reactContext) {
        this.webRTCModule = webRTCModule;
        this.reactContext = reactContext;

        boolean camera2supported = false;

        try {
            camera2supported = Camera2Enumerator.isSupported(reactContext);
        } catch (Throwable tr) {
            // Some devices will crash here with: Fatal Exception: java.lang.AssertionError: Supported FPS ranges cannot be null.
            // Make sure we don't.
            Log.w(TAG, "Error checking for Camera2 API support.", tr);
        }

        if (camera2supported) {
            Log.d(TAG, "Creating video capturer using Camera2 API.");
            cameraEnumerator = new Camera2Enumerator(reactContext);
        } else {
            Log.d(TAG, "Creating video capturer using Camera1 API.");
            cameraEnumerator = new Camera1Enumerator(false);
        }

        reactContext.addActivityEventListener(new BaseActivityEventListener() {
            @Override
            public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
                super.onActivityResult(activity, requestCode, resultCode, data);
                if (requestCode == PERMISSION_REQUEST_CODE) {
                    if (resultCode != Activity.RESULT_OK) {
                        displayMediaPromise.reject("DOMException", "NotAllowedError");
                        displayMediaPromise = null;
                        return;
                    }

                    mediaProjectionPermissionResultData = data;
                    createScreenStream();
                }
            }
        });
    }

    private AudioTrack createAudioTrack(ReadableMap constraints) {
        ReadableMap audioConstraintsMap = constraints.getMap("audio");

        Log.d(TAG, "getUserMedia(audio): " + audioConstraintsMap);

        String id = UUID.randomUUID().toString();
        PeerConnectionFactory pcFactory = webRTCModule.mFactory;
        AudioSource audioSource = pcFactory.createAudioSource(webRTCModule.constraintsForOptions(audioConstraintsMap));
        AudioTrack track = pcFactory.createAudioTrack(id, audioSource);
        tracks.put(
            id,
            new TrackPrivate(track, audioSource, /* videoCapturer */ null));

        return track;
    }

    ReadableArray enumerateDevices() {
        WritableArray array = Arguments.createArray();
        String[] devices = cameraEnumerator.getDeviceNames();

        for (int i = 0; i < devices.length; ++i) {
            String deviceName = devices[i];
            boolean isFrontFacing;
            try {
                // This can throw an exception when using the Camera 1 API.
                isFrontFacing = cameraEnumerator.isFrontFacing(deviceName);
            } catch (Exception e) {
                Log.e(TAG, "Failed to check the facing mode of camera");
                continue;
            }
            WritableMap params = Arguments.createMap();
            params.putString("facing", isFrontFacing ? "front" : "environment");
            params.putString("deviceId", "" + i);
            params.putString("groupId", "");
            params.putString("label", deviceName);
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
            ReadableMap videoConstraintsMap = constraints.getMap("video");

            Log.d(TAG, "getUserMedia(video): " + videoConstraintsMap);

            CameraCaptureController cameraCaptureController = new CameraCaptureController(
                cameraEnumerator,
                videoConstraintsMap);

            videoTrack = createVideoTrack(cameraCaptureController);
        }

        if (audioTrack == null && videoTrack == null) {
            // Fail with DOMException with name AbortError as per:
            // https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-getusermedia
            errorCallback.invoke("DOMException", "AbortError");
            return;
        }

        createStream(new MediaStreamTrack[]{audioTrack, videoTrack}, (streamId, tracksInfo) -> {
            WritableArray tracksInfoWritableArray = Arguments.createArray();

            for (WritableMap trackInfo : tracksInfo) {
                tracksInfoWritableArray.pushMap(trackInfo);
            }

            successCallback.invoke(streamId, tracksInfoWritableArray);
        });
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

    void disposeTrack(String id) {
        TrackPrivate track = tracks.remove(id);
        if (track != null) {
            track.dispose();
        }
    }

    void switchCamera(String trackId) {
        TrackPrivate track = tracks.get(trackId);
        if (track != null && track.videoCaptureController instanceof CameraCaptureController) {
            CameraCaptureController cameraCaptureController = (CameraCaptureController) track.videoCaptureController;
            cameraCaptureController.switchCamera();
        }
    }

    void getDisplayMedia(Promise promise) {
        if (this.displayMediaPromise != null) {
            promise.reject(new RuntimeException("Another operation is pending."));
            return;
        }

        Activity currentActivity = this.reactContext.getCurrentActivity();
        if (currentActivity == null) {
            promise.reject(new RuntimeException("No current Activity."));
            return;
        }

        this.displayMediaPromise = promise;

        MediaProjectionManager mediaProjectionManager =
            (MediaProjectionManager) currentActivity.getApplication().getSystemService(
                Context.MEDIA_PROJECTION_SERVICE);
        currentActivity.startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(), PERMISSION_REQUEST_CODE);
    }

    private void createScreenStream() {
        VideoTrack track = createScreenTrack();

        createStream(new MediaStreamTrack[]{track}, (streamId, tracksInfo) -> {
            WritableMap data = Arguments.createMap();

            data.putString("streamId", streamId);
            data.putMap("track", tracksInfo.get(0));

            displayMediaPromise.resolve(data);

            // Cleanup
            mediaProjectionPermissionResultData = null;
            displayMediaPromise = null;
        });
    }

    private void createStream(MediaStreamTrack[] tracks, BiConsumer<String, ArrayList<WritableMap>> successCallback) {
        String streamId = UUID.randomUUID().toString();
        MediaStream mediaStream = webRTCModule.mFactory.createLocalMediaStream(streamId);

        ArrayList<WritableMap> tracksInfo = new ArrayList<>();

        for (MediaStreamTrack track : tracks) {
            if (track == null) {
                continue;
            }

            if (track instanceof AudioTrack) {
                mediaStream.addTrack((AudioTrack) track);
            } else {
                mediaStream.addTrack((VideoTrack) track);
            }

            WritableMap trackInfo = Arguments.createMap();
            String trackId = track.id();

            trackInfo.putBoolean("enabled", track.enabled());
            trackInfo.putString("id", trackId);
            trackInfo.putString("kind", track.kind());
            trackInfo.putString("label", trackId);
            trackInfo.putString("readyState", track.state().toString());
            trackInfo.putBoolean("remote", false);
            tracksInfo.add(trackInfo);
        }

        Log.d(TAG, "MediaStream id: " + streamId);
        webRTCModule.localStreams.put(streamId, mediaStream);

        successCallback.accept(streamId, tracksInfo);
    }

    private VideoTrack createScreenTrack() {
        DisplayMetrics displayMetrics = getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        int fps = 30;
        ScreenCaptureController screenCaptureController = new ScreenCaptureController(width, height, fps, mediaProjectionPermissionResultData);
        VideoTrack track = createVideoTrack(screenCaptureController);

        return track;
    }

    private VideoTrack createVideoTrack(AbstractVideoCaptureController videoCaptureController) {
        videoCaptureController.initializeVideoCapturer();

        VideoCapturer videoCapturer = videoCaptureController.videoCapturer;
        if (videoCapturer == null) {
            return null;
        }

        PeerConnectionFactory pcFactory = webRTCModule.mFactory;
        EglBase.Context eglContext = EglUtils.getRootEglBaseContext();
        SurfaceTextureHelper surfaceTextureHelper =
            SurfaceTextureHelper.create("CaptureThread", eglContext);

        if (surfaceTextureHelper == null) {
            Log.d(TAG, "Error creating SurfaceTextureHelper");
            return null;
        }

        VideoSource videoSource = pcFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, reactContext, videoSource.getCapturerObserver());

        String id = UUID.randomUUID().toString();
        VideoTrack track = pcFactory.createVideoTrack(id, videoSource);

        track.setEnabled(true);
        tracks.put(id, new TrackPrivate(track, videoSource, videoCaptureController));

        videoCaptureController.startCapture();

        return track;
    }

    private DisplayMetrics getDisplayMetrics() {
        Activity currentActivity = this.reactContext.getCurrentActivity();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager =
            (WindowManager) currentActivity.getApplication().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        return displayMetrics;
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
        public final AbstractVideoCaptureController videoCaptureController;

        /**
         * Whether this object has been disposed or not.
         */
        private boolean disposed;

        /**
         * Initializes a new {@code TrackPrivate} instance.
         *
         * @param track
         * @param mediaSource            the {@code MediaSource} from which the specified
         *                               {@code code} was created
         * @param videoCaptureController the {@code AbstractVideoCaptureController} from which the
         *                               specified {@code mediaSource} was created if the specified
         *                               {@code track} is a {@link VideoTrack}
         */
        public TrackPrivate(
            MediaStreamTrack track,
            MediaSource mediaSource,
            AbstractVideoCaptureController videoCaptureController) {
            this.track = track;
            this.mediaSource = mediaSource;
            this.videoCaptureController = videoCaptureController;
            this.disposed = false;
        }

        public void dispose() {
            if (!disposed) {
                if (videoCaptureController != null) {
                    if (videoCaptureController.stopCapture()) {
                        videoCaptureController.dispose();
                    }
                }
                mediaSource.dispose();
                track.dispose();
                disposed = true;
            }
        }
    }

    private interface BiConsumer<T, U> {
        void accept(T t, U u);
    }
}
