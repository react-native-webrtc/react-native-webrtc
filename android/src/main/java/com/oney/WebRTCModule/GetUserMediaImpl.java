package com.oney.WebRTCModule;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
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
    private static final String PERMISSION_AUDIO = Manifest.permission.RECORD_AUDIO;
    private static final String PERMISSION_VIDEO = Manifest.permission.CAMERA;

    /**
     * The {@link Log} tag with which {@code GetUserMediaImpl} is to log.
     */
    private static final String TAG = WebRTCModule.TAG;

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
     * the respective {@link Manifest.permission} value.
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

    private ReactApplicationContext getReactApplicationContext() {
        return reactContext;
    }

    MediaStreamTrack getTrack(String id) {
        TrackPrivate private_ = tracks.get(id);

        return private_ == null ? null : private_.track;
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

        String id = webRTCModule.getNextTrackUUID();
        PeerConnectionFactory pcFactory = webRTCModule.mFactory;
        AudioSource audioSource = pcFactory.createAudioSource(audioConstraints);
        AudioTrack track = pcFactory.createAudioTrack(id, audioSource);
        tracks.put(
            id,
            new TrackPrivate(track, audioSource, /* videoCapturer */ null));

        return track;
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
                     removeTrack(track.id());
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

        if (constraints.getType("video") == ReadableType.Map) {
            videoConstraintsMap = constraints.getMap("video");
        }

        Log.i(TAG, "getUserMedia(video): " + videoConstraintsMap);

        Context context = getReactApplicationContext();

        VideoCaptureController videoCaptureController
            = new VideoCaptureController(context, constraints);
        VideoCapturer videoCapturer = videoCaptureController.getVideoCapturer();
        if (videoCapturer == null) {
            return null;
        }

        PeerConnectionFactory pcFactory = webRTCModule.mFactory;
        VideoSource videoSource = pcFactory.createVideoSource(videoCapturer);

        String id = webRTCModule.getNextTrackUUID();
        VideoTrack track = pcFactory.createVideoTrack(id, videoSource);

        track.setEnabled(true);
        videoCaptureController.startCapture();

        tracks.put(id, new TrackPrivate(track, videoSource, videoCaptureController));

        return track;
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

    private void requestPermissions(
            final ArrayList<String> permissions,
            final Callback successCallback,
            final Callback errorCallback) {
        PermissionUtils.Callback callback = new PermissionUtils.Callback() {
            /**
             * The indicator which determines whether this
             * {@code PermissionUtils.Callback} has already been invoked.
             * Introduced in order to prevent multiple invocations of one and
             * the same instance of a react-native callback from native code
             * which is illegal and raises a fatal {@link RuntimeException}. The
             * rationale behind the introduction of the indicator is that asking
             * multiple times for one and the same permission is not as severe
             * an issue as killing the app's process. I don't see how it can
             * happen at all but Crashlytics says it has happened on at least
             * four different phone brands.
             */
            private boolean invoked = false;

            @Override
            public void invoke(String[] permissions_, int[] grantResults) {
                if (invoked) {
                    Log.w(
                        TAG,
                        "GetUserMediaImpl.PermissionUtils.Callback "
                            + "invoked more than once!");
                    return;
                }
                invoked = true;

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
