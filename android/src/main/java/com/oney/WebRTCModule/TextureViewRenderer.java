package com.oney.WebRTCModule;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.util.Log;
import android.view.TextureView;

import org.webrtc.EglBase;
import org.webrtc.EglRenderer;
import org.webrtc.GlRectDrawer;
import org.webrtc.RendererCommon;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

public class TextureViewRenderer extends TextureView implements VideoSink {
    private static final String TAG = "TextureViewRenderer";

    private static final int[] EGL_CONFIG_ATTRIBUTES = new int[] {
        EGL14.EGL_RED_SIZE, 8,
        EGL14.EGL_GREEN_SIZE, 8,
        EGL14.EGL_BLUE_SIZE, 8,
        EGL14.EGL_ALPHA_SIZE, 8,
        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
        EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
        EGL14.EGL_NONE,
    };

    private final EglRenderer eglRenderer;
    private boolean isInitialized;
    private RendererCommon.RendererEvents rendererEvents;

    private int frameWidth;
    private int frameHeight;
    private int frameRotation;

    public TextureViewRenderer(Context context) {
        super(context);
        this.eglRenderer = new EglRenderer("TextureViewRenderer");
        setSurfaceTextureListener(surfaceTextureListener);
    }

    public void init(EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents) {
        if (isInitialized) return;
        this.rendererEvents = rendererEvents;
        eglRenderer.init(sharedContext, EGL_CONFIG_ATTRIBUTES, new GlRectDrawer());
        isInitialized = true;
    }

    public void release() {
        if (isInitialized) {
            eglRenderer.release();
            isInitialized = false;
        }
    }

    @Override
    public void onFrame(VideoFrame frame) {
        eglRenderer.onFrame(frame);
        updateFrameDimensions(frame);
    }

    private void updateFrameDimensions(VideoFrame frame) {
        int rotation = frame.getRotation();
        int width = frame.getRotatedWidth();
        int height = frame.getRotatedHeight();

        boolean changed = false;
        if (frameWidth != width || frameHeight != height || frameRotation != rotation) {
            frameWidth = width;
            frameHeight = height;
            frameRotation = rotation;
            changed = true;
        }

        if (changed && rendererEvents != null) {
            rendererEvents.onFrameResolutionChanged(width, height, rotation);
            post(() -> {
                requestLayout();
                // Применяем setTransform для cover-эффекта:
                // кадр заполняет view, сохраняя aspect ratio, избыток центрируется.
                int vw = getWidth();
                int vh = getHeight();
                if (vw > 0 && vh > 0 && width > 0 && height > 0) {
                    float frameAspect = (float) frameWidth / frameHeight;
                    float viewAspect = (float) vw / vh;
                    transformMatrix.reset();
                    Matrix matrix = transformMatrix;
                    boolean fill = (scalingType == RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                    if (fill ? (frameAspect > viewAspect) : (frameAspect < viewAspect)) {
                        // Scale to fill/fit by height.
                        float displayW = vh * frameAspect;
                        float sx = displayW / vw;
                        float dx = -(displayW - vw) / 2f;
                        matrix.setScale(sx, 1f);
                        matrix.postTranslate(dx, 0);
                    } else {
                        // Scale to fill/fit by width.
                        float displayH = vw / frameAspect;
                        float sy = displayH / vh;
                        float dy = -(displayH - vh) / 2f;
                        matrix.setScale(1f, sy);
                        matrix.postTranslate(0, dy);
                    }
                    // Зеркалирование для фронтальной камеры
                    if (mirror) {
                        flipMatrix.reset();
                        Matrix flip = flipMatrix;
                        flip.setScale(-1f, 1f);
                        flip.postTranslate(vw, 0f);
                        matrix.postConcat(flip);
                    }
                    setTransform(matrix);
                }
            });
        }
    }

    public void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    public void setScalingType(RendererCommon.ScalingType scalingType) {
        this.scalingType = scalingType;
    }

    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;
    private boolean mirror;
    private final Matrix transformMatrix = new Matrix();
    private final Matrix flipMatrix = new Matrix();

    private final SurfaceTextureListener surfaceTextureListener = new SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "Surface texture available: " + width + "x" + height);
            eglRenderer.createEglSurface(surface);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            if (!isInitialized) return false;
            Log.d(TAG, "Surface texture destroyed");
            eglRenderer.releaseEglSurface(() -> surface.release());
            return true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "Surface texture size changed: " + width + "x" + height);
            if (width > 0 && height > 0 && isInitialized) {
                eglRenderer.releaseEglSurface(new Runnable() {
                    @Override
                    public void run() {
                        // No-op
                    }
                });
                eglRenderer.createEglSurface(surface);
            }
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // No-op
        }
    };
}
