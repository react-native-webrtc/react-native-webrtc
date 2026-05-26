package com.oney.WebRTCModule;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.view.ViewCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.RendererCommon.RendererEvents;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class WebRTCView extends ViewGroup {
    /**
     * The scaling type to be utilized by default.
     *
     * The default value is in accord with
     * https://www.w3.org/TR/html5/embedded-content-0.html#the-video-element:
     *
     * In the absence of style rules to the contrary, video content should be
     * rendered inside the element's playback area such that the video content
     * is shown centered in the playback area at the largest possible size that
     * fits completely within it, with the video content's aspect ratio being
     * preserved. Thus, if the aspect ratio of the playback area does not match
     * the aspect ratio of the video, the video will be shown letterboxed or
     * pillarboxed. Areas of the element's playback area that do not contain the
     * video represent nothing.
     */
    private static final ScalingType DEFAULT_SCALING_TYPE = ScalingType.SCALE_ASPECT_FIT;

    private static final String TAG = WebRTCModule.TAG;

    /**
     * The number of instances for {@link SurfaceViewRenderer}, used for logging.
     * When the renderer is initialized, it creates a new {@link javax.microedition.khronos.egl.EGLContext}
     * which can throw an exception, probably due to memory limitations. We log the number of instances that can
     * be created before the exception is thrown.
     */
    private static int surfaceViewRendererInstances;
    private static final CopyOnWriteArrayList<WeakReference<WebRTCView>> streamTrackObservers =
            new CopyOnWriteArrayList<>();

    /**
     * The height of the last video frame rendered by
     * {@link #surfaceViewRenderer}.
     */
    private int frameHeight;

    /**
     * The rotation (degree) of the last video frame rendered by
     * {@link #surfaceViewRenderer}.
     */
    private int frameRotation;

    /**
     * The width of the last video frame rendered by
     * {@link #surfaceViewRenderer}.
     */
    private int frameWidth;

    /**
     * The {@code Object} which synchronizes the access to the layout-related
     * state of this instance such as {@link #frameHeight},
     * {@link #frameRotation}, {@link #frameWidth}, and {@link #scalingType}.
     */
    private final Object layoutSyncRoot = new Object();

    /**
     * The indicator which determines whether this {@code WebRTCView} is to
     * mirror the video represented by {@link #videoTrack} during its rendering.
     */
    private boolean mirror;

    /**
     * Indicates if the {@link SurfaceViewRenderer} is attached to the video
     * track.
     */
    private volatile boolean rendererAttached;

    /**
     * The {@code RendererEvents} which listens to rendering events reported by
     * {@link #surfaceViewRenderer}.
     */
    private final RendererEvents rendererEvents = new RendererEvents() {
        @Override
        public void onFirstFrameRendered() {
            WebRTCView.this.onFirstFrameRendered();
        }

        @Override
        public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {
            WebRTCView.this.onFrameResolutionChanged(videoWidth, videoHeight, rotation);
        }
    };

    /**
     * The {@code Runnable} representation of
     * {@link #requestSurfaceViewRendererLayout()}. Explicitly defined in order
     * to allow the use of the latter with {@link #post(Runnable)} without
     * initializing new instances on every (method) call.
     */
    private final Runnable requestSurfaceViewRendererLayoutRunnable = new Runnable() {
        @Override
        public void run() {
            requestSurfaceViewRendererLayout();
        }
    };

    /**
     * The scaling type this {@code WebRTCView} is to apply to the video
     * represented by {@link #videoTrack} during its rendering. An expression of
     * the CSS property {@code object-fit} in the terms of WebRTC.
     */
    private ScalingType scalingType;

    /**
     * The URL, if any, of the {@link MediaStream} (to be) rendered by this
     * {@code WebRTCView}. The value of {@link #videoTrack} is derived from it.
     */
    private String streamURL;

    /**
     * The {@link View} and {@link VideoSink} implementation which
     * actually renders {@link #videoTrack} on behalf of this instance.
     */
    private final SurfaceViewRenderer surfaceViewRenderer;

    /**
     * The {@code VideoTrack}, if any, rendered by this {@code WebRTCView}.
     */
    private VideoTrack videoTrack;

    /**
     * The callback to be called when video dimensions change.
     */
    private boolean onDimensionsChangeEnabled = false;

    /**
     * The PIP manager for this view (lazily initialized).
     */
    private PIPManager pipManager;

    public WebRTCView(Context context) {
        super(context);

        surfaceViewRenderer = new SurfaceViewRenderer(context);
        addView(surfaceViewRenderer);

        setMirror(false);
        setScalingType(DEFAULT_SCALING_TYPE);
    }

    public static void notifyStreamVideoTrackChanged(String streamId) {
        for (WeakReference<WebRTCView> weakRef : streamTrackObservers) {
            WebRTCView view = weakRef.get();
            if (view == null) {
                streamTrackObservers.remove(weakRef);
                continue;
            }
            view.post(() -> view.onStreamVideoTrackChanged(streamId));
        }
    }

    private void registerStreamTrackObserver() {
        for (WeakReference<WebRTCView> weakRef : streamTrackObservers) {
            WebRTCView view = weakRef.get();
            if (view == null) {
                streamTrackObservers.remove(weakRef);
                continue;
            }
            if (view == this) {
                return;
            }
        }
        streamTrackObservers.add(new WeakReference<>(this));
    }

    private void unregisterStreamTrackObserver() {
        List<WeakReference<WebRTCView>> toRemove = new ArrayList<>();
        for (WeakReference<WebRTCView> weakRef : streamTrackObservers) {
            WebRTCView view = weakRef.get();
            if (view == null || view == this) {
                toRemove.add(weakRef);
            }
        }
        streamTrackObservers.removeAll(toRemove);
    }

    private void onStreamVideoTrackChanged(String streamId) {
        if (!Objects.equals(streamId, streamURL)) {
            return;
        }
        String expectedStreamURL = this.streamURL;
        getVideoTrackForStreamURL(expectedStreamURL, videoTrack -> {
            if (!Objects.equals(expectedStreamURL, this.streamURL)) {
                return;
            }
            if (videoTrack == null) {
                return;
            }
            setVideoTrack(videoTrack);
        });
    }

    /**
     * Gets the PIP manager for this view, creating it if necessary.
     *
     * @return The PIP manager.
     */
    public PIPManager getPipManager() {
        if (pipManager == null) {
            pipManager = new PIPManager(this);
        }
        return pipManager;
    }

    /**
     * Gets the current video track being rendered.
     *
     * @return The video track, or null if none is set.
     */
    VideoTrack getVideoTrack() {
        return videoTrack;
    }

    /**
     * Gets the current scaling type.
     *
     * @return The scaling type.
     */
    ScalingType getScalingType() {
        synchronized (layoutSyncRoot) {
            return scalingType;
        }
    }

    /**
     * Gets whether the video is mirrored.
     *
     * @return Whether the video is mirrored.
     */
    boolean getMirror() {
        synchronized (layoutSyncRoot) {
            return mirror;
        }
    }

    /**
     * "Cleans" the {@code SurfaceViewRenderer} by setting the view part to
     * opaque black and the surface part to transparent.
     */
    private void cleanSurfaceViewRenderer() {
        surfaceViewRenderer.setBackgroundColor(Color.BLACK);
        surfaceViewRenderer.clearImage();
    }

    /**
     * Asynchronously retrieves the VideoTrack for the given streamURL.
     * This method avoids blocking the UI thread by performing the lookup
     * on the WebRTC executor thread and posting the result back to the UI thread.
     *
     * @param streamURL The stream URL to lookup
     * @param callback Callback invoked on UI thread with the VideoTrack (or null if not found)
     */
    private void getVideoTrackForStreamURL(String streamURL, java.util.function.Consumer<VideoTrack> callback) {
        if (streamURL == null) {
            callback.accept(null);
            return;
        }

        ReactContext reactContext = (ReactContext) getContext();
        WebRTCModule module = reactContext.getNativeModule(WebRTCModule.class);

        // Submit lookup to executor thread to avoid blocking UI thread
        ThreadUtils.runOnExecutor(() -> {
            try {
                MediaStream stream = module.getStreamForReactTag(streamURL);
                if (stream == null) {
                    Log.w(TAG, "Stream not found for URL: " + streamURL);
                    post(() -> callback.accept(null));
                    return;
                }

                VideoTrack videoTrack = null;
                List<VideoTrack> videoTracks = stream.videoTracks;
                if (!videoTracks.isEmpty()) {
                    videoTrack = videoTracks.get(0);
                }

                if (videoTrack == null) {
                    Log.w(TAG, "No video stream for react tag: " + streamURL);
                    post(() -> callback.accept(null));
                    return;
                }

                // Post result back to UI thread
                final VideoTrack result = videoTrack;
                post(() -> callback.accept(result));
            } catch (Throwable tr) {
                Log.e(TAG, "Error getting video track for stream URL: " + streamURL, tr);
                post(() -> callback.accept(null));
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        try {
            registerStreamTrackObserver();
            if (streamURL != null) {
                String expectedStreamURL = this.streamURL;
                getVideoTrackForStreamURL(expectedStreamURL, videoTrack -> {
                    if (!Objects.equals(expectedStreamURL, this.streamURL)) {
                        return;
                    }
                    setVideoTrack(videoTrack);
                });
            }
            // Generally, OpenGL is only necessary while this View is attached
            // to a window so there is no point in having the whole rendering
            // infrastructure hooked up while this View is not attached to a
            // window. Additionally, a memory leak was solved in a similar way
            // on iOS.
            tryAddRendererToVideoTrack();

            // Notify PIP manager if it exists
            if (pipManager != null) {
                pipManager.onAttachedToWindow();
            }
        } finally {
            super.onAttachedToWindow();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            unregisterStreamTrackObserver();
            // Generally, OpenGL is only necessary while this View is attached
            // to a window so there is no point in having the whole rendering
            // infrastructure hooked up while this View is not attached to a
            // window. Additionally, a memory leak was solved in a similar way
            // on iOS.
            removeRendererFromVideoTrack();

            // Notify PIP manager if it exists
            if (pipManager != null) {
                pipManager.onDetachedFromWindow();
            }
        } finally {
            super.onDetachedFromWindow();
        }
    }

    /**
     * Callback fired by {@link #surfaceViewRenderer} when the first frame is
     * rendered. Here we will set the background of the view part of the
     * SurfaceView to transparent, so the surface (where video is actually
     * rendered) shines through.
     */
    private void onFirstFrameRendered() {
        post(() -> {
            Log.d(TAG, "First frame rendered.");
            surfaceViewRenderer.setBackgroundColor(Color.TRANSPARENT);
        });
    }

    /**
     * Callback fired by {@link #surfaceViewRenderer} when the resolution or
     * rotation of the frame it renders has changed.
     *
     * @param videoWidth The new width of the rendered video frame.
     * @param videoHeight The new height of the rendered video frame.
     * @param rotation The new rotation of the rendered video frame.
     */
    private void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {
        boolean changed = false;

        synchronized (layoutSyncRoot) {
            if (this.frameHeight != videoHeight) {
                this.frameHeight = videoHeight;
                changed = true;
            }
            if (this.frameRotation != rotation) {
                this.frameRotation = rotation;
                changed = true;
            }
            if (this.frameWidth != videoWidth) {
                this.frameWidth = videoWidth;
                changed = true;
            }
        }
        if (changed) {
            // The onFrameResolutionChanged method call executes on the
            // surfaceViewRenderer's render Thread.
            post(requestSurfaceViewRendererLayoutRunnable);

            // Call the onDimensionsChange callback if it's enabled
            if (onDimensionsChangeEnabled) {
                post(() -> {
                    try {
                        ReactContext reactContext = (ReactContext) getContext();
                        WritableMap params = Arguments.createMap();
                        params.putInt("width", videoWidth);
                        params.putInt("height", videoHeight);

                        // Send the event through React Native's event system
                        reactContext.getJSModule(RCTEventEmitter.class)
                                .receiveEvent(getId(), "onDimensionsChange", params);
                    } catch (Exception e) {
                        Log.e(TAG, "Error calling onDimensionsChange callback", e);
                    }
                });
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int height = b - t;
        int width = r - l;

        if (height == 0 || width == 0) {
            l = t = r = b = 0;
        } else {
            int frameHeight;
            int frameRotation;
            int frameWidth;
            ScalingType scalingType;

            synchronized (layoutSyncRoot) {
                frameHeight = this.frameHeight;
                frameRotation = this.frameRotation;
                frameWidth = this.frameWidth;
                scalingType = this.scalingType;
            }

            switch (scalingType) {
                case SCALE_ASPECT_FILL:
                    // Fill this ViewGroup with surfaceViewRenderer and the latter
                    // will take care of filling itself with the video similarly to
                    // the cover value the CSS property object-fit.
                    r = width;
                    l = 0;
                    b = height;
                    t = 0;
                    break;
                case SCALE_ASPECT_FIT:
                default:
                    // Lay surfaceViewRenderer out inside this ViewGroup in accord
                    // with the contain value of the CSS property object-fit.
                    // SurfaceViewRenderer will fill itself with the video similarly
                    // to the cover or contain value of the CSS property object-fit
                    // (which will not matter, eventually).
                    if (frameHeight == 0 || frameWidth == 0) {
                        l = t = r = b = 0;
                    } else {
                        float frameAspectRatio = (frameRotation % 180 == 0) ? frameWidth / (float) frameHeight
                                                                            : frameHeight / (float) frameWidth;
                        Point frameDisplaySize =
                                RendererCommon.getDisplaySize(scalingType, frameAspectRatio, width, height);

                        l = (width - frameDisplaySize.x) / 2;
                        t = (height - frameDisplaySize.y) / 2;
                        r = l + frameDisplaySize.x;
                        b = t + frameDisplaySize.y;
                    }
                    break;
            }
        }
        surfaceViewRenderer.layout(l, t, r, b);
    }

    /**
     * Stops rendering {@link #videoTrack} and releases the associated acquired
     * resources (if rendering is in progress).
     */
    private void removeRendererFromVideoTrack() {
        if (rendererAttached) {
            if (videoTrack != null) {
                final VideoTrack trackToRemove = videoTrack;
                ThreadUtils.runOnExecutor(() -> {
                    try {
                        trackToRemove.removeSink(surfaceViewRenderer);
                    } catch (Throwable tr) {
                        Log.w(TAG, "Failed to remove sink from track", tr);
                    }
                });
            }

            surfaceViewRenderer.release();
            surfaceViewRendererInstances--;
            rendererAttached = false;

            // Since this WebRTCView is no longer rendering anything, make sure
            // surfaceViewRenderer displays nothing as well.
            synchronized (layoutSyncRoot) {
                frameHeight = 0;
                frameRotation = 0;
                frameWidth = 0;
            }
            requestSurfaceViewRendererLayout();
        }
    }

    /**
     * Request that {@link #surfaceViewRenderer} be laid out (as soon as
     * possible) because layout-related state either of this instance or of
     * {@code surfaceViewRenderer} has changed.
     */
    @SuppressLint("WrongCall")
    private void requestSurfaceViewRendererLayout() {
        // Google/WebRTC just call requestLayout() on surfaceViewRenderer when
        // they change the value of its mirror or surfaceType property.
        surfaceViewRenderer.requestLayout();
        // The above is not enough though when the video frame's dimensions or
        // rotation change. The following will suffice.
        if (!ViewCompat.isInLayout(this)) {
            onLayout(
                    /* changed */ false, getLeft(), getTop(), getRight(), getBottom());
        }
    }

    /**
     * Sets the indicator which determines whether this {@code WebRTCView} is to
     * mirror the video represented by {@link #videoTrack} during its rendering.
     *
     * @param mirror If this {@code WebRTCView} is to mirror the video
     * represented by {@code videoTrack} during its rendering, {@code true};
     * otherwise, {@code false}.
     */
    public void setMirror(boolean mirror) {
        if (this.mirror != mirror) {
            this.mirror = mirror;
            surfaceViewRenderer.setMirror(mirror);
            // SurfaceViewRenderer takes the value of its mirror property into
            // account upon its layout.
            requestSurfaceViewRendererLayout();
        }
    }

    /**
     * In the fashion of
     * https://www.w3.org/TR/html5/embedded-content-0.html#dom-video-videowidth
     * and https://www.w3.org/TR/html5/rendering.html#video-object-fit,
     * resembles the CSS style {@code object-fit}.
     *
     * @param objectFit For details, refer to the documentation of the
     * {@code objectFit} property of the JavaScript counterpart of
     * {@code WebRTCView} i.e. {@code RTCView}.
     */
    public void setObjectFit(String objectFit) {
        ScalingType scalingType =
                "cover".equals(objectFit) ? ScalingType.SCALE_ASPECT_FILL : ScalingType.SCALE_ASPECT_FIT;

        setScalingType(scalingType);
    }

    private void setScalingType(ScalingType scalingType) {
        synchronized (layoutSyncRoot) {
            if (this.scalingType == scalingType) {
                return;
            }
            this.scalingType = scalingType;
            surfaceViewRenderer.setScalingType(scalingType);
        }
        // Both this instance ant its SurfaceViewRenderer take the value of
        // their scalingType properties into account upon their layouts.
        requestSurfaceViewRendererLayout();
    }

    /**
     * Sets the {@code MediaStream} to be rendered by this {@code WebRTCView}.
     * The implementation renders the first {@link VideoTrack}, if any, of the
     * specified {@code mediaStream}.
     *
     * @param streamURL The URL of the {@code MediaStream} to be rendered by
     * this {@code WebRTCView} or {@code null}.
     */
    void setStreamURL(String streamURL) {
        Log.d(TAG, "Set stream URL " + streamURL + " current: " + this.streamURL);
        if (Objects.equals(streamURL, this.streamURL)) {
            return;
        }

        this.streamURL = streamURL;
        getVideoTrackForStreamURL(streamURL, videoTrack -> {
            if (!Objects.equals(streamURL, this.streamURL)) {
                return;
            }
            setVideoTrack(videoTrack);
        });
    }

    /**
     * Sets the {@code VideoTrack} to be rendered by this {@code WebRTCView}.
     *
     * <p>The SurfaceViewRenderer (and its underlying EGL context) is kept alive as long as this
     * view is attached to a window, even when the track becomes {@code null} (camera off).  This
     * avoids the pattern of calling {@code SurfaceViewRenderer.release()} on camera-off and
     * {@code SurfaceViewRenderer.init()} on camera-on, which each time allocates a new EGL context
     * from Android's finite per-process pool 
     * {@code release()} is only called from {@link #onDetachedFromWindow()}.
     *
     * @param videoTrack The {@code VideoTrack} to be rendered by this
     * {@code WebRTCView} or {@code null}.
     */
    private void setVideoTrack(VideoTrack videoTrack) {
        VideoTrack oldVideoTrack = this.videoTrack;

        if (oldVideoTrack == videoTrack) {
            return;
        }

        // Case 1: Swapping one live track for another while the renderer is active.
        // Just move the sink — no EGL context churn.
        if (oldVideoTrack != null && videoTrack != null && rendererAttached) {
            final VideoTrack capturedOld = oldVideoTrack;
            final VideoTrack capturedNew = videoTrack;
            this.videoTrack = videoTrack;
            ThreadUtils.runOnExecutor(() -> {
                try {
                    capturedOld.removeSink(surfaceViewRenderer);
                } catch (Throwable tr) {
                    Log.w(TAG, "Failed to remove sink from old track", tr);
                }
                if (!rendererAttached) {
                    return;
                }
                try {
                    capturedNew.addSink(surfaceViewRenderer);
                } catch (Throwable tr) {
                    Log.e(TAG, "Failed to add renderer", tr);
                }
            });
            return;
        }

        // Detach the old track's sink from the renderer (without releasing the EGL context).
        if (oldVideoTrack != null && rendererAttached) {
            final VideoTrack trackToRemove = oldVideoTrack;
            ThreadUtils.runOnExecutor(() -> {
                try {
                    trackToRemove.removeSink(surfaceViewRenderer);
                } catch (Throwable tr) {
                    Log.w(TAG, "Failed to remove sink from track", tr);
                }
            });
        }

        this.videoTrack = videoTrack;

        if (videoTrack != null) {
            if (rendererAttached) {
                // The EGL context was kept alive from a previous session — just add the sink.
                if (oldVideoTrack == null) {
                    cleanSurfaceViewRenderer();
                }
                final VideoTrack capturedNew = videoTrack;
                ThreadUtils.runOnExecutor(() -> {
                    try {
                        capturedNew.addSink(surfaceViewRenderer);
                    } catch (Throwable tr) {
                        Log.e(TAG, "Failed to add renderer", tr);
                    }
                });
            } else {
                // Renderer not yet initialised (first render after attach, or after detach).
                if (oldVideoTrack == null) {
                    cleanSurfaceViewRenderer();
                }
                tryAddRendererToVideoTrack();
            }
        } else {
            // Track became null (camera off). Keep the EGL context alive so the next
            // camera-on can reuse it without calling surfaceViewRenderer.init() again.
            // The renderer is only released in onDetachedFromWindow().
            cleanSurfaceViewRenderer();
        }
    }

    /**
     * Sets the z-order of this {@link WebRTCView} in the stacking space of all
     * {@code WebRTCView}s. For more details, refer to the documentation of the
     * {@code zOrder} property of the JavaScript counterpart of
     * {@code WebRTCView} i.e. {@code RTCView}.
     *
     * @param zOrder The z-order to set on this {@code WebRTCView}.
     */
    public void setZOrder(int zOrder) {
        switch (zOrder) {
            case 0:
                surfaceViewRenderer.setZOrderMediaOverlay(false);
                break;
            case 1:
                surfaceViewRenderer.setZOrderMediaOverlay(true);
                break;
            case 2:
                surfaceViewRenderer.setZOrderOnTop(true);
                break;
        }
    }

    /**
     * Starts rendering {@link #videoTrack} if rendering is not in progress and
     * all preconditions for the start of rendering are met.
     */
    private void tryAddRendererToVideoTrack() {
        if (!rendererAttached && videoTrack != null && ViewCompat.isAttachedToWindow(this)) {
            EglBase.Context sharedContext = EglUtils.getRootEglBaseContext();

            if (sharedContext == null) {
                // If SurfaceViewRenderer#init() is invoked, it will throw a
                // RuntimeException which will very likely kill the application.
                Log.e(TAG, "Failed to render a VideoTrack!");
                return;
            }

            try {
                surfaceViewRenderer.init(sharedContext, rendererEvents);
                surfaceViewRendererInstances++;
            } catch (Exception e) {
                Logging.e(
                        TAG, "Failed to initialize surfaceViewRenderer on instance " + surfaceViewRendererInstances, e);
                return;
            }

            final VideoTrack trackToAdd = videoTrack;
            ThreadUtils.runOnExecutor(() -> {
                try {
                    trackToAdd.addSink(surfaceViewRenderer);
                } catch (Throwable tr) {
                    Log.e(TAG, "Failed to add renderer", tr);
                }
            });

            rendererAttached = true;
        }
    }

    /**
     * Sets whether the onDimensionsChange callback should be called.
     *
     * @param enabled Whether the callback should be enabled.
     */
    public void setOnDimensionsChange(boolean enabled) {
        this.onDimensionsChangeEnabled = enabled;
    }
}
