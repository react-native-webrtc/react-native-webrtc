package com.oney.WebRTCModule;

import android.util.Log;

import com.facebook.react.bridge.ReadableMap;

import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.VideoCapturer;

import java.util.ArrayList;
import java.util.List;

public class CameraCaptureController extends AbstractVideoCaptureController {
    /**
     * The {@link Log} tag with which {@code CameraCaptureController} is to log.
     */
    private static final String TAG = CameraCaptureController.class.getSimpleName();

    private boolean isFrontFacing;

    private final CameraEnumerator cameraEnumerator;
    private final ReadableMap constraints;

    /**
     * The {@link CameraEventsHandler} used with
     * {@link CameraEnumerator#createCapturer}. Cached because the
     * implementation does not do anything but logging unspecific to the camera
     * device's name anyway.
     */
    private final CameraEventsHandler cameraEventsHandler = new CameraEventsHandler();

    public CameraCaptureController(CameraEnumerator cameraEnumerator, ReadableMap constraints) {
        super(constraints.getInt("width"), constraints.getInt("height"), constraints.getInt("frameRate"));

        this.cameraEnumerator = cameraEnumerator;
        this.constraints = constraints;
    }

    public void switchCamera() {
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
                    public void onCameraSwitchDone(boolean b) {
                        isFrontFacing = b;
                    }

                    @Override
                    public void onCameraSwitchError(String s) {
                        Log.e(TAG, "Error switching camera: " + s);
                    }
                });
                return;
            }

            // If we are here the device has more than 2 cameras. Cycle through them
            // and switch to the first one of the desired facing mode.
            switchCamera(!isFrontFacing, deviceCount);
        }
    }

    @Override
    protected VideoCapturer createVideoCapturer() {
        String deviceId = ReactBridgeUtil.getMapStrValue(this.constraints, "deviceId");
        String facingMode = ReactBridgeUtil.getMapStrValue(this.constraints, "facingMode");

        return createVideoCapturer(deviceId, facingMode);
    }

    /**
     * Helper function which tries to switch cameras until the desired facing mode is found.
     *
     * @param desiredFrontFacing - The desired front facing value.
     * @param tries - How many times to try switching.
     */
    private void switchCamera(boolean desiredFrontFacing, int tries) {
        CameraVideoCapturer capturer = (CameraVideoCapturer) videoCapturer;

        capturer.switchCamera(new CameraVideoCapturer.CameraSwitchHandler() {
            @Override
            public void onCameraSwitchDone(boolean b) {
                if (b != desiredFrontFacing) {
                    int newTries = tries - 1;
                    if (newTries > 0) {
                        switchCamera(desiredFrontFacing, newTries);
                    }
                } else {
                    isFrontFacing = desiredFrontFacing;
                }
            }

            @Override
            public void onCameraSwitchError(String s) {
                Log.e(TAG, "Error switching camera: " + s);
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
     * @return a {@code VideoCapturer} satisfying the {@code facingMode} or
     * {@code deviceId} constraint
     */
    private VideoCapturer createVideoCapturer(String deviceId, String facingMode) {
        String[] deviceNames = cameraEnumerator.getDeviceNames();
        List<String> failedDevices = new ArrayList<>();

        String cameraName = null;
        try {
            int index = Integer.parseInt(deviceId);
            cameraName = deviceNames[index];
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
                return videoCapturer;
            } else {
                // fallback to facingMode
                Log.d(TAG, message + " failed");
                failedDevices.add(cameraName);
            }
        }

        // Otherwise, use facingMode (defaulting to front/user facing).
        final boolean isFrontFacing = facingMode == null || !facingMode.equals("environment");
        for (String name : deviceNames) {
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
                return videoCapturer;
            } else {
                Log.d(TAG, message + " failed");
                failedDevices.add(name);
            }
        }

        // Fallback to any available camera.
        for (String name : deviceNames) {
            if (!failedDevices.contains(name)) {
                VideoCapturer videoCapturer = cameraEnumerator.createCapturer(name, cameraEventsHandler);
                String message = "Create fallback camera " + name;
                if (videoCapturer != null) {
                    Log.d(TAG, message + " succeeded");
                    this.isFrontFacing = cameraEnumerator.isFrontFacing(name);
                    return videoCapturer;
                } else {
                    Log.d(TAG, message + " failed");
                    failedDevices.add(name);
                }
            }
        }

        Log.w(TAG, "Unable to identify a suitable camera.");

        return null;
    }
}
