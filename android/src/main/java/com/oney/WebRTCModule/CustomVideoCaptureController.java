package com.oney.WebRTCModule;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;

/**
 * Capturer-less video capture controller for the Android custom-video-track: the
 * app supplies the frames instead of a camera/screen {@link VideoCapturer} pulling
 * them. Two modes, chosen by <em>how the app produces frames</em>:
 *
 * <ul>
 *   <li><b>Pooled</b> — the app renders GPU frames into the AHB-backed surfaces of
 *       a {@link CustomVideoBufferPool} and pushes them back by index via
 *       {@link #pushFrame}, optionally guarded by a sync-fd GPU fence. The
 *       controller holds a reference to the pool but does not own it; the pool is
 *       disposed separately via {@code releaseCustomVideoBufferPool}.</li>
 *   <li><b>Forwarding</b> — the frames are already produced natively (a camera,
 *       VisionCamera, a native ML pipeline) and the app forwards a finished
 *       {@code AHardwareBuffer*} via {@link #pushExternalBuffer}; no pool and no
 *       fence are involved.</li>
 * </ul>
 *
 * <p>Both modes hand each app-rendered AHB to a {@link CustomVideoFrameDelivery},
 * which imports it as a GL OES texture, wraps it in a
 * {@code TextureBufferImpl}/{@code VideoFrame}, and feeds the {@link VideoSource}'s
 * {@code CapturerObserver}.
 *
 * <p>It extends {@link AbstractVideoCaptureController} purely so it slots into the
 * existing {@code GetUserMediaImpl.TrackPrivate} lifecycle ({@code getSettings()},
 * {@code stopCapture()}, {@code dispose()}). It has no real {@link VideoCapturer},
 * so the capturer-driven base methods are overridden.
 */
class CustomVideoCaptureController extends AbstractVideoCaptureController {
    private static final String TAG = WebRTCModule.TAG;

    /**
     * The buffer pool this pooled track renders into; {@code null} in forwarding
     * mode. Owned by JS (freed via {@code releaseCustomVideoBufferPool}), so this
     * controller never disposes it.
     */
    private final CustomVideoBufferPool pool;
    private boolean disposed = false;

    /**
     * Delivers pushed frames into WebRTC (AHB&nbsp;→&nbsp;OES texture&nbsp;→&nbsp;
     * {@code VideoFrame}). Created in {@link #attachVideoSource(VideoSource)} once
     * the capturer-less {@link VideoSource} exists, since delivery targets its
     * {@code CapturerObserver}. Null until attached, and null again after
     * {@link #releaseGpuResources()}. Volatile because it is written on the module
     * executor but read by the per-frame push path on worklet threads; the push
     * methods read it exactly once into a local so a concurrent teardown cannot
     * null it between a check and a call.
     */
    private volatile CustomVideoFrameDelivery frameDelivery;

    /**
     * Builds a <b>pooled</b> controller bound to an app-allocated buffer pool. The
     * pool's dimensions become this track's reported size.
     *
     * @param pool the buffer pool this track renders into and pushes by index.
     */
    CustomVideoCaptureController(CustomVideoBufferPool pool) {
        // fps is irrelevant for an app-pushed track; reuse width/height as the
        // target/actual dimensions so getSettings() reports the real size.
        super(pool.getWidth(), pool.getHeight(), /* fps */ 0);
        this.pool = pool;
    }

    /**
     * Builds a <b>forwarding</b> controller with no pool. Frames arrive as finished
     * native {@code AHardwareBuffer*}s via {@link #pushExternalBuffer}; their
     * dimensions are read per frame, so the reported size is 0x0 until then.
     */
    CustomVideoCaptureController() {
        super(/* width */ 0, /* height */ 0, /* fps */ 0);
        this.pool = null;
    }

    /**
     * Wires this controller to the capturer-less {@link VideoSource} created by
     * {@link GetUserMediaImpl#createCustomVideoTrack}. Builds the
     * {@link CustomVideoFrameDelivery} that imports the AHBs and ships frames into
     * the source. In pooled mode the delivery caches an OES texture per pool index;
     * in forwarding mode ({@code null} handles) it imports each external buffer per
     * frame. Must be called before {@link #startCapture()}.
     */
    void attachVideoSource(VideoSource videoSource) {
        if (frameDelivery != null) {
            return;
        }
        if (pool != null) {
            frameDelivery = new CustomVideoFrameDelivery(
                    videoSource, pool.getBufferHandles(), pool.getWidth(), pool.getHeight());
        } else {
            frameDelivery = new CustomVideoFrameDelivery(videoSource, /* bufferHandles */ null, 0, 0);
        }
    }

    /**
     * Pushes one app-rendered <b>pooled</b> frame for delivery into WebRTC.
     * Fire-and-forget.
     *
     * @param bufferIndex        pool index of the AHB the app rendered into.
     * @param fenceHandle        the exported GPU fence handle. On Android this is a
     *                           {@code sync-fd} file descriptor
     *                           ({@code GPUSharedFence.export()} →
     *                           {@code {type:'sync-fd', handle: fd}}). {@code 0} (or
     *                           a non-positive fd) means the no-fence fallback
     *                           (deliver immediately).
     * @param fenceSignaledValue unused on Android (the sync-fd already encodes the
     *                           signal); kept for parity with the shared JSI
     *                           contract / iOS.
     * @param timestampNs        frame presentation timestamp in nanoseconds.
     * @param rotation           frame rotation in degrees (0/90/180/270).
     */
    void pushFrame(int bufferIndex, long fenceHandle, long fenceSignaledValue, long timestampNs, int rotation) {
        CustomVideoFrameDelivery delivery = frameDelivery;
        if (delivery == null) {
            Log.w(TAG, "pushFrame before attachVideoSource or after release; dropping frame");
            return;
        }
        delivery.pushFrame(bufferIndex, fenceFdFromHandle(fenceHandle), timestampNs, rotation);
    }

    /**
     * Forwards an already-produced native {@code AHardwareBuffer*} (forwarding
     * mode). Takes an owning reference on the buffer <em>synchronously on the
     * calling (worklet) thread</em> before returning, so it survives the caller's
     * {@code frame.dispose()} that runs immediately after; the import and delivery
     * then run on the GL thread. Fire-and-forget.
     *
     * @param ahbHandle   the finished {@code AHardwareBuffer*} (as a {@code long}).
     * @param timestampNs frame presentation timestamp in nanoseconds; {@code 0}
     *                    means stamp at delivery (a raw buffer carries no time).
     * @param rotation    frame rotation in degrees (0/90/180/270).
     */
    void pushExternalBuffer(long ahbHandle, long timestampNs, int rotation) {
        CustomVideoFrameDelivery delivery = frameDelivery;
        if (delivery == null) {
            Log.w(TAG, "pushExternalBuffer before attachVideoSource or after release; dropping frame");
            return;
        }
        delivery.pushExternalBuffer(ahbHandle, timestampNs, rotation);
    }

    /**
     * Whether this controller has released its GPU resources (or was never wired to
     * a source). Once true, no in-flight delivery can reference the pool's AHBs, so
     * the pool may be disposed safely.
     */
    boolean isReleased() {
        return frameDelivery == null;
    }

    /**
     * Maps the JSI-resolved fence handle (a sync-fd carried as a 64-bit value) to
     * an fd. Returns -1 for the no-fence fallback ({@code 0}, negative, or beyond
     * the {@code int} fd range). The fd's ownership then transfers to
     * {@link CustomVideoFrameDelivery}/EGL.
     */
    private static int fenceFdFromHandle(long fenceHandle) {
        return fenceHandle > 0 && fenceHandle <= Integer.MAX_VALUE ? (int) fenceHandle : -1;
    }

    @Override
    public String getDeviceId() {
        return "custom-video";
    }

    @Override
    public WritableMap getSettings() {
        WritableMap settings = Arguments.createMap();
        settings.putString("deviceId", getDeviceId());
        settings.putString("groupId", "");
        settings.putInt("width", getWidth());
        settings.putInt("height", getHeight());
        return settings;
    }

    // There is no real VideoCapturer for an app-pushed track.
    @Override
    protected VideoCapturer createVideoCapturer() {
        return null;
    }

    @Override
    public void initializeVideoCapturer() {
        // No-op: frames are pushed by the app, not pulled from a capturer.
    }

    @Override
    public void startCapture() {
        // Begin accepting app-pushed frames. There is no real capturer to start;
        // frames arrive via pushFrame()/pushExternalBuffer() and are delivered by
        // frameDelivery.
        if (frameDelivery != null) {
            frameDelivery.start();
        }
    }

    @Override
    public boolean stopCapture() {
        // Pause delivery only. The existing mediaStreamTrackSetEnabled(false)
        // path calls stopCapture(), so this must not free GL imports; startCapture()
        // should be able to resume.
        if (frameDelivery != null) {
            frameDelivery.drain();
        }
        return true;
    }

    /**
     * Custom-video teardown, phase 1 (see {@code GetUserMediaImpl.TrackPrivate.dispose}):
     * stop accepting pushes and drain the in-flight delivery runnables. Frees NO
     * GL/AHB resources — the hardware encoder may still hold a delivered
     * {@code VideoFrame} until the {@code VideoSource}/{@code VideoTrack} are
     * disposed, so the textures/AHBs must stay alive past this point. Idempotent.
     */
    void stopAccepting() {
        if (frameDelivery != null) {
            frameDelivery.drain();
        }
    }

    /**
     * Custom-video teardown, phase 2: free the GL import resources (OES textures /
     * EGLImages, GL thread). Call ONLY after the {@code VideoSource}/{@code VideoTrack}
     * have been disposed so the encoder is quiesced and no longer references the
     * textures; freeing earlier is a use-after-free (the encoder samples a deleted
     * texture). Does NOT free the AHBs: in pooled mode they are owned by the
     * {@link CustomVideoBufferPool} and freed via {@code releaseCustomVideoBufferPool};
     * forwarding buffers are freed per frame by their release callback. Idempotent.
     */
    void releaseGpuResources() {
        if (frameDelivery != null) {
            // release() = final drain (idempotent after stopAccepting) + free GL imports.
            frameDelivery.release();
            frameDelivery = null;
        }
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        // Safety net for any path that disposes without the ordered two-phase
        // teardown above; releaseGpuResources() (drain + free GL imports) is
        // idempotent.
        releaseGpuResources();
    }
}
