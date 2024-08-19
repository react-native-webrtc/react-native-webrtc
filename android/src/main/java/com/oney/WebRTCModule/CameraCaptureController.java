package com.oney.WebRTCModule;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.Nullable;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.facebook.react.bridge.ReadableMap;

import org.webrtc.Camera1Capturer;
import org.webrtc.Camera1Helper;
import org.webrtc.Camera2Capturer;
import org.webrtc.Camera2Helper;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.Size;
import org.webrtc.VideoCapturer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CameraCaptureController extends AbstractVideoCaptureController {
    /**
     * The {@link Log} tag with which {@code CameraCaptureController} is to log.
     */
    private static final String TAG = CameraCaptureController.class.getSimpleName();

    private boolean isFrontFacing;
    @Nullable
    private String currentDeviceId = null;

    private final Context context;
    private final CameraEnumerator cameraEnumerator;
    private ReadableMap constraints;

    /**
     * The {@link CameraEventsHandler} used with
     * {@link CameraEnumerator#createCapturer}.
     */
    private final CameraEventsHandler cameraEventsHandler = new CameraEventsHandler() {

        @Override
        public void onCameraOpening(String cameraName) {
            super.onCameraOpening(cameraName);
            updateActualSize(cameraName, videoCapturer);
            CameraCaptureController.this.currentDeviceId = findDeviceId(cameraName);
        }
    };

    public CameraCaptureController(Context context, CameraEnumerator cameraEnumerator, ReadableMap constraints) {
        super(constraints.getInt("width"), constraints.getInt("height"), constraints.getInt("frameRate"));

        this.context = context;
        this.cameraEnumerator = cameraEnumerator;
        this.constraints = constraints;
    }

    @Nullable
    @Override
    public String getDeviceId() {
        return currentDeviceId;
    }

    private String findDeviceId(String cameraName) {
        String[] deviceNames = cameraEnumerator.getDeviceNames();
        for (int i = 0; i < deviceNames.length; i++) {
            if (Objects.equals(deviceNames[i], cameraName)) {
                return String.valueOf(i);
            }
        }
        return null;
    }

    public void applyConstraints(ReadableMap constraints, Consumer<Exception> onFinishedCallback) {
        ReadableMap oldConstraints = this.constraints;
        int oldTargetWidth = this.targetWidth;
        int oldTargetHeight = this.targetHeight;
        int oldTargetFps = this.targetFps;

        // Don't save constraints yet, since we may fail to find a fit.
        Runnable saveConstraints = () -> {
            this.constraints = constraints;
            this.targetWidth = constraints.getInt("width");
            this.targetHeight = constraints.getInt("height");
            this.targetFps = constraints.getInt("frameRate");
        };

        if (videoCapturer == null) {
            // No existing capturer, just let it initialize normally.
            saveConstraints.run();
            onFinishedCallback.accept(null);
            return;
        }

        // Find target camera to switch to.
        String[] deviceNames = cameraEnumerator.getDeviceNames();
        final String deviceId = ReactBridgeUtil.getMapStrValue(constraints, "deviceId");
        final String facingMode = ReactBridgeUtil.getMapStrValue(constraints, "facingMode");
        final boolean isFrontFacing = facingMode == null || !facingMode.equals("environment");
        int cameraIndex = -1;
        String cameraName = null;

        // If deviceId is specified, then it takes precedence over facingMode.
        if (deviceId != null) {
            try {
                cameraIndex = Integer.parseInt(deviceId);
                cameraName = deviceNames[cameraIndex];
            } catch (Exception e) {
                Log.d(TAG, "failed to find device with id: " + deviceId);
            }
        }


        // Otherwise, use facingMode (defaulting to front/user facing).
        if (cameraName == null) {
            cameraIndex = -1;
            for (String name : deviceNames) {
                cameraIndex++;
                if (cameraEnumerator.isFrontFacing(name) == isFrontFacing) {
                    cameraName = name;
                    break;
                }
            }
        }

        if (cameraName == null) {
            onFinishedCallback.accept(new Exception("OverconstrainedError: could not find camera with deviceId: " + deviceId + " or facingMode: " + facingMode));
            return;
        }
        
        final String finalCameraName = cameraName; // For lambda reference
        boolean shouldSwitchCamera = false;
        try {
            int currentCameraIndex = Integer.parseInt(currentDeviceId);
            shouldSwitchCamera = cameraIndex != currentCameraIndex;
        } catch (Exception e) {
            shouldSwitchCamera = true;
            Log.d(TAG, "Forcing camera switch, couldn't parse current device id: " + currentDeviceId);
        }

        CameraVideoCapturer capturer = (CameraVideoCapturer) videoCapturer;
        Runnable changeFormatIfNeededAndFinish = () -> {
            saveConstraints.run();
            if (targetWidth != oldTargetWidth ||
                    targetHeight != oldTargetHeight ||
                    targetFps != oldTargetFps) {
                updateActualSize(finalCameraName, videoCapturer);
                capturer.changeCaptureFormat(targetWidth, targetHeight, targetFps);
            }
            if (onFinishedCallback != null) {
                onFinishedCallback.accept(null);
            }
        };

        if (shouldSwitchCamera) {
            capturer.switchCamera(new CameraVideoCapturer.CameraSwitchHandler() {
                @Override
                public void onCameraSwitchDone(boolean isFrontCamera) {
                    CameraCaptureController.this.isFrontFacing = isFrontCamera;
                    changeFormatIfNeededAndFinish.run();
                }

                @Override
                public void onCameraSwitchError(String s) {
                    Exception e = new Exception("Error switching camera: " + s);
                    Log.e(TAG, "OnCameraSwitchError", e);
                    if(onFinishedCallback != null) {
                        onFinishedCallback.accept(e);
                    }
                }
            }, cameraName);
        } else {
            // No camera switch needed, just change format if needed.
            changeFormatIfNeededAndFinish.run();
        }
    }

    /**
     * Use applyConstraints instead.
     * @deprecated
     */
    @Deprecated
    public void switchCamera(Consumer<Exception> onFinishedCallback) {
        if (videoCapturer instanceof CameraVideoCapturer) {
            CameraVideoCapturer capturer = (CameraVideoCapturer) videoCapturer;
            String[] deviceNames = cameraEnumerator.getDeviceNames();
            int deviceCount = deviceNames.length;

            // Nothing to switch to.
            if (deviceCount < 2) {
                return;
            }

            // The usual case.
            if (deviceCount == 2) {
                capturer.switchCamera(new CameraVideoCapturer.CameraSwitchHandler() {
                    @Override
                    public void onCameraSwitchDone(boolean isFrontCamera) {
                        isFrontFacing = isFrontCamera;
                        if(onFinishedCallback != null) {
                            onFinishedCallback.accept(null);
                        }
                    }

                    @Override
                    public void onCameraSwitchError(String s) {
                        Exception e = new Exception("Error switching camera: " + s);
                        Log.e(TAG, "OnCameraSwitchError", e);
                        if(onFinishedCallback != null) {
                            onFinishedCallback.accept(e);
                        }
                    }
                });
                return;
            }

            // If we are here the device has more than 2 cameras. Cycle through them
            // and switch to the first one of the desired facing mode.
            switchCamera(!isFrontFacing, deviceCount, onFinishedCallback);
        }
    }

    @Override
    protected VideoCapturer createVideoCapturer() {
        String deviceId = ReactBridgeUtil.getMapStrValue(this.constraints, "deviceId");
        String facingMode = ReactBridgeUtil.getMapStrValue(this.constraints, "facingMode");

        Pair<String, VideoCapturer> result = createVideoCapturer(deviceId, facingMode);
        if(result == null) {
            return null;
        }

        String cameraName = result.first;
        VideoCapturer videoCapturer = result.second;

        updateActualSize(cameraName, videoCapturer);

        return videoCapturer;
    }

    private void updateActualSize(String cameraName, VideoCapturer videoCapturer) {
        // Find actual capture format.
        Size actualSize = null;
        if (videoCapturer instanceof Camera1Capturer) {
            int cameraId = Camera1Helper.getCameraId(cameraName);
            actualSize = Camera1Helper.findClosestCaptureFormat(cameraId, targetWidth, targetHeight);
        } else if (videoCapturer instanceof Camera2Capturer) {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            actualSize = Camera2Helper.findClosestCaptureFormat(cameraManager, cameraName, targetWidth, targetHeight);
        }

        if (actualSize != null) {
            actualWidth = actualSize.width;
            actualHeight = actualSize.height;
        }
    }

    /**
     * Helper function which tries to switch cameras until the desired facing mode is found.
     *
     * @param desiredFrontFacing - The desired front facing value.
     * @param tries - How many times to try switching.
     */
    private void switchCamera(boolean desiredFrontFacing, int tries, Consumer<Exception> onFinishedCallback) {
        CameraVideoCapturer capturer = (CameraVideoCapturer) videoCapturer;

        capturer.switchCamera(new CameraVideoCapturer.CameraSwitchHandler() {
            @Override
            public void onCameraSwitchDone(boolean b) {
                if (b != desiredFrontFacing) {
                    int newTries = tries - 1;
                    if (newTries > 0) {
                        switchCamera(desiredFrontFacing, newTries, onFinishedCallback);
                    }
                } else {
                    isFrontFacing = desiredFrontFacing;
                    if(onFinishedCallback != null) {
                        onFinishedCallback.accept(null);
                    }
                }
            }

            @Override
            public void onCameraSwitchError(String s) {
                Exception e = new Exception("Error switching camera: " + s);
                Log.e(TAG, "OnCameraSwitchError", e);
                if(onFinishedCallback != null) {
                    onFinishedCallback.accept(e);
                }
            }
        });
    }

    /**
     * Constructs a new {@code VideoCapturer} instance attempting to satisfy
     * specific constraints.
     *
     * @param deviceId the ID of the requested video device. If not
     * {@code null} and a {@code VideoCapturer} can be created for it, then
     * {@code facingMode} is ignored.
     * @param facingMode the facing of the requested video source such as
     * {@code user} and {@code environment}. If {@code null}, "user" is
     * presumed.
     * @return a pair containing the deviceId and {@code VideoCapturer} satisfying the {@code facingMode} or
     * {@code deviceId} constraint, or null.
     */
    @Nullable
    private Pair<String, VideoCapturer> createVideoCapturer(String deviceId, String facingMode) {
        String[] deviceNames = cameraEnumerator.getDeviceNames();
        List<String> failedDevices = new ArrayList<>();

        String cameraName = null;
        int cameraIndex = 0;
        try {
            cameraIndex = Integer.parseInt(deviceId);
            cameraName = deviceNames[cameraIndex];
        } catch (Exception e) {
            Log.d(TAG, "failed to find device with id: " + deviceId);
        }

        // If deviceId is specified, then it takes precedence over facingMode.
        if (cameraName != null) {
            VideoCapturer videoCapturer = cameraEnumerator.createCapturer(cameraName, cameraEventsHandler);
            String message = "Create user-specified camera " + cameraName;
            if (videoCapturer != null) {
                Log.d(TAG, message + " succeeded");
                this.isFrontFacing = cameraEnumerator.isFrontFacing(cameraName);
                this.currentDeviceId = String.valueOf(cameraIndex);
                return new Pair(cameraName, videoCapturer);
            } else {
                // fallback to facingMode
                Log.d(TAG, message + " failed");
                failedDevices.add(cameraName);
            }
        }

        // Otherwise, use facingMode (defaulting to front/user facing).
        final boolean isFrontFacing = facingMode == null || !facingMode.equals("environment");
        cameraIndex = -1;
        for (String name : deviceNames) {
            cameraIndex++;
            if (failedDevices.contains(name)) {
                continue;
            }
            if (cameraEnumerator.isFrontFacing(name) != isFrontFacing) {
                continue;
            }
            VideoCapturer videoCapturer = cameraEnumerator.createCapturer(name, cameraEventsHandler);
            String message = "Create camera " + name;
            if (videoCapturer != null) {
                Log.d(TAG, message + " succeeded");
                this.isFrontFacing = cameraEnumerator.isFrontFacing(name);
                this.currentDeviceId = String.valueOf(cameraIndex);
                return new Pair(name, videoCapturer);
            } else {
                Log.d(TAG, message + " failed");
                failedDevices.add(name);
            }
        }

        cameraIndex = -1;
        // Fallback to any available camera.
        for (String name : deviceNames) {
            cameraIndex++;
            if (!failedDevices.contains(name)) {
                VideoCapturer videoCapturer = cameraEnumerator.createCapturer(name, cameraEventsHandler);
                String message = "Create fallback camera " + name;
                if (videoCapturer != null) {
                    Log.d(TAG, message + " succeeded");
                    this.isFrontFacing = cameraEnumerator.isFrontFacing(name);
                    this.currentDeviceId = String.valueOf(cameraIndex);
                    return new Pair(name, videoCapturer);
                } else {
                    failedDevices.add(name);
                }
            }
        }

        currentDeviceId = null;
        Log.w(TAG, "Unable to identify a suitable camera.");

        return null;
    }
}
