package com.oney.WebRTCModule;

import com.facebook.jni.HybridData;
import com.facebook.proguard.annotations.DoNotStrip;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.turbomodule.core.CallInvokerHolderImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Installs the JS global {@code __fishjamWebrtcGetCustomVideoSink(trackId)} through
 * which the JS SDK obtains the per-track sink used to push custom video frames.
 *
 * <p>A JSI global must be set on the JS thread with the live runtime, which a
 * React method (running on the native-modules thread) can't do directly. So we
 * pass the React {@link CallInvokerHolderImpl} down to C++ ({@link #initHybrid}),
 * and the shared native {@code FJVideoPush} hops onto the JS thread via that
 * CallInvoker, sets the global, and calls {@link #onPushInstalled()} back here.
 * The install Promise is resolved only then, so JS never observes a missing
 * global.
 *
 * <p>Every frame the global pushes is forwarded by C++ back to
 * {@link #deliverFrame} (on the JS thread), which routes it through the
 * {@link FrameRouter} supplied by {@link WebRTCModule} to the matching
 * {@link CustomVideoFrameDelivery}.
 */
@DoNotStrip
final class FJVideoPushInstaller {
    static {
        System.loadLibrary("webrtc-custom-video-track");
    }

    /**
     * Routes one JS-pushed frame to its track's delivery engine. {@code nativeBuffer}
     * is the forwarding discriminator: non-zero is a finished {@code AHardwareBuffer*}
     * to forward; zero means pooled delivery of {@code bufferIndex}.
     */
    interface FrameRouter {
        void route(String trackId, int bufferIndex, long nativeBuffer, long timestampNs, int rotation, long fenceHandle,
                long fenceSignaledValue);
    }

    private final HybridData mHybridData;
    private final FrameRouter frameRouter;

    // Callers waiting for the JSI global to be installed. Guarded by `this`.
    private final List<Promise> pendingInstalls = new ArrayList<>();

    FJVideoPushInstaller(ReactApplicationContext reactContext, FrameRouter frameRouter) {
        this.frameRouter = frameRouter;
        mHybridData = initHybrid((CallInvokerHolderImpl) reactContext.getJSCallInvokerHolder());
    }

    /**
     * Installs (or re-installs) the JSI global, resolving {@code promise} once it
     * is in place.
     *
     * <p>Always (re)invokes the native install, never short-circuiting on any
     * cached "already installed" state: after a JS reload this installer (and the
     * owning native module) persist while the JS runtime is recreated, so caching
     * would leave the global unset on the new runtime — and batching behind an
     * earlier in-flight install would wedge permanently if that install's hop to
     * the JS thread was dropped in a reload race (JS's timeout-retry could then
     * never trigger another native install). {@code FJVideoPush::install} owns
     * idempotency — it resets its installed flag and re-sets the global — so
     * redundant calls are harmless: the first completion resolves every pending
     * promise, later completions find the list empty.
     */
    void install(Promise promise) {
        synchronized (this) {
            pendingInstalls.add(promise);
        }
        installPush();
    }

    /** Invoked from C++ on the JS thread once the global has been set. */
    @DoNotStrip
    private void onPushInstalled() {
        List<Promise> promises;
        synchronized (this) {
            promises = new ArrayList<>(pendingInstalls);
            pendingInstalls.clear();
        }
        for (Promise p : promises) {
            p.resolve(true);
        }
    }

    /**
     * Invoked from C++ for every frame pushed through the JSI global. Runs on
     * whatever thread called {@code sink.push} — the RN JS thread or a
     * frame-processor worklet thread (attached to the JVM by the C++ ThreadScope) —
     * so the router must be thread-safe and dispatch synchronously (the forwarding
     * path must retain {@code nativeBuffer} before this returns). {@code nativeBuffer}
     * non-zero is a finished {@code AHardwareBuffer*} to forward; zero means pooled
     * delivery of {@code bufferIndex}. {@code fenceHandle} is a sync-fd
     * ({@code 0} = no fence); {@code fenceSignaledValue} is unused on Android.
     */
    @DoNotStrip
    private void deliverFrame(String trackId, int bufferIndex, long nativeBuffer, long timestampNs, int rotation,
            long fenceHandle, long fenceSignaledValue) {
        frameRouter.route(trackId, bufferIndex, nativeBuffer, timestampNs, rotation, fenceHandle, fenceSignaledValue);
    }

    @DoNotStrip
    private native HybridData initHybrid(CallInvokerHolderImpl callInvokerHolder);

    @DoNotStrip
    private native void installPush();
}
