package com.oney.WebRTCModule;

import org.webrtc.VideoCapturer;

public abstract class AbstractVideoCaptureController {

    private final int width;
    private final int height;
    private final int fps;

    /**
     * {@link VideoCapturer} which this controller manages.
     */
    protected VideoCapturer videoCapturer;

    public AbstractVideoCaptureController(int width, int height, int fps) {
        this.width = width;
        this.height = height;
        this.fps = fps;
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
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getFrameRate() {
        return fps;
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

    protected abstract VideoCapturer createVideoCapturer();
}
