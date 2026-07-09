package com.oney.WebRTCModule;

import android.graphics.Matrix;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import org.webrtc.EglBase;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSource;
import org.webrtc.YuvConverter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Delivers app-rendered {@code AHardwareBuffer} (AHB) frames into a WebRTC
 * {@link VideoSource} for the Android custom-video-track.
 *
 * <p>Waits a sync-fd GPU fence, then ships an OES-texture-backed
 * {@link VideoFrame}. libwebrtc encodes from a GL texture (not an AHB), so per
 * pooled AHB we build a {@code GL_TEXTURE_EXTERNAL_OES} texture aliasing it ONCE
 * (native: AHB&nbsp;→&nbsp;EGLImage&nbsp;→&nbsp;OES) and reuse it for every frame.
 *
 * <h2>Threading / EGL context</h2>
 * All GL work — the AHB import, the fence wait, and the {@code onFrameCaptured}
 * delivery — runs on ONE dedicated thread: the {@link SurfaceTextureHelper}
 * handler thread. We create that helper from WebRTC's root EGL context
 * ({@link EglUtils#getRootEglBaseContext()}), so its handler-thread EGL context
 * is share-linked to the root and the OES texture is visible to WebRTC's encoder
 * GL thread. The helper makes its (shared) context current on its own thread, so
 * any native call we post to {@link SurfaceTextureHelper#getHandler()} runs with
 * that context current — exactly what {@code eglCreateImageKHR} /
 * {@code glEGLImageTargetTexture2DOES} / {@code eglWaitSyncKHR} require. The same
 * handler and a {@link YuvConverter} created on it back the
 * {@link TextureBufferImpl} {@code toI420()} fallback used by the SW encoder /
 * screenshots.
 */
final class CustomVideoFrameDelivery {
    static {
        // Loads the native library backing the GL import/sync entry points below.
        System.loadLibrary("webrtc-custom-video-track");
    }

    private static final String TAG = WebRTCModule.TAG;
    // Must exceed the native fence-wait timeout (2s in custom_video_gl.cpp) plus
    // queue latency, so a single slow-but-legitimate fence wait does not trip the
    // drain warning.
    private static final long DRAIN_TIMEOUT_MS = 3_000;

    private final VideoSource videoSource;
    private final int width;
    private final int height;

    /** Dedicated shared-context GL thread (handler + EGL context current on it). */
    private final SurfaceTextureHelper surfaceTextureHelper;
    private final Handler glHandler;
    /** Created on {@link #glHandler}; backs TextureBufferImpl.toI420(). */
    private YuvConverter yuvConverter;

    /**
     * Per-pool-index AHB handles (AHardwareBuffer* as longs), index-stable, for the
     * pooled path. {@code null} in forwarding mode (external buffers carry their own
     * pointer/size per frame and are not pool-indexed).
     */
    private final long[] bufferHandles;
    /** Per-index cached EGLImageKHR (as long); 0 until first import. Pooled only. */
    private final long[] cachedEglImages;
    /** Per-index cached OES GL texture id; 0 until first import. Pooled only. */
    private final int[] cachedTextureIds;

    /** Lifecycle / drain bookkeeping, guarded by {@code stateLock}. */
    private final Object stateLock = new Object();
    private boolean accepting = false;
    private int inFlightCount = 0;
    /** Incremented whenever delivery is paused, resumed, or released. */
    private long generation = 0;
    /** Set by {@link #releaseGlResources()}: OES textures/EGLImages freed, STH disposed. */
    private boolean glResourcesReleased = false;

    /**
     * Outstanding acquired references on forwarded AHBs, keyed by handle with a
     * count (the same buffer pointer recurs when the producer recycles it).
     * Incremented next to {@link #nativeAcquireAhb}; decremented by
     * {@link #releaseForwardedAhbReference}. Whatever is left after the GL thread
     * is gone (runnables discarded at looper quit can no longer balance their
     * acquires) is force-released in {@link #releaseGlResources()}; the map is the
     * source of truth, so a late release callback finding it empty releases
     * nothing and cannot double-release.
     */
    private final ConcurrentHashMap<Long, Integer> outstandingForwardedAhbs = new ConcurrentHashMap<>();

    /** Identity transform: the WebGPU render already produced an upright RGBA image. */
    private static final Matrix IDENTITY_MATRIX = new Matrix();

    CustomVideoFrameDelivery(VideoSource videoSource, long[] bufferHandles, int width, int height) {
        this.videoSource = videoSource;
        this.bufferHandles = bufferHandles;
        this.width = width;
        this.height = height;
        // Forwarding mode has no pool, so no per-index caches (length 0).
        int poolSize = bufferHandles != null ? bufferHandles.length : 0;
        this.cachedEglImages = new long[poolSize];
        this.cachedTextureIds = new int[poolSize];

        EglBase.Context rootContext = EglUtils.getRootEglBaseContext();
        if (rootContext == null) {
            throw new IllegalStateException("Root EGL context unavailable; cannot deliver custom video frames");
        }
        // Handler-thread EGL context is share-linked to the root, so the imported
        // OES texture is usable by WebRTC's encoder GL thread.
        this.surfaceTextureHelper = SurfaceTextureHelper.create("CustomVideoGL", rootContext);
        if (surfaceTextureHelper == null) {
            throw new IllegalStateException("SurfaceTextureHelper.create returned null");
        }
        this.glHandler = surfaceTextureHelper.getHandler();
        // YuvConverter must be created on the GL thread (it allocates GL resources).
        this.glHandler.post(() -> yuvConverter = new YuvConverter());
    }

    /** Begin accepting pushed frames. */
    void start() {
        synchronized (stateLock) {
            if (glResourcesReleased) {
                return;
            }
            generation++;
            accepting = true;
        }
    }

    /**
     * Pushes one app-rendered frame. Fire-and-forget: schedules the import +
     * fence wait + delivery on the GL thread and returns immediately.
     *
     * @param bufferIndex pool index of the AHB the app rendered into.
     * @param fenceFd     dup'd sync-fd of the GPU render-complete fence, or -1 for
     *                    the no-fence fallback (deliver immediately). EGL takes
     *                    ownership of the fd on the GL thread.
     * @param timestampNs frame presentation timestamp in nanoseconds.
     * @param rotation    frame rotation in degrees (0/90/180/270).
     */
    void pushFrame(int bufferIndex, int fenceFd, long timestampNs, int rotation) {
        if (bufferHandles == null) {
            // Forwarding-mode delivery received a pooled push (JS misuse); drop it.
            Log.w(TAG, "pushFrame on a forwarding track; dropping frame");
            closeFd(fenceFd);
            return;
        }
        if (bufferIndex < 0 || bufferIndex >= bufferHandles.length) {
            Log.w(TAG, "pushFrame: bufferIndex " + bufferIndex + " out of range");
            closeFd(fenceFd);
            return;
        }

        // Reserve an in-flight slot only while accepting, and capture the current
        // generation. A pause/resume/release bumps generation so this runnable can
        // later tell whether it is stale and should drop instead of deliver.
        final long frameGeneration;
        synchronized (stateLock) {
            if (!accepting || glResourcesReleased) {
                closeFd(fenceFd);
                return;
            }
            frameGeneration = generation;
            inFlightCount++;
        }

        boolean posted = glHandler.post(() -> {
            // The fd is owned by this Runnable until handed to nativeWaitSyncFd
            // (which transfers ownership to EGL). Track it so the error paths close
            // it exactly once and never double-close after the hand-off.
            int[] ownedFd = {fenceFd};
            try {
                deliverOnGlThread(bufferIndex, ownedFd, frameGeneration, timestampNs, rotation);
            } catch (Throwable t) {
                Log.e(TAG, "pushFrame: delivery failed for index " + bufferIndex, t);
            } finally {
                // Close the fence fd iff it was never handed to native (import
                // failure or a throw before nativeWaitSyncFd). After hand-off
                // ownedFd[0] is -1, so this is a no-op.
                closeFd(ownedFd[0]);
                finishInFlight();
            }
        });
        if (!posted) {
            // GL thread already gone (a teardown raced this push): the runnable
            // will never run, so balance its bookkeeping here.
            closeFd(fenceFd);
            finishInFlight();
        }
    }

    /** Runs entirely on the GL thread (shared EGL context current). */
    private void deliverOnGlThread(
            int bufferIndex, int[] ownedFd, long frameGeneration, long timestampNs, int rotation) {
        if (!shouldDeliver(frameGeneration)) {
            return;
        }

        // 1. Import (once) the AHB at this index into an OES texture; reuse after.
        int textureId = cachedTextureIds[bufferIndex];
        if (textureId == 0) {
            long[] imported = nativeImportAhbToOesTexture(bufferHandles[bufferIndex]);
            if (imported == null || imported.length != 2 || imported[1] == 0) {
                Log.e(TAG, "AHB import failed for index " + bufferIndex);
                return; // outer catch/finally closes ownedFd[0]
            }
            cachedEglImages[bufferIndex] = imported[0];
            cachedTextureIds[bufferIndex] = (int) imported[1];
            textureId = cachedTextureIds[bufferIndex];
        }

        // 2. Wait the GPU fence BEFORE the encoder samples this texture. This is a
        //    client (CPU) wait that blocks THIS GL delivery thread until the render
        //    completes, so the texture is gated for the encoder's separate context
        //    too (a server-side wait would only order this context). EGL takes
        //    ownership of the fd here (<0 is a no-op), so drop our ownership
        //    immediately to avoid a double-close on any later throw.
        int fenceFd = ownedFd[0];
        ownedFd[0] = -1;
        if (!nativeWaitSyncFd(fenceFd)) {
            Log.w(TAG, "pushFrame: timed out or failed waiting for GPU fence");
            return;
        }

        if (!shouldDeliver(frameGeneration)) {
            return;
        }

        // 3. Wrap the OES texture as a VideoFrame and deliver. The release callback
        //    is a no-op: the texture is pool-owned and reused across frames, NOT
        //    freed per frame (freed in release() on teardown). TextureBufferImpl
        //    needs the GL-thread handler + a YuvConverter on it for toI420().
        TextureBufferImpl buffer = new TextureBufferImpl(width,
                height,
                VideoFrame.TextureBuffer.Type.OES,
                textureId,
                IDENTITY_MATRIX,
                glHandler,
                yuvConverter,
                /* releaseCallback */ () -> {});
        VideoFrame frame = new VideoFrame(buffer, rotation, timestampNs);
        try {
            videoSource.getCapturerObserver().onFrameCaptured(frame);
        } finally {
            frame.release(); // balances TextureBufferImpl's initial +1 ref
        }
    }

    /**
     * Forwards one finished external {@code AHardwareBuffer*} (forwarding mode).
     *
     * <p>Takes an owning reference on the AHB <em>synchronously on the calling
     * (worklet) thread</em>, BEFORE returning, so the buffer survives the caller's
     * {@code frame.dispose()} that runs immediately after the push. The import into
     * an OES texture and the delivery then run on the GL thread. Unlike the pooled
     * path there is no fence wait (a forwarded buffer is already complete) and no
     * index caching (external pointers are unbounded, so the texture/EGLImage are
     * created per frame and destroyed by the {@code VideoFrame} release callback).
     *
     * @param ahbHandle   the finished {@code AHardwareBuffer*} (as a {@code long}).
     * @param timestampNs frame presentation timestamp in nanoseconds; {@code 0}
     *                    means stamp with {@code System.nanoTime()} at delivery.
     * @param rotation    frame rotation in degrees (0/90/180/270).
     */
    void pushExternalBuffer(long ahbHandle, long timestampNs, int rotation) {
        if (ahbHandle == 0) {
            return;
        }

        // Reserve an in-flight slot only while accepting, and capture the current
        // generation so a later pause/resume/release can tell this runnable is stale.
        final long frameGeneration;
        synchronized (stateLock) {
            if (!accepting || glResourcesReleased) {
                return;
            }
            frameGeneration = generation;
            inFlightCount++;
        }

        // Take the owning ref NOW, on the caller's thread, before it releases its
        // own reference, and record it in the outstanding map. The GL runnable (or
        // the frame's release callback) balances it with exactly one
        // releaseForwardedAhbReference.
        nativeAcquireAhb(ahbHandle);
        outstandingForwardedAhbs.merge(ahbHandle, 1, Integer::sum);

        boolean posted = glHandler.post(() -> {
            // The AHB is owned by this Runnable until ownership passes to the
            // delivered frame's release callback; ownedAhb[0] is zeroed on that
            // hand-off so the finally does not double-release.
            long[] ownedAhb = {ahbHandle};
            try {
                deliverExternalOnGlThread(ownedAhb, frameGeneration, timestampNs, rotation);
            } catch (Throwable t) {
                Log.e(TAG, "pushExternalBuffer: delivery failed", t);
            } finally {
                if (ownedAhb[0] != 0) {
                    releaseForwardedAhbReference(ownedAhb[0]);
                }
                finishInFlight();
            }
        });
        if (!posted) {
            // GL thread already gone (a teardown raced this push): the runnable
            // will never run, so balance its bookkeeping here.
            releaseForwardedAhbReference(ahbHandle);
            finishInFlight();
        }
    }

    /** Runs entirely on the GL thread (shared EGL context current). */
    private void deliverExternalOnGlThread(long[] ownedAhb, long frameGeneration, long timestampNs, int rotation) {
        long ahbHandle = ownedAhb[0];
        if (!shouldDeliver(frameGeneration)) {
            return; // outer finally releases the AHB
        }

        // External buffers carry their own dimensions (no pool geometry to reuse).
        int[] dimensions = nativeDescribeAhb(ahbHandle);
        if (dimensions == null || dimensions.length != 2 || dimensions[0] <= 0 || dimensions[1] <= 0) {
            Log.e(TAG, "AHB describe failed for forwarded buffer");
            return;
        }

        // Per-frame import (external pointers are unbounded, so nothing to cache).
        long[] imported = nativeImportAhbToOesTexture(ahbHandle);
        if (imported == null || imported.length != 2 || imported[1] == 0) {
            Log.e(TAG, "AHB import failed for forwarded buffer");
            return;
        }
        long eglImage = imported[0];
        int textureId = (int) imported[1];

        if (!shouldDeliver(frameGeneration)) {
            nativeReleaseImportedTexture(eglImage, textureId); // AHB released by finally
            return;
        }

        // A raw buffer pointer carries no presentation time; stamp a monotonic
        // timestamp when JS did not supply one.
        long stampNs = timestampNs != 0 ? timestampNs : System.nanoTime();

        // Ownership of {AHB, EGLImage, OES texture} now transfers to the frame's
        // release callback, fired when the encoder drops its last reference.
        ownedAhb[0] = 0;
        TextureBufferImpl buffer = new TextureBufferImpl(dimensions[0],
                dimensions[1],
                VideoFrame.TextureBuffer.Type.OES,
                textureId,
                IDENTITY_MATRIX,
                glHandler,
                yuvConverter,
                () -> releaseForwardedFrame(eglImage, textureId, ahbHandle));
        VideoFrame frame = new VideoFrame(buffer, rotation, stampNs);
        try {
            videoSource.getCapturerObserver().onFrameCaptured(frame);
        } finally {
            frame.release(); // balances TextureBufferImpl's initial +1 ref
        }
    }

    /**
     * Frees one forwarded frame's per-frame GL import and its acquired AHB ref.
     * Invoked from {@link TextureBufferImpl}'s release callback when the encoder
     * drops its last reference — on whatever thread that release happens, so the GL
     * teardown is posted onto the GL thread. If the GL thread is already gone
     * (teardown raced ahead) the texture/EGLImage die with the context; the AHB
     * release is thread-safe, so we still balance the acquire directly.
     */
    private void releaseForwardedFrame(long eglImage, int textureId, long ahbHandle) {
        boolean posted = glHandler.post(() -> {
            nativeReleaseImportedTexture(eglImage, textureId);
            releaseForwardedAhbReference(ahbHandle);
        });
        if (!posted) {
            releaseForwardedAhbReference(ahbHandle);
        }
    }

    /**
     * Balances one {@link #nativeAcquireAhb(long)}, but only if the outstanding map
     * still records it — {@link #releaseGlResources()} may have already
     * force-released leftovers, and releasing past that would drop a reference we
     * no longer own. Thread-safe; the map is the single source of truth.
     */
    private void releaseForwardedAhbReference(long ahbHandle) {
        boolean[] owned = {false};
        outstandingForwardedAhbs.computeIfPresent(ahbHandle, (handle, count) -> {
            owned[0] = true;
            return count == 1 ? null : count - 1;
        });
        if (owned[0]) {
            nativeReleaseAhb(ahbHandle);
        }
    }

    private void finishInFlight() {
        synchronized (stateLock) {
            inFlightCount--;
            if (inFlightCount <= 0) {
                stateLock.notifyAll();
            }
        }
    }

    private boolean shouldDeliver(long frameGeneration) {
        synchronized (stateLock) {
            return accepting && !glResourcesReleased && generation == frameGeneration;
        }
    }

    /**
     * Full teardown: {@link #drain()} then {@link #releaseGlResources()}. Idempotent.
     *
     * <p>Used as the safety-net path. The correct custom-video teardown ordering
     * (see {@code GetUserMediaImpl.TrackPrivate.dispose}) calls the two halves
     * separately so the {@code VideoSource}/{@code VideoTrack} can be disposed
     * BETWEEN them — the encoder must be quiesced before the GL textures/AHBs are
     * freed, otherwise it samples a deleted texture / freed buffer (UAF).
     */
    void release() {
        drain();
        releaseGlResources();
    }

    /**
     * Stops accepting new pushes and blocks until every in-flight delivery runnable
     * has run, so no Runnable is still using a cached OES texture / AHB. Does NOT
     * free GL resources — call {@link #releaseGlResources()} for that, but only
     * AFTER the {@code VideoSource}/{@code VideoTrack} have been disposed so the
     * encoder is quiesced and no longer retains a delivered frame. Idempotent.
     */
    void drain() {
        synchronized (stateLock) {
            if (glResourcesReleased) {
                return;
            }
            accepting = false;
            generation++;
            long deadline = SystemClock.uptimeMillis() + DRAIN_TIMEOUT_MS;
            while (inFlightCount > 0) {
                long remainingMs = deadline - SystemClock.uptimeMillis();
                if (remainingMs <= 0) {
                    Log.w(TAG, "Timed out waiting for custom video frame delivery to drain");
                    break;
                }
                try {
                    stateLock.wait(remainingMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Frees every cached EGLImage/OES texture on the GL thread, then disposes the
     * {@link SurfaceTextureHelper}. MUST be called after {@link #drain()} AND after
     * the encoder has been quiesced (its {@code VideoSource}/{@code VideoTrack}
     * disposed), so no encoder context can still sample the textures. Idempotent.
     */
    void releaseGlResources() {
        synchronized (stateLock) {
            if (glResourcesReleased) {
                return;
            }
            accepting = false;
            generation++;
            glResourcesReleased = true;
        }

        // Free GL resources on the GL thread (context current there), then dispose.
        final Object done = new Object();
        final boolean[] finished = {false};
        synchronized (done) {
            glHandler.post(() -> {
                for (int index = 0; index < cachedTextureIds.length; index++) {
                    if (cachedTextureIds[index] != 0 || cachedEglImages[index] != 0) {
                        nativeReleaseImportedTexture(cachedEglImages[index], cachedTextureIds[index]);
                        cachedEglImages[index] = 0;
                        cachedTextureIds[index] = 0;
                    }
                }
                if (yuvConverter != null) {
                    yuvConverter.release();
                    yuvConverter = null;
                }
                synchronized (done) {
                    finished[0] = true;
                    done.notifyAll();
                }
            });
            try {
                done.wait(DRAIN_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (!finished[0]) {
            Log.w(TAG, "Timed out releasing custom video GL resources");
        }

        surfaceTextureHelper.dispose();

        // The looper is gone: any release runnable still queued was discarded and
        // can no longer balance its forwarded-AHB acquire. Force-release whatever
        // the outstanding map still records; late release callbacks then find the
        // map empty and release nothing (no double-release).
        for (Long handle : outstandingForwardedAhbs.keySet()) {
            Integer count = outstandingForwardedAhbs.remove(handle);
            if (count != null) {
                for (int i = 0; i < count; i++) {
                    nativeReleaseAhb(handle);
                }
            }
        }
    }

    /**
     * Closes a fence fd that never reached native ownership (e.g. out-of-range
     * index, or pushed while not accepting). Once the fd is handed to
     * {@link #nativeWaitSyncFd(int)} it is owned/closed by native/EGL, so Java
     * must NOT also close it on those paths.
     */
    private static void closeFd(int fenceFd) {
        if (fenceFd >= 0) {
            nativeCloseFd(fenceFd);
        }
    }

    /**
     * Closes a stray fence sync-fd carried on a <em>forwarding</em> push. Forwarding
     * never uses a fence and the typed {@code forwardFrame} wrapper forbids one, but
     * the JSI layer parses {@code fence} unconditionally, so a raw-sink caller can
     * still attach one. Its fd ownership transferred to native on push (same
     * contract as the pooled path), so drop it here rather than leak an fd per frame.
     * No-op for the no-fence sentinel ({@code 0} or beyond the {@code int} fd range).
     */
    static void closeFenceHandle(long fenceHandle) {
        if (fenceHandle > 0 && fenceHandle <= Integer.MAX_VALUE) {
            nativeCloseFd((int) fenceHandle);
        }
    }

    // --- Native (custom_video_gl.cpp); GL ones MUST be called on the GL thread. ---

    private static native long[] nativeImportAhbToOesTexture(long ahbHandle);

    private static native boolean nativeWaitSyncFd(int fenceFd);

    private static native void nativeReleaseImportedTexture(long eglImageHandle, int texId);

    /** Closes a raw fd; safe to call from any thread (no GL/EGL state touched). */
    private static native void nativeCloseFd(int fenceFd);

    // --- Forwarding-mode AHB ref-counting/describe; thread-safe (no GL/EGL state). ---

    /** Takes one owning ref on a forwarded AHB; called on the worklet push thread. */
    private static native void nativeAcquireAhb(long ahbHandle);

    /** Releases one ref taken by {@link #nativeAcquireAhb(long)}. */
    private static native void nativeReleaseAhb(long ahbHandle);

    /** Returns {@code {width, height}} of a forwarded AHB, or null on failure. */
    private static native int[] nativeDescribeAhb(long ahbHandle);
}
