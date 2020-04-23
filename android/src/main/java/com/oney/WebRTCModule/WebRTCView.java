package com.oney.WebRTCModule;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import androidx.core.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

import com.facebook.react.bridge.ReactContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.RendererCommon.RendererEvents;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

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
    private static final ScalingType DEFAULT_SCALING_TYPE
        = ScalingType.SCALE_ASPECT_FIT;

    /**
     * {@link View#isInLayout()} as a <tt>Method</tt> to be invoked via
     * reflection in order to accommodate its lack of availability before API
     * level 18. {@link ViewCompat#isInLayout(View)} is the best solution but I
     * could not make it available along with
     * {@link ViewCompat#isAttachedToWindow(View)} at the time of this writing.
     */
    private static final Method IS_IN_LAYOUT;

    private static final String TAG = WebRTCModule.TAG;

    static {
        // IS_IN_LAYOUT
        Method isInLayout = null;

        try {
            Method m = WebRTCView.class.getMethod("isInLayout");

            if (boolean.class.isAssignableFrom(m.getReturnType())) {
                isInLayout = m;
            }
        } catch (NoSuchMethodException e) {
            // Fall back to the behavior of ViewCompat#isInLayout(View).
        }
        IS_IN_LAYOUT = isInLayout;
    }

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
    private boolean rendererAttached;

    /**
     * The {@code RendererEvents} which listens to rendering events reported by
     * {@link #surfaceViewRenderer}.
     */
    private final RendererEvents rendererEvents
        = new RendererEvents() {
            @Override
            public void onFirstFrameRendered() {
                WebRTCView.this.onFirstFrameRendered();
            }

            @Override
            public void onFrameResolutionChanged(
                    int videoWidth, int videoHeight,
                    int rotation) {
                WebRTCView.this.onFrameResolutionChanged(
                        videoWidth, videoHeight,
                        rotation);
            }
        };

    /**
     * The {@code Runnable} representation of
     * {@link #requestSurfaceViewRendererLayout()}. Explicitly defined in order
     * to allow the use of the latter with {@link #post(Runnable)} without
     * initializing new instances on every (method) call.
     */
    private final Runnable requestSurfaceViewRendererLayoutRunnable
        = new Runnable() {
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

    public WebRTCView(Context context) {
        super(context);

        surfaceViewRenderer = new SurfaceViewRenderer(context);
        addView(surfaceViewRenderer);

        setMirror(false);
        setScalingType(DEFAULT_SCALING_TYPE);
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
     * Gets the {@link VideoTrack}, if any, (to be) rendered by this
     * {@code WebRTCView}.
     *
     * @return The {@code VideoTrack} (to be) rendered by this
     * {@code WebRTCView}.
     */
    private VideoTrack getVideoTrack() {
        VideoTrack videoTrack = this.videoTrack;

        // XXX If WebRTCModule#mediaStreamTrackRelease has already been invoked
        // on videoTrack, then it is no longer safe to call methods (e.g.
        // addRenderer, removeRenderer) on videoTrack.
        if (videoTrack != null
                && videoTrack != getVideoTrackForStreamURL(this.streamURL)) {
            videoTrack = null;
        }

        return videoTrack;
    }

    private VideoTrack getVideoTrackForStreamURL(String streamURL) {
        VideoTrack videoTrack = null;

        if (streamURL != null) {
            ReactContext reactContext = (ReactContext) getContext();
            WebRTCModule module
                = reactContext.getNativeModule(WebRTCModule.class);
            MediaStream stream = module.getStreamForReactTag(streamURL);

            if (stream != null) {
                List<VideoTrack> videoTracks = stream.videoTracks;

                if (!videoTracks.isEmpty()) {
                    videoTrack = videoTracks.get(0);
                }
            }
        }

        return videoTrack;
    }

    /**
     * If this <tt>View</tt> has {@link View#isInLayout()}, invokes it and
     * returns its return value; otherwise, returns <tt>false</tt> like
     * {@link ViewCompat#isInLayout(View)}.
     *
     * @return If this <tt>View</tt> has <tt>View#isInLayout()</tt>, invokes it
     * and returns its return value; otherwise, returns <tt>false</tt>.
     */
    private boolean invokeIsInLayout() {
        Method m = IS_IN_LAYOUT;
        boolean b = false;

        if (m != null) {
            try {
                b = (boolean) m.invoke(this);
            } catch (IllegalAccessException | InvocationTargetException e) {
                // Fall back to the behavior of ViewCompat#isInLayout(View).
            }
        }
        return b;
    }

    @Override
    protected void onAttachedToWindow() {
        try {
            // Generally, OpenGL is only necessary while this View is attached
            // to a window so there is no point in having the whole rendering
            // infrastructure hooked up while this View is not attached to a
            // window. Additionally, a memory leak was solved in a similar way
            // on iOS.
            tryAddRendererToVideoTrack();
        } finally {
            super.onAttachedToWindow();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            // Generally, OpenGL is only necessary while this View is attached
            // to a window so there is no point in having the whole rendering
            // infrastructure hooked up while this View is not attached to a
            // window. Additionally, a memory leak was solved in a similar way
            // on iOS.
            removeRendererFromVideoTrack();
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
    private void onFrameResolutionChanged(
            int videoWidth, int videoHeight,
            int rotation) {
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
                    float frameAspectRatio
                        = (frameRotation % 180 == 0)
                            ? frameWidth / (float) frameHeight
                            : frameHeight / (float) frameWidth;
                    Point frameDisplaySize
                        = RendererCommon.getDisplaySize(
                                scalingType,
                                frameAspectRatio,
                                width, height);

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
            // XXX If WebRTCModule#mediaStreamTrackRelease has already been
            // invoked on videoTrack, then it is no longer safe to call methods
            // (e.g. addSink, removeSink) on videoTrack. It is OK to
            // skip the removeSink invocation in such a case because
            // VideoTrack#dispose() has performed it already.
            VideoTrack videoTrack = getVideoTrack();

            if (videoTrack != null) {
                try {
                    videoTrack.removeSink(surfaceViewRenderer);
                } catch (Throwable tr) {
                    // Releasing streams happens in the WebRTC thread, thus we might (briefly) hold
                    // a reference to a released stream.
                    Log.e(TAG, "Failed to remove renderer", tr);
                }
            }

            surfaceViewRenderer.release();
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
        if (!invokeIsInLayout()) {
            onLayout(
                /* changed */ false,
                getLeft(), getTop(), getRight(), getBottom());
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
        ScalingType scalingType
            = "cover".equals(objectFit)
                ? ScalingType.SCALE_ASPECT_FILL
                : ScalingType.SCALE_ASPECT_FIT;

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
        // Is the value of this.streamURL really changing?
        if (!Objects.equals(streamURL, this.streamURL)) {
            // XXX The value of this.streamURL is really changing. Before
            // realizing/applying the change, let go of the old videoTrack. Of
            // course, that is only necessary if the value of videoTrack will
            // really change. Please note though that letting go of the old
            // videoTrack before assigning to this.streamURL is vital;
            // otherwise, removeRendererFromVideoTrack will fail to remove the
            // old videoTrack from the associated videoRenderer, two
            // VideoTracks (the old and the new) may start rendering and, most
            // importantly the videoRender may eventually crash when the old
            // videoTrack is disposed.
            VideoTrack videoTrack = getVideoTrackForStreamURL(streamURL);

            if (this.videoTrack != videoTrack) {
                setVideoTrack(null);
            }

            this.streamURL = streamURL;

            // After realizing/applying the change in the value of
            // this.streamURL, reflect it on the value of videoTrack.
            setVideoTrack(videoTrack);
        }
    }

    /**
     * Sets the {@code VideoTrack} to be rendered by this {@code WebRTCView}.
     *
     * @param videoTrack The {@code VideoTrack} to be rendered by this
     * {@code WebRTCView} or {@code null}.
     */
    private void setVideoTrack(VideoTrack videoTrack) {
        VideoTrack oldVideoTrack = this.videoTrack;

        if (oldVideoTrack != videoTrack) {
            if (oldVideoTrack != null) {
                if (videoTrack == null) {
                    // If we are not going to render any stream, clean the
                    // surface.
                    cleanSurfaceViewRenderer();
                }
                removeRendererFromVideoTrack();
            }

            this.videoTrack = videoTrack;

            if (videoTrack != null) {
                tryAddRendererToVideoTrack();
                if (oldVideoTrack == null) {
                    // If there was no old track, clean the surface so we start
                    // with black.
                    cleanSurfaceViewRenderer();
                }
            }
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
        VideoTrack videoTrack;

        if (!rendererAttached
                // XXX If WebRTCModule#mediaStreamTrackRelease has already been
                // invoked on videoTrack, then it is no longer safe to call
                // methods (e.g. addRenderer, removeRenderer) on videoTrack.
                && (videoTrack = getVideoTrack()) != null
                && ViewCompat.isAttachedToWindow(this)) {
            EglBase.Context sharedContext = EglUtils.getRootEglBaseContext();

            if (sharedContext == null) {
                // If SurfaceViewRenderer#init() is invoked, it will throw a
                // RuntimeException which will very likely kill the application.
                Log.e(TAG, "Failed to render a VideoTrack!");
                return;
            }

            surfaceViewRenderer.init(sharedContext, rendererEvents);

            try {
                videoTrack.addSink(surfaceViewRenderer);
            } catch (Throwable tr) {
                // Releasing streams happens in the WebRTC thread, thus we might (briefly) hold
                // a reference to a released stream.
                Log.e(TAG, "Failed to add renderer", tr);

                surfaceViewRenderer.release();
                return;
            }

            rendererAttached = true;
        }
    }
}
