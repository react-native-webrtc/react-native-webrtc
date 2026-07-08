package com.oney.WebRTCModule;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionConfig;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.core.util.Consumer;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.oney.WebRTCModule.foregroundService.ForegroundServiceController;
import com.oney.WebRTCModule.videoEffects.ProcessorProvider;
import com.oney.WebRTCModule.videoEffects.VideoEffectProcessor;
import com.oney.WebRTCModule.videoEffects.VideoFrameProcessor;

import org.webrtc.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    private CameraEnumerator cameraEnumerator;
    private final ReactApplicationContext reactContext;

    /**
     * The application/library-specific private members of local
     * {@link MediaStreamTrack}s created by {@code GetUserMediaImpl} mapped by
     * track ID.
     */
    private final Map<String, TrackPrivate> tracks = new HashMap<>();

    /**
     * poolId -> {@link CustomVideoBufferPool} registry for custom-video pooled
     * buffers. Pools are owned by JS: an entry is added by
     * {@link #createCustomVideoBufferPool} and removed by
     * {@link #releaseCustomVideoBufferPool}. Mutated only on the module executor,
     * but concurrent for parity with {@link #customVideoControllers}.
     */
    private final Map<String, CustomVideoBufferPool> customVideoBufferPools = new ConcurrentHashMap<>();

    /**
     * trackId -> {@link CustomVideoCaptureController} registry, resolved by the
     * per-frame push path. That push runs synchronously on the caller's (worklet)
     * thread, which races the executor mutating {@link #tracks} (an unsynchronised
     * HashMap); this concurrent registry decouples delivery from {@code tracks}.
     * Entries are added by {@link #createCustomVideoTrack} and removed by
     * {@link #disposeTrack}.
     */
    private final Map<String, CustomVideoCaptureController> customVideoControllers = new ConcurrentHashMap<>();

    private final WebRTCModule webRTCModule;

    private Promise displayMediaPromise;
    private Intent mediaProjectionPermissionResultData;
    private boolean createConfigForDefaultDisplay = false;
    private float resolutionScale = 1.0f;

    // Reusable SurfaceTextureHelper for camera captures.
    // By reusing a single STH for all camera sessions we hold exactly one EGL context
    // regardless of how many times the camera is toggled.  On dispose we only call
    // stopListening() so
    // the context stays alive for the next getUserMedia call.
    private SurfaceTextureHelper reusableCameraSTH = null;

    GetUserMediaImpl(WebRTCModule webRTCModule, ReactApplicationContext reactContext) {
        this.webRTCModule = webRTCModule;
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

                    new Thread(() -> {
                        ForegroundServiceController.getInstance().onScreenShareStarted(activity);
                        ThreadUtils.runOnExecutor(GetUserMediaImpl.this::createScreenStream);
                    }).start();
                }
            }
        });
    }

    private AudioTrack createAudioTrack(ReadableMap constraints) {
        ReadableMap audioConstraintsMap = constraints.getMap("audio");

        Log.d(TAG, "getUserMedia(audio): " + audioConstraintsMap);

        String id = UUID.randomUUID().toString();
        PeerConnectionFactory pcFactory = webRTCModule.mFactory;
        MediaConstraints peerConstraints = webRTCModule.constraintsForOptions(audioConstraintsMap);

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

    ReadableArray enumerateDevices() {
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
    void getUserMedia(final ReadableMap constraints, final Callback successCallback, final Callback errorCallback) {
        AudioTrack audioTrack = null;
        VideoTrack videoTrack = null;

        if (constraints.hasKey("audio")) {
            audioTrack = createAudioTrack(constraints);
        }

        if (constraints.hasKey("video")) {
            ReadableMap videoConstraintsMap = constraints.getMap("video");

            Log.d(TAG, "getUserMedia(video): " + videoConstraintsMap);

            Activity currentActivity = this.reactContext.getCurrentActivity();
            if (currentActivity == null) {
                errorCallback.invoke("Error", "No current Activity.");
                return;
            }

            CameraCaptureController cameraCaptureController =
                    new CameraCaptureController(currentActivity, getCameraEnumerator(), videoConstraintsMap);

            videoTrack = createVideoTrack(cameraCaptureController);
        }

        if (audioTrack == null && videoTrack == null) {
            // Fail with DOMException with name AbortError as per:
            // https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-getusermedia
            errorCallback.invoke("DOMException", "AbortError");
            return;
        }

        createStream(new MediaStreamTrack[] {audioTrack, videoTrack}, (streamId, tracksInfo) -> {
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
        customVideoControllers.remove(id);
        if (track != null) {
            track.dispose();
        }
    }

    void applyConstraints(String trackId, ReadableMap constraints, Promise promise) {
        TrackPrivate track = tracks.get(trackId);
        if (track != null && track.videoCaptureController instanceof AbstractVideoCaptureController) {
            AbstractVideoCaptureController captureController =
                    (AbstractVideoCaptureController) track.videoCaptureController;
            captureController.applyConstraints(constraints, new Consumer<Exception>() {
                public void accept(Exception e) {
                    if (e != null) {
                        promise.reject(e);
                        return;
                    }

                    promise.resolve(captureController.getSettings());
                }
            });
        } else {
            promise.reject(new Exception("Camera track not found!"));
        }
    }

    void initializeConstraints(ReadableMap constraints) {
        // Handle the incoming params

        ReadableMap androidConstraints = null;
        if (constraints.hasKey("android") && constraints.getType("android") == ReadableType.Map) {
            androidConstraints = constraints.getMap("android");
        }

        // Default values
        boolean createConfigForDefaultDisplay = false;
        float scale = 1.0f;

        if (androidConstraints != null) {
            // MediaProjectionConfig need API level 34
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    && androidConstraints.hasKey("createConfigForDefaultDisplay")
                    && androidConstraints.getType("createConfigForDefaultDisplay") == ReadableType.Boolean) {
                createConfigForDefaultDisplay = androidConstraints.getBoolean("createConfigForDefaultDisplay");
            }
            if (androidConstraints.hasKey("resolutionScale")
                    && androidConstraints.getType("resolutionScale") == ReadableType.Number) {
                scale = (float) androidConstraints.getDouble("resolutionScale");
            }
        }

        this.createConfigForDefaultDisplay = createConfigForDefaultDisplay;
        // Force the value in [0, 1]
        this.resolutionScale = Math.max(0.0f, Math.min(1.0f, scale));

        Log.d(TAG,
                "initializeConstraints: createConfigForDefaultDisplay=" + this.createConfigForDefaultDisplay
                        + " resolutionScale=" + this.resolutionScale);
    }

    void getDisplayMedia(final ReadableMap constraints, Promise promise) {
        if (this.displayMediaPromise != null) {
            promise.reject(new RuntimeException("Another operation is pending."));
            return;
        }

        Activity currentActivity = this.reactContext.getCurrentActivity();
        if (currentActivity == null) {
            promise.reject(new RuntimeException("No current Activity."));
            return;
        }

        // Screen capture needs a foreground service of type mediaProjection running before/while
        // capturing — otherwise MediaProjection delivers black frames. Enable the dedicated
        // MediaProjectionService here so the capture path is self-contained and does not depend on
        // the app having started a separate foreground service (e.g. for microphone) first.
        WebRTCModuleOptions.getInstance().enableMediaProjectionService = true;

        this.initializeConstraints(constraints);

        this.displayMediaPromise = promise;

        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) currentActivity.getApplication().getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);

        if (mediaProjectionManager != null) {
            UiThreadUtil.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (createConfigForDefaultDisplay == true) {
                        // MediaProjectionConfig need API level 34
                        // Return mediaProjection which restricts the user to capturing the default display
                        currentActivity.startActivityForResult(
                                mediaProjectionManager.createScreenCaptureIntent(
                                        MediaProjectionConfig.createConfigForDefaultDisplay()),
                                PERMISSION_REQUEST_CODE);
                    } else {
                        // Return mediaProjection which allows the user to decide which region is captured
                        currentActivity.startActivityForResult(
                                mediaProjectionManager.createScreenCaptureIntent(), PERMISSION_REQUEST_CODE);
                    }
                }
            });

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

                if (tracksInfo.size() == 0) {
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
            trackInfo.putString("readyState", "live");
            trackInfo.putBoolean("remote", false);

            if (track instanceof VideoTrack) {
                TrackPrivate tp = this.tracks.get(trackId);
                AbstractVideoCaptureController vcc = tp.videoCaptureController;
                trackInfo.putMap("settings", vcc.getSettings());
            }

            if (track instanceof AudioTrack) {
                WritableMap settings = Arguments.createMap();
                settings.putString("deviceId", "audio-1");
                settings.putString("groupId", "");
                trackInfo.putMap("settings", settings);
            }

            tracksInfo.add(trackInfo);
        }

        Log.d(TAG, "MediaStream id: " + streamId);
        webRTCModule.localStreams.put(streamId, mediaStream);

        successCallback.accept(streamId, tracksInfo);
    }

    private VideoTrack createScreenTrack() {
        DisplayMetrics displayMetrics = DisplayUtils.getDisplayMetrics(reactContext.getCurrentActivity());
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        ScreenCaptureController screenCaptureController = new ScreenCaptureController(
                reactContext.getCurrentActivity(), width, height, mediaProjectionPermissionResultData, resolutionScale);
        return createVideoTrack(screenCaptureController);
    }

    VideoTrack createVideoTrack(AbstractVideoCaptureController videoCaptureController) {
        videoCaptureController.initializeVideoCapturer();

        VideoCapturer videoCapturer = videoCaptureController.videoCapturer;
        if (videoCapturer == null) {
            return null;
        }

        PeerConnectionFactory pcFactory = webRTCModule.mFactory;
        EglBase.Context eglContext = EglUtils.getRootEglBaseContext();

        boolean isCameraCapture = videoCaptureController instanceof CameraCaptureController;
        SurfaceTextureHelper surfaceTextureHelper;

        if (isCameraCapture) {
            if (reusableCameraSTH == null) {
                reusableCameraSTH = SurfaceTextureHelper.create("CaptureThread", eglContext);
            }
            surfaceTextureHelper = reusableCameraSTH;
        } else {
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglContext);
        }

        if (surfaceTextureHelper == null) {
            Log.d(TAG, "Error creating SurfaceTextureHelper");
            return null;
        }

        String id = UUID.randomUUID().toString();

        TrackCapturerEventsEmitter eventsEmitter = new TrackCapturerEventsEmitter(webRTCModule, id);
        videoCaptureController.setCapturerEventsListener(eventsEmitter);

        VideoSource videoSource = pcFactory.createVideoSource(videoCapturer.isScreencast());
        CapturerObserver capturerObserver = videoSource.getCapturerObserver();
        if (videoCapturer.isScreencast()) {
            // Screen capture only emits a frame when the screen content changes, so on a static
            // screen the encoder can't satisfy a keyframe request (PLI) from a newly-joined viewer.
            // Repeat the last frame at a minimum cadence so keyframes stay available. Runs on the
            // SurfaceTextureHelper handler thread (where frames are delivered).
            capturerObserver = new FrameRepeatingCapturerObserver(capturerObserver, surfaceTextureHelper.getHandler());
        }
        videoCapturer.initialize(surfaceTextureHelper, reactContext, capturerObserver);

        VideoTrack track = pcFactory.createVideoTrack(id, videoSource);

        track.setEnabled(true);
        tracks.put(id,
                new TrackPrivate(track, videoSource, videoCaptureController, surfaceTextureHelper, isCameraCapture));

        videoCaptureController.startCapture();

        return track;
    }

    /**
     * Allocates a pool of AHardwareBuffer (AHB) backed surfaces the app renders into on the GPU
     * (pooled mode). Resolves the cross-platform shape
     * {@code { poolId, buffers:[{ index, surfaceHandle, width, height }] }} which
     * {@code src/createCustomVideoTrack.ts} consumes unchanged. The pool is owned by JS and freed
     * via {@link #releaseCustomVideoBufferPool}; attach it to a track with
     * {@link #createCustomVideoTrack}.
     *
     * <p>Requires API level 26+ (the AHB pool uses {@code __INTRODUCED_IN(26)} APIs); rejects on
     * older devices BEFORE referencing {@link CustomVideoBufferPool}/{@link AHardwareBufferAllocator},
     * so the native AHB library is never loaded on unsupported systems.
     *
     * @param init    {@code { width, height, poolSize }} pool description.
     * @param promise resolves with {@code { poolId, buffers }} or rejects on failure.
     */
    void createCustomVideoBufferPool(ReadableMap init, Promise promise) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            promise.reject("E_UNSUPPORTED_API_LEVEL", "Custom video tracks require Android 8.0 (API 26) or newer.");
            return;
        }

        int width;
        int height;
        int poolSize;
        try {
            width = init != null && init.hasKey("width") ? init.getInt("width") : 0;
            height = init != null && init.hasKey("height") ? init.getInt("height") : 0;
            poolSize = init != null && init.hasKey("poolSize") ? init.getInt("poolSize") : 0;
        } catch (Exception e) {
            promise.reject("E_INVALID_CUSTOM_VIDEO_BUFFER_POOL_INIT",
                    "Custom video buffer pool width, height and poolSize must be positive integers.",
                    e);
            return;
        }
        if (width <= 0 || height <= 0 || poolSize <= 0) {
            promise.reject("E_INVALID_CUSTOM_VIDEO_BUFFER_POOL_INIT",
                    "Custom video buffer pool width, height and poolSize must be positive integers.");
            return;
        }

        CustomVideoBufferPool pool;
        try {
            pool = new CustomVideoBufferPool(width, height, poolSize);
        } catch (IllegalArgumentException e) {
            promise.reject("E_INVALID_CUSTOM_VIDEO_BUFFER_POOL_INIT", e.getMessage(), e);
            return;
        } catch (Exception e) {
            promise.reject("E_CUSTOM_VIDEO_BUFFER_POOL_FAILED", e.getMessage(), e);
            return;
        }

        String poolId = UUID.randomUUID().toString();
        customVideoBufferPools.put(poolId, pool);

        WritableMap data = Arguments.createMap();
        data.putString("poolId", poolId);
        data.putArray("buffers", pool.getBufferDescriptors());

        Log.d(TAG, "createCustomVideoBufferPool poolId=" + poolId + " " + width + "x" + height + " x" + poolSize);
        promise.resolve(data);
    }

    /**
     * Releases a pool created by {@link #createCustomVideoBufferPool}, freeing its AHBs. Resolves
     * null; a no-op (still resolves) when the poolId is null or already released. Rejects
     * {@code E_CUSTOM_VIDEO_POOL_IN_USE} while the pool's attached track is still live: in-flight
     * frame deliveries may hold references to the pool's AHB handles, so disposing here would be a
     * use-after-free. Stop the track first, then retry.
     */
    void releaseCustomVideoBufferPool(String poolId, Promise promise) {
        if (poolId == null) {
            promise.resolve(null);
            return;
        }
        CustomVideoBufferPool pool = customVideoBufferPools.get(poolId);
        if (pool == null) {
            promise.resolve(null);
            return;
        }
        if (pool.isAttachedToLiveTrack()) {
            promise.reject("E_CUSTOM_VIDEO_POOL_IN_USE",
                    "Cannot release a custom video buffer pool while its track is live. Stop the track first.");
            return;
        }
        customVideoBufferPools.remove(poolId);
        pool.dispose();
        promise.resolve(null);
    }

    /**
     * Creates a custom video track. Resolves the cross-platform shape
     * {@code { streamId, track }} which {@code src/createCustomVideoTrack.ts} consumes unchanged.
     *
     * <ul>
     *   <li>{@code poolId} present -> <b>pooled</b>: bind to the named
     *       {@link CustomVideoBufferPool} (the app renders into it and pushes by index). A pool binds
     *       to exactly one track; a missing pool rejects {@code E_CUSTOM_VIDEO_TRACK_FAILED}, an
     *       already-attached pool rejects {@code E_CUSTOM_VIDEO_POOL_IN_USE}.</li>
     *   <li>{@code poolId} absent -> <b>forwarding</b>: no pool; the app forwards finished
     *       {@code AHardwareBuffer*}s.</li>
     * </ul>
     *
     * <p>Requires API level 26+; rejects on older devices BEFORE referencing
     * {@link CustomVideoCaptureController}.
     *
     * @param init    {@code { poolId? }}.
     * @param promise resolves with {@code { streamId, track }} or rejects on failure.
     */
    void createCustomVideoTrack(ReadableMap init, Promise promise) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            promise.reject("E_UNSUPPORTED_API_LEVEL", "Custom video tracks require Android 8.0 (API 26) or newer.");
            return;
        }

        String poolId = init != null && init.hasKey("poolId") ? init.getString("poolId") : null;

        CustomVideoCaptureController captureController;
        if (poolId != null) {
            CustomVideoBufferPool pool = customVideoBufferPools.get(poolId);
            if (pool == null) {
                promise.reject("E_CUSTOM_VIDEO_TRACK_FAILED", "No custom video buffer pool for id " + poolId);
                return;
            }
            captureController = new CustomVideoCaptureController(pool);
            if (!pool.tryAttach(captureController)) {
                promise.reject(
                        "E_CUSTOM_VIDEO_POOL_IN_USE", "Custom video buffer pool is already attached to a track.");
                return;
            }
        } else {
            captureController = new CustomVideoCaptureController();
        }

        PeerConnectionFactory pcFactory = webRTCModule.mFactory;

        // Capturer-less video source: no SurfaceTextureHelper / VideoCapturer. Frames are pushed
        // by the app. isScreencast=false keeps the standard (non-screen) encoder tuning.
        VideoSource videoSource = pcFactory.createVideoSource(false);
        // Mark the source as started so it accepts delivered frames (mirrors
        // VideoCapturer.initialize(...).startCapture() signalling onCapturerStarted).
        videoSource.getCapturerObserver().onCapturerStarted(true);

        // Wire frame delivery (AHB -> OES texture -> VideoFrame) into the source, then start
        // accepting pushes.
        captureController.attachVideoSource(videoSource);
        captureController.startCapture();

        String trackId = UUID.randomUUID().toString();
        VideoTrack videoTrack = pcFactory.createVideoTrack(trackId, videoSource);
        videoTrack.setEnabled(true);

        // Register so the existing disposeTrack -> TrackPrivate.dispose path tears down the GL
        // imports (CustomVideoCaptureController.dispose) and disposes the source/track.
        tracks.put(
                trackId, new TrackPrivate(videoTrack, videoSource, captureController, /* surfaceTextureHelper */ null));
        // Thread-safe registry read by the per-frame push path (worklet thread).
        customVideoControllers.put(trackId, captureController);

        String streamId = UUID.randomUUID().toString();
        MediaStream mediaStream = pcFactory.createLocalMediaStream(streamId);
        mediaStream.addTrack(videoTrack);
        webRTCModule.localStreams.put(streamId, mediaStream);

        WritableMap trackInfo = Arguments.createMap();
        trackInfo.putString("id", trackId);
        trackInfo.putString("kind", videoTrack.kind());
        trackInfo.putString("readyState", "live");
        trackInfo.putBoolean("remote", false);
        trackInfo.putBoolean("enabled", videoTrack.enabled());
        // Same shape as the getUserMedia path, so track.getSettings() reports the
        // real dimensions (pool size for pooled tracks; 0x0 for forwarding tracks,
        // whose buffers carry their own size per frame).
        trackInfo.putMap("settings", captureController.getSettings());

        WritableMap data = Arguments.createMap();
        data.putString("streamId", streamId);
        data.putMap("track", trackInfo);

        Log.d(TAG,
                "createCustomVideoTrack streamId=" + streamId + " trackId=" + trackId
                        + (poolId != null ? " pooled" : " forwarding"));
        promise.resolve(data);
    }

    /**
     * Pushes one app frame into a custom video track. Routed from the JSI push channel
     * ({@link FJVideoPushInstaller}) synchronously on the caller's (worklet) thread. Resolves the
     * track's {@link CustomVideoCaptureController} from the thread-safe registry and hands it the
     * frame:
     * <ul>
     *   <li>{@code nativeBuffer != 0} -> forwarding: {@code pushExternalBuffer} takes an owning ref
     *       on the AHB before returning, then imports/delivers it on the GL thread.</li>
     *   <li>otherwise -> pooled: {@code pushFrame} imports the AHB at {@code bufferIndex}, waits the
     *       sync-fd fence in {@code fenceHandle}, and delivers.</li>
     * </ul>
     * {@code fenceSignaledValue} is unused on Android. Fire-and-forget.
     */
    void pushCustomVideoFrame(String trackId, int bufferIndex, long nativeBuffer, long fenceHandle,
            long fenceSignaledValue, long timestampNs, int rotation) {
        CustomVideoCaptureController controller = customVideoControllers.get(trackId);
        if (controller == null) {
            Log.w(TAG, "pushCustomVideoFrame: no custom video track for id " + trackId);
            return;
        }
        if (nativeBuffer != 0) {
            // Forwarding ignores any fence; if a raw-sink caller attached one, its fd
            // ownership transferred to native on push, so close it rather than leak it.
            CustomVideoFrameDelivery.closeFenceHandle(fenceHandle);
            controller.pushExternalBuffer(nativeBuffer, timestampNs, rotation);
        } else {
            controller.pushFrame(bufferIndex, fenceHandle, fenceSignaledValue, timestampNs, rotation);
        }
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
                List<VideoFrameProcessor> processors =
                        names.toArrayList()
                                .stream()
                                .filter(name -> name instanceof String)
                                .map(name -> {
                                    VideoFrameProcessor videoFrameProcessor =
                                            ProcessorProvider.getProcessor((String) name);
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
         * When true, {@link #surfaceTextureHelper} is the shared reusable camera STH owned by
         * {@link GetUserMediaImpl#reusableCameraSTH}.  Its EGL context must NOT be destroyed on
         * dispose — only {@code stopListening()} is called so the context can be reclaimed by the
         * next camera session.  Non-camera STHs (screen share, etc.) are not reused and are fully
         * disposed.
         */
        private final boolean reusableSTH;

        /**
         * Whether this object has been disposed or not.
         */
        private boolean disposed;

        public TrackPrivate(MediaStreamTrack track, MediaSource mediaSource,
                AbstractVideoCaptureController videoCaptureController, SurfaceTextureHelper surfaceTextureHelper) {
            this(track, mediaSource, videoCaptureController, surfaceTextureHelper, false);
        }

        public TrackPrivate(MediaStreamTrack track, MediaSource mediaSource,
                AbstractVideoCaptureController videoCaptureController, SurfaceTextureHelper surfaceTextureHelper,
                boolean reusableSTH) {
            this.track = track;
            this.mediaSource = mediaSource;
            this.videoCaptureController = videoCaptureController;
            this.surfaceTextureHelper = surfaceTextureHelper;
            this.reusableSTH = reusableSTH;
            this.disposed = false;
        }

        public void dispose() {
            if (!disposed) {
                /*
                 * Custom-video teardown ORDERING (GPU UAF guard). The hardware
                 * encoder samples the delivered OES texture / AHardwareBuffer on
                 * ITS OWN EGL context and may retain a VideoFrame past this call.
                 * So we MUST quiesce the encoder (dispose the VideoSource then the
                 * VideoTrack, which makes libwebrtc stop the encoder and release
                 * retained frames) BEFORE freeing the GL textures/EGLImages —
                 * otherwise the encoder samples a deleted texture. Required order:
                 *   stop accepting + drain delivery runnables
                 *     -> dispose VideoSource/VideoTrack (quiesce encoder)
                 *     -> free GL imports.
                 * releaseGpuResources() no longer frees the AHBs: in pooled mode
                 * they are owned by the CustomVideoBufferPool and freed by JS via
                 * releaseCustomVideoBufferPool (after this, once the OES textures
                 * aliasing them are gone); forwarding buffers are freed per frame by
                 * their VideoFrame release callback. The generic path below frees
                 * capturer resources before mediaSource/track, which is unsafe for
                 * this capturer-less track. (surfaceTextureHelper is always null for
                 * custom video.)
                 */
                if (videoCaptureController instanceof CustomVideoCaptureController) {
                    CustomVideoCaptureController customController =
                            (CustomVideoCaptureController) videoCaptureController;
                    customController.stopAccepting();
                    mediaSource.dispose();
                    track.dispose();
                    customController.releaseGpuResources();
                    disposed = true;
                    return;
                }

                if (videoCaptureController != null) {
                    if (videoCaptureController.stopCapture()) {
                        videoCaptureController.dispose();
                    }
                }

                /*
                 * As per webrtc library documentation - The caller still has ownership of {@code
                 * surfaceTextureHelper} and is responsible for making sure surfaceTextureHelper.dispose() is
                 * called. This also means that the caller can reuse the SurfaceTextureHelper to initialize a new
                 * VideoCapturer once the previous VideoCapturer has been disposed.
                 *
                 * For camera captures we use a single reusable STH (GetUserMediaImpl.reusableCameraSTH)
                 * so we only call stopListening() here — the EGL context is kept alive for the next
                 * getUserMedia call.  Full dispose() is only called for non-reusable STHs (screen share).
                 */
                if (surfaceTextureHelper != null) {
                    surfaceTextureHelper.stopListening();
                    if (!reusableSTH) {
                        surfaceTextureHelper.dispose();
                    }
                }

                mediaSource.dispose();
                track.dispose();
                disposed = true;
            }
        }
    }

    /**
     * Releases resources held by this instance.  Must be called when the owning
     * {@link WebRTCModule} is invalidated (e.g. React Native JS reload) so that
     * the reusable camera EGL context and its GL thread are not kept alive for
     * the rest of the process lifetime.
     *
     * <p>All active {@link TrackPrivate} entries are disposed first so no capturer
     * is left running against an already-disposed STH.
     */
    void dispose() {
        for (TrackPrivate track : tracks.values()) {
            track.dispose();
        }
        tracks.clear();
        customVideoControllers.clear();

        // Dispose any buffer pools JS never released (defensive; JS owns pool lifetime). Their
        // tracks were disposed above, so the GL imports aliasing these AHBs are already freed.
        for (CustomVideoBufferPool pool : customVideoBufferPools.values()) {
            pool.dispose();
        }
        customVideoBufferPools.clear();

        if (reusableCameraSTH != null) {
            reusableCameraSTH.stopListening();
            reusableCameraSTH.dispose();
            reusableCameraSTH = null;
        }
    }

    public interface BiConsumer<T, U> {
        void accept(T t, U u);
    }
}
