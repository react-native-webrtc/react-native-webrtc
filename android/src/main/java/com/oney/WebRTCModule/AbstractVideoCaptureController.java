package com.oney.WebRTCModule;

import org.webrtc.VideoCapturer;

public abstract class AbstractVideoCaptureController {
    protected final int targetWidth;
    protected final int targetHeight;
    protected final int targetFps;

    protected int actualWidth;
    protected int actualHeight;
    protected int actualFps;

    /**
     * {@link VideoCapturer} which this controller manages.
     */
    protected VideoCapturer videoCapturer;

    protected CapturerEventsListener capturerEventsListener;

    public AbstractVideoCaptureController(int width, int height, int fps) {
        this.targetWidth = width;
        this.targetHeight = height;
        this.targetFps = fps;
        this.actualWidth = width;
        this.actualHeight = height;
        this.actualFps = fps;
    }

    public void initializeVideoCapturer() {
        videoCapturer = createVideoCapturer();
    }

    public void dispose() {
        if (videoCapturer != null) {
            videoCapturer.dispose();
            videoCapturer = null;
        }
    }

    public int getHeight() {
        return actualHeight;
    }

    public int getWidth() {
        return actualWidth;
    }

    public int getFrameRate() {
        return actualFps;
    }

    public VideoCapturer getVideoCapturer() {
        return videoCapturer;
    }

    public void startCapture() {
        try {
            videoCapturer.startCapture(targetWidth, targetHeight, targetFps);
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

    public void setCapturerEventsListener(CapturerEventsListener listener) {
        this.capturerEventsListener = listener;
    }

    protected abstract VideoCapturer createVideoCapturer();

    public interface CapturerEventsListener {
        /** Called when the capturer is ended and in an irrecoverable state. */
        public void onCapturerEnded();
    }
}
