package com.oney.WebRTCModule;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.webrtc.*;

/**
 * The implementation of {@code getUserMedia} extracted into a separate file in
 * order to reduce complexity and to (somewhat) separate concerns.
 */
class GetUserMediaImpl {
    private static final int DEFAULT_WIDTH  = 1280;
    private static final int DEFAULT_HEIGHT = 720;
    private static final int DEFAULT_FPS    = 30;

    private static final String PERMISSION_AUDIO = Manifest.permission.RECORD_AUDIO;
    private static final String PERMISSION_VIDEO = Manifest.permission.CAMERA;

    /**
     * The {@link Log} tag with which {@code GetUserMediaImpl} is to log.
     */
    private static final String TAG = WebRTCModule.TAG;

    /**
     * The {@link CamearEventsHandler} used with
     * {@link CameraEnumerator#createCapturer}. Cached because the
     * implementation does not do anything but logging unspecific to the camera
     * device's name anyway.
     */
    private final CameraEventsHandler cameraEventsHandler
        = new CameraEventsHandler();

    private final ReactApplicationContext reactContext;

    /**
     * The application/library-specific private members of local
     * {@link VideoTrack}s created by {@code GetUserMediaImpl} mapped by track
     * ID.
     */
    private final Map<String, LocalVideoTrackPrivate> videoTracks
        = new HashMap<>();

    private final WebRTCModule webRTCModule;

    GetUserMediaImpl(
            WebRTCModule webRTCModule,
            ReactApplicationContext reactContext) {
        this.webRTCModule = webRTCModule;
        this.reactContext = reactContext;
    }

    /**
     * Includes default constraints set for the audio media type.
     *
     * @param audioConstraints {@code MediaConstraints} instance to be filled
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
     * Converts the value of a specific {@code MediaStreamConstraints} key to
     * the respective {@link Manifest#permission} value.
     *
     * @param constraints the {@code MediaStreamConstraints} within which the
     * specified {@code key} may be associated with the value to convert
     * @param key the key within the specified {@code constraints} which may be
     * associated with the value to convert
     * @param permissions the {@code List} of {@code Manifest.permission} values
     * to collect the result of the conversion
     */
    private void constraint2permission(
            ReadableMap constraints,
            String key,
            List<String> permissions) {
        if (constraints.hasKey(key)) {
            ReadableType type = constraints.getType(key);

            if (type == ReadableType.Boolean
                    ? constraints.getBoolean(key)
                    : type == ReadableType.Map) {
                if ("audio".equals(key)) {
                    permissions.add(PERMISSION_AUDIO);
                } else if ("video".equals(key)) {
                    permissions.add(PERMISSION_VIDEO);
                }
            }
        }
    }

    /**
     * Constructs a new {@code VideoCapturer} instance satisfying specific
     * constraints.
     *
     * @param enumerator a {@code CameraEnumerator} provided by WebRTC. It can
     * be {@code Camera1Enumerator} or {@code Camera2Enumerator}.
     * @param sourceId the ID of the requested video source. If not
     * {@code null} and a {@code VideoCapturer} can be created for it, then
     * {@code facingMode} is ignored.
     * @param facingMode the facing of the requested video source such as
     * {@code user} and {@code environment}. If {@code null}, "user" is
     * presumed.
     * @return a {@code VideoCapturer} satisfying th {@code facingMode} or
     * {@code sourceId} constraint
     */
    private VideoCapturer createVideoCapturer(
            CameraEnumerator enumerator,
            String sourceId,
            String facingMode) {
        String[] deviceNames = enumerator.getDeviceNames();

        // If sourceId is specified, then it takes precedence over facingMode.
        if (sourceId != null) {
            for (String name : deviceNames) {
                if (name.equals(sourceId)) {
                    VideoCapturer videoCapturer
                        = enumerator.createCapturer(name, cameraEventsHandler);
                    String message = "Create user-specified camera " + name;
                    if (videoCapturer != null) {
                        Log.d(TAG, message + " succeeded");
                        return videoCapturer;
                    } else {
                        Log.d(TAG, message + " failed");
                        break; // fallback to facingMode
                    }
                }
            }
        }

        // Otherwise, use facingMode.
        boolean isFrontFacing;
        if (facingMode == null) {
            facingMode = "user";
            isFrontFacing = true;
        } else {
            isFrontFacing = !facingMode.equals("environment");
        }
        for (String name : deviceNames) {
            if (enumerator.isFrontFacing(name) == isFrontFacing) {
                VideoCapturer videoCapturer
                    = enumerator.createCapturer(name, cameraEventsHandler);
                String message
                    = "Create " + facingMode + "-facing camera " + name;
                if (videoCapturer != null) {
                    Log.d(TAG, message + " succeeded");
                    return videoCapturer;
                } else {
                    Log.d(TAG, message + " failed");
                }
            }
        }

        // should we fallback to available camera automatically?
        return null;
    }

    /**
     * Retrieves "facingMode" constraint value.
     *
     * @param mediaConstraints a {@code ReadableMap} which represents "GUM"
     * constraints argument.
     * @return String value of "facingMode" constraints in "GUM" or
     * {@code null} if not specified.
     */
    private String getFacingMode(ReadableMap mediaConstraints) {
        return
            mediaConstraints == null
                ? null
                : ReactBridgeUtil.getMapStrValue(mediaConstraints, "facingMode");
    }

    private ReactApplicationContext getReactApplicationContext() {
        return reactContext;
    }

    /**
     * Retrieves "sourceId" constraint value.
     *
     * @param mediaConstraints a {@code ReadableMap} which represents "GUM"
     * constraints argument
     * @return String value of "sourceId" optional "GUM" constraint or
     * {@code null} if not specified.
     */
    private String getSourceIdConstraint(ReadableMap mediaConstraints) {
        if (mediaConstraints != null
                && mediaConstraints.hasKey("optional")
                && mediaConstraints.getType("optional") == ReadableType.Array) {
            ReadableArray optional = mediaConstraints.getArray("optional");

            for (int i = 0, size = optional.size(); i < size; i++) {
                if (optional.getType(i) == ReadableType.Map) {
                    ReadableMap option = optional.getMap(i);

                    if (option.hasKey("sourceId")
                            && option.getType("sourceId")
                                == ReadableType.String) {
                        return option.getString("sourceId");
                    }
                }
            }
        }

        return null;
    }

    private AudioTrack getUserAudio(ReadableMap constraints) {
        MediaConstraints audioConstraints;
        if (constraints.getType("audio") == ReadableType.Boolean) {
            audioConstraints = new MediaConstraints();
            addDefaultAudioConstraints(audioConstraints);
        } else {
            audioConstraints
                = webRTCModule.parseMediaConstraints(
                    constraints.getMap("audio"));
        }

        Log.i(TAG, "getUserMedia(audio): " + audioConstraints);

        String trackId = webRTCModule.getNextTrackUUID();
        PeerConnectionFactory pcFactory = webRTCModule.mFactory;
        AudioSource audioSource = pcFactory.createAudioSource(audioConstraints);

        return pcFactory.createAudioTrack(trackId, audioSource);
    }

    /**
     * Implements {@code getUserMedia} without knowledge whether the necessary
     * permissions have already been granted. If the necessary permissions have
     * not been granted yet, they will be requested.
     */
    void getUserMedia(
            final ReadableMap constraints,
            final Callback successCallback,
            final Callback errorCallback,
            final MediaStream mediaStream) {
        // TODO: change getUserMedia constraints format to support new syntax
        //   constraint format seems changed, and there is no mandatory any more.
        //   and has a new syntax/attrs to specify resolution
        //   should change `parseConstraints()` according
        //   see: https://www.w3.org/TR/mediacapture-streams/#idl-def-MediaTrackConstraints

        final ArrayList<String> requestPermissions = new ArrayList<>();

        constraint2permission(constraints, "audio", requestPermissions);
        constraint2permission(constraints, "video", requestPermissions);

        // According to step 2 of the getUserMedia() algorithm,
        // requestedMediaTypes is the set of media types in constraints with
        // either a dictionary value or a value of "true".
        // According to step 3 of the getUserMedia() algorithm, if
        // requestedMediaTypes is the empty set, the method invocation fails
        // with a TypeError.
        if (requestPermissions.isEmpty()) {
            errorCallback.invoke(
                "TypeError",
                "constraints requests no media types");
            return;
        }

        requestPermissions(
            requestPermissions,
            /* successCallback */ new Callback() {
                @Override
                public void invoke(Object... args) {
                    getUserMedia(
                        constraints,
                        successCallback,
                        errorCallback,
                        mediaStream,
                        /* grantedPermissions */ (List<String>) args[0]);
                }
            },
            /* errorCallback */ new Callback() {
                @Override
                public void invoke(Object... args) {
                    // According to step 10 Permission Failure of the
                    // getUserMedia() algorithm, if the user has denied
                    // permission, fail "with a new DOMException object whose
                    // name attribute has the value NotAllowedError."
                    errorCallback.invoke("DOMException", "NotAllowedError");
                }
            });
    }

    /**
     * Implements {@code getUserMedia} with the knowledge that the necessary
     * permissions have already been granted. If the necessary permissions have
     * not been granted yet, they will NOT be requested.
     */
    private void getUserMedia(
            ReadableMap constraints,
            Callback successCallback,
            Callback errorCallback,
            MediaStream mediaStream,
            List<String> grantedPermissions) {
        MediaStreamTrack[] tracks = new MediaStreamTrack[2];

        // If we fail to create either, destroy the other one and fail.
        if ((grantedPermissions.contains(PERMISSION_AUDIO)
                    && (tracks[0] = getUserAudio(constraints)) == null)
                || (grantedPermissions.contains(PERMISSION_VIDEO)
                    && (tracks[1] = getUserVideo(constraints)) == null)) {
             for (MediaStreamTrack track : tracks) {
                 if (track != null) {
                     track.dispose();
                 }
             }

             // XXX The following does not follow the getUserMedia() algorithm
             // specified by
             // https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-getusermedia
             // with respect to distinguishing the various causes of failure.
             errorCallback.invoke(
                 /* type */ null,
                 "Failed to create new track");
             return;
        }

        WritableArray tracks_ = Arguments.createArray();

        for (MediaStreamTrack track : tracks) {
            if (track == null) {
                continue;
            }

            String id = track.id();

            if (track instanceof AudioTrack) {
                mediaStream.addTrack((AudioTrack) track);
            } else {
                mediaStream.addTrack((VideoTrack) track);
            }
            webRTCModule.localTracks.put(id, track);

            WritableMap track_ = Arguments.createMap();
            String kind = track.kind();

            track_.putBoolean("enabled", track.enabled());
            track_.putString("id", id);
            track_.putString("kind", kind);
            track_.putString("label", kind);
            track_.putString("readyState", track.state().toString());
            track_.putBoolean("remote", false);
            tracks_.pushMap(track_);
        }

        String streamId = mediaStream.label();

        Log.d(TAG, "MediaStream id: " + streamId);
        webRTCModule.localStreams.put(streamId, mediaStream);

        successCallback.invoke(streamId, tracks_);
    }

    private VideoTrack getUserVideo(ReadableMap constraints) {
        ReadableMap videoConstraintsMap = null;
        ReadableMap videoConstraintsMandatory = null;
        if (constraints.getType("video") == ReadableType.Map) {
            videoConstraintsMap = constraints.getMap("video");
            if (videoConstraintsMap.hasKey("mandatory")
                    && videoConstraintsMap.getType("mandatory")
                        == ReadableType.Map) {
                videoConstraintsMandatory
                    = videoConstraintsMap.getMap("mandatory");
            }
        }

        Log.i(TAG, "getUserMedia(video): " + videoConstraintsMap);

        // NOTE: to support Camera2, the device should:
        //   1. Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        //   2. all camera support level should greater than LEGACY
        //   see: https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics.html#INFO_SUPPORTED_HARDWARE_LEVEL
        // TODO Enable camera2 enumerator
        Context context = getReactApplicationContext();
        CameraEnumerator cameraEnumerator;

        if (Camera2Enumerator.isSupported(context)) {
            Log.d(TAG, "Creating video capturer using Camera2 API.");
            cameraEnumerator = new Camera2Enumerator(context);
        } else {
            Log.d(TAG, "Creating video capturer using Camera1 API.");
            cameraEnumerator = new Camera1Enumerator(false);
        }

        String sourceId = getSourceIdConstraint(videoConstraintsMap);
        String facingMode = getFacingMode(videoConstraintsMap);
        VideoCapturer videoCapturer
            = createVideoCapturer(cameraEnumerator, sourceId, facingMode);
        if (videoCapturer == null) {
            return null;
        }

        PeerConnectionFactory pcFactory = webRTCModule.mFactory;
        VideoSource videoSource = pcFactory.createVideoSource(videoCapturer);

        // Fall back to defaults if keys are missing.
        int width
            = videoConstraintsMandatory.hasKey("minWidth")
                ? videoConstraintsMandatory.getInt("minWidth")
                : DEFAULT_WIDTH;
        int height
            = videoConstraintsMandatory.hasKey("minHeight")
                ? videoConstraintsMandatory.getInt("minHeight")
                : DEFAULT_HEIGHT;
        int fps
            = videoConstraintsMandatory.hasKey("minFrameRate")
                ? videoConstraintsMandatory.getInt("minFrameRate")
                : DEFAULT_FPS;
        try {
            videoCapturer.startCapture(width, height, fps);
        } catch (RuntimeException re) {
            // XXX PeerConnectionFactory#createVideoSource(videoCapturer) will
            // initialize videoCapturer. Unfortunately, the initialization may
            // be unsuccessful and VideoCapturer#startCapture(int, int, int) may
            // be able to unintentionally detect the failure.
            videoSource.dispose();
            videoCapturer.dispose();
            return null;
        }

        String trackId = webRTCModule.getNextTrackUUID();
        videoTracks.put(
            trackId,
            new LocalVideoTrackPrivate(videoCapturer, videoSource));

        return pcFactory.createVideoTrack(trackId, videoSource);
    }

    void removeVideoCapturer(String trackId) {
        LocalVideoTrackPrivate videoTrack = videoTracks.remove(trackId);
        if (videoTrack != null) {
            boolean captureStopped = false;
            try {
                videoTrack.videoCapturer.stopCapture();
                captureStopped = true;
            } catch (InterruptedException e) {
                Log.e(TAG, "removeVideoCapturer() Failed to stop video capturer");
            }
            if (captureStopped) {
                videoTrack.videoSource.dispose();
                videoTrack.videoCapturer.dispose();
            }
        }
    }

    private void requestPermissions(
            final ArrayList<String> permissions,
            final Callback successCallback,
            final Callback errorCallback) {
        PermissionUtils.Callback callback = new PermissionUtils.Callback() {
            @Override
            public void invoke(String[] permissions_, int[] grantResults) {
                List<String> grantedPermissions = new ArrayList<>();
                List<String> deniedPermissions = new ArrayList<>();

                for (int i = 0; i < permissions_.length; ++i) {
                    String permission = permissions_[i];
                    int grantResult = grantResults[i];

                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        grantedPermissions.add(permission);
                    } else {
                        deniedPermissions.add(permission);
                    }
                }

                // Success means that all requested permissions were granted.
                for (String p : permissions) {
                    if (!grantedPermissions.contains(p)) {
                        // According to step 6 of the getUserMedia() algorithm
                        // "if the result is denied, jump to the step Permission
                        // Failure."
                        errorCallback.invoke(deniedPermissions);
                        return;
                    }
                }
                successCallback.invoke(grantedPermissions);
            }
        };

        PermissionUtils.requestPermissions(
            getReactApplicationContext(),
            permissions.toArray(new String[permissions.size()]),
            callback);
    }

    void switchCamera(String trackId) {
        LocalVideoTrackPrivate videoTrack = videoTracks.get(trackId);
        if (videoTrack != null) {
            ((CameraVideoCapturer) videoTrack.videoCapturer).switchCamera(null);
        }
    }

    /**
     * Application/library-specific private members of local {@code VideoTrack}s
     * created by {@code GetUserMediaImpl}.
     */
    private static class LocalVideoTrackPrivate {
        public final VideoCapturer videoCapturer;

        /**
         * The {@code VideoSource} created from {@link #videoCapturer}.
         */
        public final VideoSource videoSource;

        /**
         * Initializes a new {@code LocalVideoTrackPrivate} instance.
         *
         * @param videoCapturer
         * @param videoSource the {@code VideoSource} created from the specified
         * {@code videoCapturer}
         */
        public LocalVideoTrackPrivate(
                VideoCapturer videoCapturer,
                VideoSource videoSource) {
            this.videoCapturer = videoCapturer;
            this.videoSource = videoSource;
        }
    }
}
