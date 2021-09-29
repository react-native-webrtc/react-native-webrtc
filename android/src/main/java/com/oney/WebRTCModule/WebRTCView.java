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
import org.webrtc.Logging;
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

    private static final String TAG = WebRTCModule.TAG;

    /**
     * The number of instances for {@link SurfaceViewRenderer}, used for logging.
     * When the renderer is initialized, it creates a new {@link javax.microedition.khronos.egl.EGLContext}
     * which can throw an exception, probably due to memory limitations. We log the number of instances that can
     * be created before the exception is thrown.
     */
    private static int surfaceViewRendererInstances;

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
            if (videoTrack != null) {
                // XXX If WebRTCModule#mediaStreamTrackRelease has already been
                // invoked on videoTrack, then it is no longer safe to call removeSink
                // on the instance, it will throw IllegalStateException.
                try {
                    videoTrack.removeSink(surfaceViewRenderer);
                } catch (Throwable tr) {
                    // Releasing streams happens in the WebRTC thread, thus we might (briefly) hold
                    // a reference to a released stream. Just ignore the error and move on.
                }
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
        if (!rendererAttached
                && videoTrack != null
                && ViewCompat.isAttachedToWindow(this)) {
            EglBase.Context sharedContext = EglUtils.getRootEglBaseContext();

            if (sharedContext == null) {
                // If SurfaceViewRenderer#init() is invoked, it will throw a
                // RuntimeException which will very likely kill the application.
                Log.e(TAG, "Failed to render a VideoTrack!");
                return;
            }

            try {
                surfaceViewRendererInstances++;
                surfaceViewRenderer.init(sharedContext, rendererEvents);
            } catch (Exception e) {
                Logging.e(TAG, "Failed to initialize surfaceViewRenderer on instance " + surfaceViewRendererInstances, e);
                surfaceViewRendererInstances--;
            }

            // XXX If WebRTCModule#mediaStreamTrackRelease has already been
            // invoked on videoTrack, then it is no longer safe to call addSink
            // on the instance, it will throw IllegalStateException.
            try {
                videoTrack.addSink(surfaceViewRenderer);
            } catch (Throwable tr) {
                // Releasing streams happens in the WebRTC thread, thus we might (briefly) hold
                // a reference to a released stream.
                Log.e(TAG, "Failed to add renderer", tr);

                surfaceViewRenderer.release();
                surfaceViewRendererInstances--;
                return;
            }

            rendererAttached = true;
        }
    }
}
