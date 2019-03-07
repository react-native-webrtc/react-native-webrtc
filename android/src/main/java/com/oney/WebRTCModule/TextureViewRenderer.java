/*
 *  Copyright 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.oney.WebRTCModule;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;
import org.webrtc.EglBase;
import org.webrtc.EglRenderer;
import org.webrtc.GlRectDrawer;
import org.webrtc.Logging;
import org.webrtc.RendererCommon;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoFrame;
import java.util.concurrent.CountDownLatch;

/**
 * Implements org.webrtc.VideoRenderer.Callbacks by displaying the video stream on a SurfaceView.
 * renderFrame() is asynchronous to avoid blocking the calling thread.
 * This class is thread safe and handles access from potentially four different threads:
 * Interaction from the main app in init, release, setMirror, and setScalingtype.
 * Interaction from C++ rtc::VideoSinkInterface in renderFrame.
 * Interaction from the Activity lifecycle in surfaceCreated, surfaceChanged, and surfaceDestroyed.
 * Interaction with the layout framework in onMeasure and onSizeChanged.
 */
public class TextureViewRenderer
        extends TextureView implements TextureView.SurfaceTextureListener, IRenderer {
    private static final String TAG = "SurfaceViewRenderer";

    // Cached resource name.
    private final String resourceName;
    private final RendererCommon.VideoLayoutMeasure videoLayoutMeasure =
            new RendererCommon.VideoLayoutMeasure();
    private final EglRenderer eglRenderer;

    // Callback for reporting renderer events. Read-only after initilization so no lock required.
    private RendererCommon.RendererEvents rendererEvents;

    private final Object layoutLock = new Object();
    private boolean isRenderingPaused = false;
    private boolean isFirstFrameRendered;
    private int rotatedFrameWidth;
    private int rotatedFrameHeight;
    private int frameRotation;

    /**
     * Standard View constructor. In order to render something, you must first call init().
     */
    public TextureViewRenderer(Context context) {
        super(context);
        this.resourceName = getResourceName();
        eglRenderer = new EglRenderer(resourceName);
        setSurfaceTextureListener(this);
    }

    /**
     * Standard View constructor. In order to render something, you must first call init().
     */
    public TextureViewRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.resourceName = getResourceName();
        eglRenderer = new EglRenderer(resourceName);
        setSurfaceTextureListener(this);
    }

    /**
     * Initialize this class, sharing resources with |sharedContext|. It is allowed to call init() to
     * reinitialize the renderer after a previous init()/release() cycle.
     */
    public void init(EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents) {
        init(sharedContext, rendererEvents, EglBase.CONFIG_PLAIN, new GlRectDrawer());
    }

    /**
     * Initialize this class, sharing resources with |sharedContext|. The custom |drawer| will be used
     * for drawing frames on the EGLSurface. This class is responsible for calling release() on
     * |drawer|. It is allowed to call init() to reinitialize the renderer after a previous
     * init()/release() cycle.
     */
    public void init(final EglBase.Context sharedContext,
                     RendererCommon.RendererEvents rendererEvents, final int[] configAttributes,
                     RendererCommon.GlDrawer drawer) {
        ThreadUtils.checkIsOnMainThread();
        this.rendererEvents = rendererEvents;
        synchronized (layoutLock) {
            isFirstFrameRendered = false;
            rotatedFrameWidth = 0;
            rotatedFrameHeight = 0;
            frameRotation = 0;
        }
        eglRenderer.init(sharedContext, configAttributes, drawer);
    }

    /**
     * Block until any pending frame is returned and all GL resources released, even if an interrupt
     * occurs. If an interrupt occurs during release(), the interrupt flag will be set. This function
     * should be called before the Activity is destroyed and the EGLContext is still valid. If you
     * don't call this function, the GL resources might leak.
     */
    public void release() {
        eglRenderer.release();
    }

    /**
     * Register a callback to be invoked when a new video frame has been received.
     *
     * @param listener The callback to be invoked. The callback will be invoked on the render thread.
     *                 It should be lightweight and must not call removeFrameListener.
     * @param scale    The scale of the Bitmap passed to the callback, or 0 if no Bitmap is
     *                 required.
     * @param drawer   Custom drawer to use for this frame listener.
     */
    public void addFrameListener(
            EglRenderer.FrameListener listener, float scale, RendererCommon.GlDrawer drawerParam) {
        eglRenderer.addFrameListener(listener, scale, drawerParam);
    }

    /**
     * Register a callback to be invoked when a new video frame has been received. This version uses
     * the drawer of the EglRenderer that was passed in init.
     *
     * @param listener The callback to be invoked. The callback will be invoked on the render thread.
     *                 It should be lightweight and must not call removeFrameListener.
     * @param scale    The scale of the Bitmap passed to the callback, or 0 if no Bitmap is
     *                 required.
     */
    public void addFrameListener(EglRenderer.FrameListener listener, float scale) {
        eglRenderer.addFrameListener(listener, scale);
    }

    public void removeFrameListener(EglRenderer.FrameListener listener) {
        eglRenderer.removeFrameListener(listener);
    }

    /**
     * Set if the video stream should be mirrored or not.
     */
    public void setMirror(final boolean mirror) {
        eglRenderer.setMirror(mirror);
    }

    @Override
    public void setZOrderMediaOverlay(boolean isMediaOverlay) {

    }

    @Override
    public void setZOrderOnTop(boolean onTop) {

    }


    /**
     * Set how the video will fill the allowed layout area.
     */
    public void setScalingType(RendererCommon.ScalingType scalingType) {
        ThreadUtils.checkIsOnMainThread();
        videoLayoutMeasure.setScalingType(scalingType);
        requestLayout();
    }

    public void setScalingType(RendererCommon.ScalingType scalingTypeMatchOrientation,
                               RendererCommon.ScalingType scalingTypeMismatchOrientation) {
        ThreadUtils.checkIsOnMainThread();
        videoLayoutMeasure.setScalingType(scalingTypeMatchOrientation, scalingTypeMismatchOrientation);
        requestLayout();
    }

    /**
     * Limit render framerate.
     *
     * @param fps Limit render framerate to this value, or use Float.POSITIVE_INFINITY to disable fps
     *            reduction.
     */
    public void setFpsReduction(float fps) {
        synchronized (layoutLock) {
            isRenderingPaused = fps == 0f;
        }
        eglRenderer.setFpsReduction(fps);
    }

    public void disableFpsReduction() {
        synchronized (layoutLock) {
            isRenderingPaused = false;
        }
        eglRenderer.disableFpsReduction();
    }

    public void pauseVideo() {
        synchronized (layoutLock) {
            isRenderingPaused = true;
        }
        eglRenderer.pauseVideo();
    }

    // View layout interface.
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        ThreadUtils.checkIsOnMainThread();
        final Point size;
        synchronized (layoutLock) {
            size =
                    videoLayoutMeasure.measure(widthSpec, heightSpec, rotatedFrameWidth, rotatedFrameHeight);
        }
        setMeasuredDimension(size.x, size.y);
        logD("onMeasure(). New size: " + size.x + "x" + size.y);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        ThreadUtils.checkIsOnMainThread();
        eglRenderer.setLayoutAspectRatio((right - left) / (float) (bottom - top));
    }

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width,
                                          final int height) {
        logD("onSurfaceTextureAvailable: " + surface + " size: " + width + "x" + height);
        ThreadUtils.checkIsOnMainThread();
        eglRenderer.createEglSurface(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(final SurfaceTexture surface, final int width,
                                            final int height) {
        logD("onSurfaceTextureSizeChanged: " + surface + " size: " + width + "x" + height);
        ThreadUtils.checkIsOnMainThread();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
        logD("onSurfaceTextureDestroyed: " + surface);
        ThreadUtils.checkIsOnMainThread();
        final CountDownLatch completionLatch = new CountDownLatch(1);
        eglRenderer.releaseEglSurface(new Runnable() {
            @Override
            public void run() {
                completionLatch.countDown();
            }
        });
        ThreadUtils.awaitUninterruptibly(completionLatch);
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
    }

    private String getResourceName() {
        try {
            return getResources().getResourceEntryName(getId()) + ": ";
        } catch (NotFoundException e) {
            return "";
        }
    }

    /**
     * Post a task to clear the SurfaceView to a transparent uniform color.
     */
    public void clearImage() {
        eglRenderer.clearImage();
    }

    // Update frame dimensions and report any changes to |rendererEvents|.
    private void updateFrameDimensionsAndReportEvents(VideoFrame frame) {
        synchronized (layoutLock) {
            if (isRenderingPaused) {
                return;
            }
            if (!isFirstFrameRendered) {
                isFirstFrameRendered = true;
                logD("Reporting first rendered frame.");
                if (rendererEvents != null) {
                    rendererEvents.onFirstFrameRendered();
                }
            }
            if (rotatedFrameWidth != frame.getRotatedWidth() || rotatedFrameHeight != frame.getRotatedHeight()
                    || frameRotation != frame.getRotation()) {
                logD("Reporting frame resolution changed to " + frame.getBuffer().getWidth()+ "x" + frame.getBuffer().getHeight()
                        + " with rotation " + frame.getRotation());
                if (rendererEvents != null) {
                    rendererEvents.onFrameResolutionChanged(frame.getBuffer().getWidth(), frame.getBuffer().getHeight(), frame.getRotation());
                }
                rotatedFrameWidth = frame.getRotatedWidth();
                rotatedFrameHeight = frame.getRotatedHeight();
                frameRotation = frame.getRotation();
                post(new Runnable() {
                    @Override
                    public void run() {
                        requestLayout();
                    }
                });
            }
        }
    }

    private void logD(String string) {
        Logging.d(TAG, resourceName + string);
    }

    @Override
    public void onFrame(VideoFrame videoFrame) {
        updateFrameDimensionsAndReportEvents(videoFrame);
        eglRenderer.onFrame(videoFrame);
    }

    @Override
    public void onFirstFrameRendered() {
        if (rendererEvents != null) {
            rendererEvents.onFirstFrameRendered();
        }
    }

    @Override
    public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {
        if (rendererEvents != null) {
            rendererEvents.onFrameResolutionChanged(videoWidth, videoHeight, rotation);
        }
        int rotatedWidth = rotation == 0 || rotation == 180 ? videoWidth : videoHeight;
        int rotatedHeight = rotation == 0 || rotation == 180 ? videoHeight : videoWidth;
        post(() -> {
            rotatedFrameWidth = rotatedWidth;
            rotatedFrameHeight = rotatedHeight;
            requestLayout();
        });

    }
}