package com.oney.rnwebrtc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import com.oney.rnwebrtc.utils.DisplayUtils;
import com.oney.rnwebrtc.utils.EglUtils;
import com.oney.rnwebrtc.utils.ThreadUtils;
import com.oney.rnwebrtc.processors.ProcessorProvider;
import com.oney.rnwebrtc.processors.VideoEffectProcessor;
import com.oney.rnwebrtc.processors.VideoFrameProcessor;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaSource;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The implementation of {@code getUserMedia} extracted into a separate file in
 * order to reduce complexity and to (somewhat) separate concerns.
 */
class GetUserMediaImpl {
    /**
     * The {@link Log} tag with which {@code GetUserMediaImpl} is to log.
     */
    private static final String TAG = WebRTCModuleImpl.TAG;

    private static final int PERMISSION_REQUEST_CODE = (int) (Math.random() * Short.MAX_VALUE);

    private CameraEnumerator cameraEnumerator;
    private final ReactApplicationContext reactContext;

    /**
     * The application/library-specific private members of local
     * {@link MediaStreamTrack}s created by {@code GetUserMediaImpl} mapped by
     * track ID.
     */
    private final Map<String, TrackPrivate> tracks = new HashMap<>();
    private final WebRTCModuleImpl webRTCModuleImpl;

    private Promise displayMediaPromise;
    private Intent mediaProjectionPermissionResultData;

    GetUserMediaImpl(WebRTCModuleImpl webRTCModuleImpl, ReactApplicationContext reactContext) {
        this.webRTCModuleImpl = webRTCModuleImpl;
        this.reactContext = reactContext;

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

                    ThreadUtils.runOnExecutor(() -> {
                        MediaProjectionService.launch(activity);
                        createScreenStream();
                    });
                }
            }
        });
    }

    private AudioTrack createAudioTrack(ReadableMap constraints) {
        ReadableMap audioConstraintsMap = constraints.getMap("audio");

        Log.d(TAG, "getUserMedia(audio): " + audioConstraintsMap);

        String id = UUID.randomUUID().toString();
        PeerConnectionFactory pcFactory = webRTCModuleImpl.peerConnectionFactory;
        MediaConstraints peerConstraints = webRTCModuleImpl.constraintsForOptions(audioConstraintsMap);

        // PeerConnectionFactory.createAudioSource will throw an error when mandatory constraints contain nulls.
        // so, let's check for nulls
        checkMandatoryConstraints(peerConstraints);

        AudioSource audioSource = pcFactory.createAudioSource(peerConstraints);
        AudioTrack track = pcFactory.createAudioTrack(id, audioSource);

        // surfaceTextureHelper is initialized for videoTrack only, so its null here.
        tracks.put(id, new TrackPrivate(track, audioSource, /* videoCapturer */ null, /* surfaceTextureHelper */ null));

        return track;
    }

    private void checkMandatoryConstraints(MediaConstraints peerConstraints) {
        ArrayList<MediaConstraints.KeyValuePair> valid = new ArrayList<>(peerConstraints.mandatory.size());

        for (MediaConstraints.KeyValuePair constraint : peerConstraints.mandatory) {
            if (constraint.getValue() != null) {
                valid.add(constraint);
            } else {
                Log.d(TAG, String.format("constraint %s is null, ignoring it", constraint.getKey()));
            }
        }

        peerConstraints.mandatory.clear();
        peerConstraints.mandatory.addAll(valid);
    }

    private CameraEnumerator getCameraEnumerator() {
        if (cameraEnumerator == null) {
            if (Camera2Enumerator.isSupported(reactContext)) {
                Log.d(TAG, "Creating camera enumerator using the Camera2 API");
                cameraEnumerator = new Camera2Enumerator(reactContext);
            } else {
                Log.d(TAG, "Creating camera enumerator using the Camera1 API");
                cameraEnumerator = new Camera1Enumerator(false);
            }
        }

        return cameraEnumerator;
    }

    void enumerateDevices(Promise promise) {
        WritableArray array = Arguments.createArray();
        String[] devices = getCameraEnumerator().getDeviceNames();

        for (int i = 0; i < devices.length; ++i) {
            String deviceName = devices[i];
            boolean isFrontFacing;

            try {
                // This can throw an exception when using the Camera 1 API.
                isFrontFacing = getCameraEnumerator().isFrontFacing(deviceName);
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

        promise.resolve(array);
    }

    MediaStreamTrack getTrack(String trackId) {
        TrackPrivate private_ = tracks.get(trackId);

        return private_ == null ? null : private_.track;
    }

    /**
     * Implements {@code getUserMedia}. Note that at this point constraints have
     * been normalized and permissions have been granted. The constraints only
     * contain keys for which permissions have already been granted, that is,
     * if audio permission was not granted, there will be no "audio" key in
     * the constraints map.
     */
    void getUserMedia(final ReadableMap constraints, Promise promise) {
        AudioTrack audioTrack = null;
        VideoTrack videoTrack = null;

        if (constraints.hasKey("audio")) {
            audioTrack = createAudioTrack(constraints);
        }

        if (constraints.hasKey("video")) {
            ReadableMap videoConstraintsMap = constraints.getMap("video");

            Log.d(TAG, "getUserMedia(video): " + videoConstraintsMap);

            CameraCaptureController cameraCaptureController =
                                        new CameraCaptureController(getCameraEnumerator(), videoConstraintsMap);

            videoTrack = createVideoTrack(cameraCaptureController);
        }

        if (audioTrack == null && videoTrack == null) {
            // Fail with DOMException with name AbortError as per:
            // https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-getusermedia
            promise.reject("DOMException", "AbortError");
            return;
        }

        createStream(new MediaStreamTrack[] {audioTrack, videoTrack}, (streamId, tracksInfo) -> {
            WritableMap data = Arguments.createMap();
            data.putString("streamId", streamId);

            WritableArray tracks = Arguments.createArray();
            for (WritableMap trackInfo : tracksInfo) {
                tracks.pushMap(trackInfo);
            }
            data.putArray("tracksInfo", tracks);

            promise.resolve(data);
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

    void disposeTrack(String trackId) {
        TrackPrivate track = tracks.remove(trackId);
        if (track != null) {
            track.dispose();
        }
    }

    void switchCamera(String trackId) {
        TrackPrivate track = tracks.get(trackId);
        if (track != null && track.videoCaptureController instanceof CameraCaptureController cameraCaptureController) {
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

        if (mediaProjectionManager != null) {
            UiThreadUtil.runOnUiThread(() -> currentActivity.startActivityForResult(
                    mediaProjectionManager.createScreenCaptureIntent(), PERMISSION_REQUEST_CODE));

        } else {
            promise.reject(new RuntimeException("MediaProjectionManager is null."));
        }
    }

    private void createScreenStream() {
        VideoTrack track = createScreenTrack();

        if (track == null) {
            displayMediaPromise.reject(new RuntimeException("ScreenTrack is null."));
        } else {
            createStream(new MediaStreamTrack[] {track}, (streamId, tracksInfo) -> {
                WritableMap data = Arguments.createMap();
                data.putString("streamId", streamId);

                if (tracksInfo.isEmpty()) {
                    displayMediaPromise.reject(new RuntimeException("No ScreenTrackInfo found."));
                } else {
                    data.putMap("track", tracksInfo.get(0));
                    displayMediaPromise.resolve(data);
                }
            });
        }

        // Cleanup
        mediaProjectionPermissionResultData = null;
        displayMediaPromise = null;
    }

    void createStream(MediaStreamTrack[] tracks, BiConsumer<String, ArrayList<WritableMap>> successCallback) {
        String streamId = UUID.randomUUID().toString();
        MediaStream mediaStream = webRTCModuleImpl.peerConnectionFactory.createLocalMediaStream(streamId);

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
            trackInfo.putString("readyState", "live");
            trackInfo.putBoolean("remote", false);

            if (track instanceof VideoTrack) {
                TrackPrivate tp = this.tracks.get(trackId);
                AbstractVideoCaptureController vcc = tp.videoCaptureController;
                WritableMap settings = Arguments.createMap();
                settings.putInt("height", vcc.getHeight());
                settings.putInt("width", vcc.getWidth());
                settings.putInt("frameRate", vcc.getFrameRate());
                trackInfo.putMap("settings", settings);
            }

            tracksInfo.add(trackInfo);
        }

        Log.d(TAG, "MediaStream id: " + streamId);
        webRTCModuleImpl.localStreams.put(streamId, mediaStream);

        successCallback.accept(streamId, tracksInfo);
    }

    private VideoTrack createScreenTrack() {
        DisplayMetrics displayMetrics = DisplayUtils.getDisplayMetrics(reactContext.getCurrentActivity());
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        ScreenCaptureController screenCaptureController = new ScreenCaptureController(
                reactContext.getCurrentActivity(), width, height, mediaProjectionPermissionResultData);
        return createVideoTrack(screenCaptureController);
    }

    VideoTrack createVideoTrack(AbstractVideoCaptureController videoCaptureController) {
        videoCaptureController.initializeVideoCapturer();

        VideoCapturer videoCapturer = videoCaptureController.videoCapturer;
        if (videoCapturer == null) {
            return null;
        }

        PeerConnectionFactory pcFactory = webRTCModuleImpl.peerConnectionFactory;
        EglBase.Context eglContext = EglUtils.getRootEglBaseContext();
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglContext);

        if (surfaceTextureHelper == null) {
            Log.d(TAG, "Error creating SurfaceTextureHelper");
            return null;
        }

        String id = UUID.randomUUID().toString();

        TrackCapturerEventsEmitter eventsEmitter = new TrackCapturerEventsEmitter(webRTCModuleImpl, id);
        videoCaptureController.setCapturerEventsListener(eventsEmitter);

        VideoSource videoSource = pcFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, reactContext, videoSource.getCapturerObserver());

        VideoTrack track = pcFactory.createVideoTrack(id, videoSource);

        track.setEnabled(true);
        tracks.put(id, new TrackPrivate(track, videoSource, videoCaptureController, surfaceTextureHelper));

        videoCaptureController.startCapture();

        return track;
    }

    /**
     * Set video effects to the TrackPrivate corresponding to the trackId with the help of VideoEffectProcessor
     * corresponding to the names.
     * @param trackId TrackPrivate id
     * @param names VideoEffectProcessor names
     */
    void setVideoEffects(String trackId, ReadableArray names) {
        TrackPrivate track = tracks.get(trackId);

        if (track != null && track.videoCaptureController instanceof CameraCaptureController) {
            VideoSource videoSource = (VideoSource) track.mediaSource;
            SurfaceTextureHelper surfaceTextureHelper = track.surfaceTextureHelper;

            if (names != null) {
                List<VideoFrameProcessor> processors = names.toArrayList().stream()
                    .filter(name -> name instanceof String)
                    .map(name -> {
                        VideoFrameProcessor videoFrameProcessor = ProcessorProvider.getProcessor((String) name);
                        if (videoFrameProcessor == null) {
                            Log.e(TAG, "no videoFrameProcessor associated with this name: " + name);
                        }
                        return videoFrameProcessor;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

                VideoEffectProcessor videoEffectProcessor = new VideoEffectProcessor(processors, surfaceTextureHelper);
                videoSource.setVideoProcessor(videoEffectProcessor);

            } else {
                videoSource.setVideoProcessor(null);
            }
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
        public final AbstractVideoCaptureController videoCaptureController;

        private final SurfaceTextureHelper surfaceTextureHelper;

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
        public TrackPrivate(MediaStreamTrack track, MediaSource mediaSource,
                AbstractVideoCaptureController videoCaptureController, SurfaceTextureHelper surfaceTextureHelper) {
            this.track = track;
            this.mediaSource = mediaSource;
            this.videoCaptureController = videoCaptureController;
            this.surfaceTextureHelper = surfaceTextureHelper;
            this.disposed = false;
        }

        public void dispose() {
            if (!disposed) {
                if (videoCaptureController != null) {
                    if (videoCaptureController.stopCapture()) {
                        videoCaptureController.dispose();
                    }
                }

                /*
                 * As per webrtc library documentation - The caller still has ownership of {@code
                 * surfaceTextureHelper} and is responsible for making sure surfaceTextureHelper.dispose() is
                 * called. This also means that the caller can reuse the SurfaceTextureHelper to initialize a new
                 * VideoCapturer once the previous VideoCapturer has been disposed. */

                if (surfaceTextureHelper != null) {
                    surfaceTextureHelper.stopListening();
                    surfaceTextureHelper.dispose();
                }

                mediaSource.dispose();
                track.dispose();
                disposed = true;
            }
        }
    }

    public interface BiConsumer<T, U> { void accept(T t, U u); }
}
