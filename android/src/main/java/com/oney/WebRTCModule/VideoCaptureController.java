package com.oney.WebRTCModule;

import android.content.Context;
import android.util.Log;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.VideoCapturer;

import java.util.ArrayList;
import java.util.List;

public class VideoCaptureController {
    /**
     * The {@link Log} tag with which {@code VideoCaptureController} is to log.
     */
    private static final String TAG
        = VideoCaptureController.class.getSimpleName();

    /**
     * Default values for width, height and fps (respectively) which will be
     * used to open the camera at.
     */
    private static final int DEFAULT_WIDTH  = 1280;
    private static final int DEFAULT_HEIGHT = 720;
    private static final int DEFAULT_FPS    = 30;

    /**
     * Values for width, height and fps (respectively) which will be
     * used to open the camera at.
     */
    private int width  = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;
    private int fps    = DEFAULT_FPS;

    /**
     * The {@link CameraEventsHandler} used with
     * {@link CameraEnumerator#createCapturer}. Cached because the
     * implementation does not do anything but logging unspecific to the camera
     * device's name anyway.
     */
    private final CameraEventsHandler cameraEventsHandler
        = new CameraEventsHandler();

    /**
     * {@link VideoCapturer} which this controller manages.
     */
    private VideoCapturer videoCapturer;

    public VideoCaptureController(Context context, ReadableMap constraints) {
        ReadableMap videoConstraintsMandatory = null;

        if (constraints.hasKey("mandatory")
                && constraints.getType("mandatory") == ReadableType.Map) {
            videoConstraintsMandatory = constraints.getMap("mandatory");
        }

        // NOTE: to support Camera2, the device should:
        //   1. Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        //   2. all camera support level should greater than LEGACY
        //   see: https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics.html#INFO_SUPPORTED_HARDWARE_LEVEL
        CameraEnumerator cameraEnumerator;

        if (Camera2Enumerator.isSupported(context)) {
            Log.d(TAG, "Creating video capturer using Camera2 API.");
            cameraEnumerator = new Camera2Enumerator(context);
        } else {
            Log.d(TAG, "Creating video capturer using Camera1 API.");
            cameraEnumerator = new Camera1Enumerator(false);
        }

        String sourceId = getSourceIdConstraint(constraints);
        String facingMode = getFacingMode(constraints);

        videoCapturer
            = createVideoCapturer(cameraEnumerator, sourceId, facingMode);

        if (videoConstraintsMandatory != null) {
            width = videoConstraintsMandatory.hasKey("minWidth")
                ? videoConstraintsMandatory.getInt("minWidth")
                : DEFAULT_WIDTH;
            height = videoConstraintsMandatory.hasKey("minHeight")
                ? videoConstraintsMandatory.getInt("minHeight")
                : DEFAULT_HEIGHT;
            fps = videoConstraintsMandatory.hasKey("minFrameRate")
                ? videoConstraintsMandatory.getInt("minFrameRate")
                : DEFAULT_FPS;
        }
    }

    public void dispose() {
        if (videoCapturer != null) {
            videoCapturer.dispose();
            videoCapturer = null;
        }
    }

    public VideoCapturer getVideoCapturer() {
        return videoCapturer;
    }

    public void startCapture() {
        try {
            videoCapturer.startCapture(width, height, fps);
        } catch (RuntimeException e) {
            // XXX This can only fail if we initialize the capturer incorrectly,
            // which we don't. Thus, ignore any failures here since we trust
            // ourselves.
        }
    }

    public boolean stopCapture() {
        try {
            videoCapturer.stopCapture();
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void switchCamera() {
        if (videoCapturer instanceof CameraVideoCapturer) {
            ((CameraVideoCapturer) videoCapturer).switchCamera(null);
        }
    }

    /**
     * Constructs a new {@code VideoCapturer} instance attempting to satisfy
     * specific constraints.
     *
     * @param enumerator a {@code CameraEnumerator} provided by WebRTC. It can
     * be {@code Camera1Enumerator} or {@code Camera2Enumerator}.
     * @param sourceId the ID of the requested video source. If not
     * {@code null} and a {@code VideoCapturer} can be created for it, then
     * {@code facingMode} is ignored.
     * @param facingMode the facing of the requested video source such as
     * {@code user} and {@code environment}. If {@code null}, "user" is
     * presumed.
     * @return a {@code VideoCapturer} satisfying the {@code facingMode} or
     * {@code sourceId} constraint
     */
    private VideoCapturer createVideoCapturer(
            CameraEnumerator enumerator,
            String sourceId,
            String facingMode) {
        String[] deviceNames = enumerator.getDeviceNames();
        List<String> failedDevices = new ArrayList<>();

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
                        failedDevices.add(name);
                        break; // fallback to facingMode
                    }
                }
            }
        }

        // Otherwise, use facingMode (defaulting to front/user facing).
        final boolean isFrontFacing
            = facingMode == null || !facingMode.equals("environment");
        for (String name : deviceNames) {
            if (!failedDevices.contains(name)
                    && enumerator.isFrontFacing(name) == isFrontFacing) {
                VideoCapturer videoCapturer
                    = enumerator.createCapturer(name, cameraEventsHandler);
                String message
                    = "Create camera " + name;
                if (videoCapturer != null) {
                    Log.d(TAG, message + " succeeded");
                    return videoCapturer;
                } else {
                    Log.d(TAG, message + " failed");
                    failedDevices.add(name);
                }
            }
        }

        // Fallback to any available camera.
        for (String name : deviceNames) {
            if (!failedDevices.contains(name)) {
                VideoCapturer videoCapturer
                    = enumerator.createCapturer(name, cameraEventsHandler);
                String message = "Create fallback camera " + name;
                if (videoCapturer != null) {
                    Log.d(TAG, message + " succeeded");
                    return videoCapturer;
                } else {
                    Log.d(TAG, message + " failed");
                    failedDevices.add(name);
                    // fallback to the next device.
                }
            }
        }

        Log.w(TAG, "Unable to identify a suitable camera.");

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
}
