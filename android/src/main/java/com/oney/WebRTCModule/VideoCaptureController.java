package com.oney.WebRTCModule;

import android.util.Log;

import com.facebook.react.bridge.ReadableMap;

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

    private boolean isFrontFacing;

    /**
     * Values for width, height and fps (respectively) which will be
     * used to open the camera at.
     */
    private final int width;
    private final int height;
    private final int fps;

    private CameraEnumerator cameraEnumerator;

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

    public VideoCaptureController(CameraEnumerator cameraEnumerator, ReadableMap constraints) {
        this.cameraEnumerator = cameraEnumerator;

        width = constraints.getInt("width");
        height = constraints.getInt("height");
        fps = constraints.getInt("frameRate");

        String deviceId = ReactBridgeUtil.getMapStrValue(constraints, "deviceId");
        String facingMode = ReactBridgeUtil.getMapStrValue(constraints, "facingMode");

        videoCapturer = createVideoCapturer(deviceId, facingMode);
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
                    int newTries = tries-1;
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

        // If deviceId is specified, then it takes precedence over facingMode.
        if (deviceId != null) {
            for (String name : deviceNames) {
                if (name.equals(deviceId)) {
                    VideoCapturer videoCapturer
                        = cameraEnumerator.createCapturer(name, cameraEventsHandler);
                    String message = "Create user-specified camera " + name;
                    if (videoCapturer != null) {
                        Log.d(TAG, message + " succeeded");
                        this.isFrontFacing = cameraEnumerator.isFrontFacing(name);
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
            if (failedDevices.contains(name)) {
                continue;
            }
            try {
                // This can throw an exception when using the Camera 1 API.
                if (cameraEnumerator.isFrontFacing(name) != isFrontFacing) {
                    continue;
                }
            } catch (Exception e) {
                Log.e(
                    TAG,
                    "Failed to check the facing mode of camera " + name,
                    e);
                failedDevices.add(name);
                continue;
            }
            VideoCapturer videoCapturer
                = cameraEnumerator.createCapturer(name, cameraEventsHandler);
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
                VideoCapturer videoCapturer
                    = cameraEnumerator.createCapturer(name, cameraEventsHandler);
                String message = "Create fallback camera " + name;
                if (videoCapturer != null) {
                    Log.d(TAG, message + " succeeded");
                    this.isFrontFacing = cameraEnumerator.isFrontFacing(name);
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
}
