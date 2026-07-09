package com.oney.WebRTCModule;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

/**
 * A fixed-size pool of {@code AHardwareBuffer} (AHB) backed surfaces for a custom
 * video track in <em>pooled</em> mode: the app renders GPU frames directly into
 * these buffers (imported by {@code surfaceHandle} via react-native-webgpu) and
 * pushes them back by index.
 *
 * <p>The pool is owned independently of any capture controller (it maps 1:1 to a
 * track, but its lifetime is managed by the JS {@code CustomVideoBufferPool.dispose}
 * path via {@code releaseCustomVideoBufferPool}, not by track teardown). It holds:
 * <ul>
 *   <li>the buffer dimensions,</li>
 *   <li>the index-stable AHB handles (JS imports each surface exactly once and
 *       addresses it by index forever after),</li>
 *   <li>the JS-facing {@code bufferDescriptors},</li>
 *   <li>an {@link #tryAttach attached} flag enforcing the 1&lt;-&gt;1 pool/track
 *       relationship.</li>
 * </ul>
 *
 * <p>Requires API level 26+ (the AHB APIs are {@code __INTRODUCED_IN(26)}); callers
 * reject on {@code SDK_INT < 26} before referencing this class, which would
 * otherwise trigger {@link AHardwareBufferAllocator}'s {@code System.loadLibrary}.
 */
final class CustomVideoBufferPool {
    private final int width;
    private final int height;

    /**
     * Native {@code AHardwareBuffer*} handles (as {@code long}s), index-stable for
     * the lifetime of the pool. JS imports each handle exactly once (by its
     * {@code index}) and renders into it forever after.
     */
    private final long[] bufferHandles;

    /** True once bound to a track; enforces the 1&lt;-&gt;1 pool/track relationship. */
    private boolean attached = false;
    /**
     * The controller of the track this pool is bound to; null until attached.
     * Used by {@code releaseCustomVideoBufferPool} to refuse disposal while the
     * track is still live (its delivery may hold in-flight references to these
     * AHBs).
     */
    private CustomVideoCaptureController attachedController;
    private boolean disposed = false;

    /**
     * Allocates {@code poolSize} RGBA8
     * {@code GPU_FRAMEBUFFER | GPU_SAMPLED_IMAGE} AHBs so the surface handles are
     * available immediately for the JS resolve.
     *
     * @param width    pixel width of every AHB.
     * @param height   pixel height of every AHB.
     * @param poolSize number of AHBs to pre-allocate (max frames in flight).
     * @throws IllegalArgumentException if width/height/poolSize are not positive.
     * @throws RuntimeException         if any AHB allocation fails.
     */
    CustomVideoBufferPool(int width, int height, int poolSize) {
        if (width <= 0 || height <= 0 || poolSize <= 0) {
            throw new IllegalArgumentException("width, height and poolSize must all be positive");
        }
        this.width = width;
        this.height = height;

        bufferHandles = new long[poolSize];
        for (int index = 0; index < poolSize; index++) {
            long handle = AHardwareBufferAllocator.allocateFramebufferAHB(width, height);
            if (handle == 0) {
                // Release whatever we already allocated before bailing out.
                for (int released = 0; released < index; released++) {
                    AHardwareBufferAllocator.releaseAHB(bufferHandles[released]);
                    bufferHandles[released] = 0;
                }
                throw new RuntimeException(
                        "AHardwareBuffer allocation failed at index " + index + " (" + width + "x" + height + ")");
            }
            bufferHandles[index] = handle;
        }
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    /**
     * Index-stable AHB handles ({@code AHardwareBuffer*} as {@code long}s). The
     * pooled {@link CustomVideoFrameDelivery} reads a rendered buffer by index.
     */
    long[] getBufferHandles() {
        return bufferHandles;
    }

    /**
     * Atomically binds this pool to a track's controller. Returns {@code false} if
     * it was already attached (a pool binds to exactly one track); {@code true} on
     * the first attach.
     */
    synchronized boolean tryAttach(CustomVideoCaptureController controller) {
        if (attached) {
            return false;
        }
        attached = true;
        attachedController = controller;
        return true;
    }

    /**
     * Whether the track this pool is bound to is still live (its controller has not
     * released its GPU resources). While true, disposing the pool would race
     * in-flight frame deliveries reading these AHB handles.
     */
    synchronized boolean isAttachedToLiveTrack() {
        return attachedController != null && !attachedController.isReleased();
    }

    /**
     * Builds the JS-facing buffer descriptors, one per AHB, in index order. Shape
     * matches iOS and the {@code createCustomVideoTrack.ts} contract:
     * {@code { index, surfaceHandle: String(handle), width, height }}. The handle is
     * a decimal string of the {@code AHardwareBuffer*} so a 64-bit pointer survives
     * JS; convert with {@code BigInt(surfaceHandle)} before
     * {@code device.importSharedTextureMemory({ handle })}.
     */
    WritableArray getBufferDescriptors() {
        WritableArray descriptors = Arguments.createArray();
        for (int index = 0; index < bufferHandles.length; index++) {
            WritableMap descriptor = Arguments.createMap();
            descriptor.putInt("index", index);
            descriptor.putString("surfaceHandle", Long.toString(bufferHandles[index]));
            descriptor.putInt("width", width);
            descriptor.putInt("height", height);
            descriptors.pushMap(descriptor);
        }
        return descriptors;
    }

    /**
     * Permanently releases all AHBs. Idempotent: a second call is a no-op. Must be
     * called only after the track bound to this pool has stopped and its GL imports
     * (OES textures aliasing these AHBs) have been freed, so no OES texture / encoder
     * still references a freed buffer.
     */
    synchronized void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (int index = 0; index < bufferHandles.length; index++) {
            if (bufferHandles[index] != 0) {
                AHardwareBufferAllocator.releaseAHB(bufferHandles[index]);
                bufferHandles[index] = 0;
            }
        }
    }
}
