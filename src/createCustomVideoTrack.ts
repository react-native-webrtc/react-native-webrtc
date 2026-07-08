/**
 * Custom video track.
 *
 * Feed your own frames into a WebRTC video track. There are two modes, and you
 * pick by *how you produce frames*:
 *
 * - **Pooled** — *you render the frames yourself* (for example with
 *   react-native-webgpu). Allocate a pool of native surfaces with
 *   {@link createCustomVideoBufferPool}, render into them, create a track over the
 *   pool with {@link createCustomVideoTrack}, and hand each frame back with
 *   {@link pushFrame} (by pool-slot index, with an optional GPU fence).
 *
 * - **Forwarding** — *the frames are already produced natively* (a camera,
 *   VisionCamera, a native ML pipeline, a compositor) and you only want to forward
 *   the finished buffer. Create a track with no pool via {@link createCustomVideoTrack},
 *   and forward each buffer pointer with {@link forwardFrame}.
 *
 * ```ts
 * // Pooled — render in JS:
 * const pool = await createCustomVideoBufferPool({ width, height, poolSize: 3 });
 * const { stream, track } = await createCustomVideoTrack({ pool });
 * // ...render into pool.buffers[i]...
 * pushFrame(track, { bufferIndex, timestampNs, fence });
 *
 * // Forwarding — hand over a finished native buffer:
 * const { stream, track } = await createCustomVideoTrack();
 * forwardFrame(track, { nativeBuffer }); // bigint CVPixelBufferRef / AHardwareBuffer*
 * ```
 *
 * New Architecture only: {@link createCustomVideoTrack} rejects with a clear error
 * on the old architecture, where the per-frame JSI channel is unavailable.
 *
 * Pooled mode needs GPU-shareable native surfaces and should be exercised on a
 * physical device — the iOS Simulator's GPU stack does not reliably back the
 * shared `IOSurface` import path.
 *
 * @module createCustomVideoTrack
 */
import { NativeModules } from 'react-native';

import MediaStream from './MediaStream';
import MediaStreamError from './MediaStreamError';
import type { MediaStreamTrackInfo } from './MediaStreamTrack';

const { WebRTCModule } = NativeModules;

// Installed natively once the JSI binding is in place (see installCustomVideoJSI).
// Returns the per-track host object used for hop-free pushes.
declare const global: {
    __fishjamWebrtcGetCustomVideoSink?: (trackId: string) => CustomVideoSink;
};

// The old architecture has no JSI-capable invoker, so the install may never
// resolve — cap the wait and reject rather than hang.
const INSTALL_TIMEOUT_MS = 10_000;

let installPromise: Promise<void> | null = null;

function normalizeInstallError(cause: unknown): Error {
    if (cause instanceof Error) {
        return (cause as { code?: string }).code === 'E_NO_JSI'
            ? new Error('Custom video tracks require the New Architecture.')
            : cause;
    }
    return new Error(`Custom video track install failed: ${String(cause)}`);
}

function invalidInitError(message: string): Error {
    const error = new Error(message) as Error & { code: string };
    error.code = 'E_INVALID_CUSTOM_VIDEO_BUFFER_POOL_INIT';
    return error;
}

function validatePoolInit(init: CustomVideoBufferPoolInit): void {
    if (
        !Number.isInteger(init?.width) ||
        !Number.isInteger(init?.height) ||
        !Number.isInteger(init?.poolSize) ||
        init.width <= 0 ||
        init.height <= 0 ||
        init.poolSize <= 0
    ) {
        throw invalidInitError(
            'Custom video buffer pool width, height, and poolSize must be positive integers.',
        );
    }
}

// Install the native JSI binding once. Re-runnable after a JS reload.
function ensureInstalled(): Promise<void> {
    if (installPromise) {
        return installPromise;
    }
    let timeoutId!: ReturnType<typeof setTimeout>;
    const timeout = new Promise<never>((_, reject) => {
        timeoutId = setTimeout(
            () => reject(new Error('Custom video track install timed out.')),
            INSTALL_TIMEOUT_MS,
        );
    });
    const install = WebRTCModule.installCustomVideoJSI().then(() => {
        if (typeof global.__fishjamWebrtcGetCustomVideoSink !== 'function') {
            throw new Error('Custom video track binding was not installed.');
        }
    });
    installPromise = Promise.race([install, timeout])
        .finally(() => clearTimeout(timeoutId))
        .catch((cause: unknown) => {
            installPromise = null;
            throw normalizeInstallError(cause);
        });
    return installPromise;
}

// Shape of one pooled surface as it arrives over the React Native bridge, where
// the 64-bit surface handle is carried as a decimal string to avoid losing
// precision through a JS number.
type BridgeCustomVideoBuffer = {
    index: number;
    surfaceHandle: string;
    width: number;
    height: number;
};

type BridgeCustomVideoBufferPool = {
    poolId: string;
    buffers: BridgeCustomVideoBuffer[];
};

type BridgeCustomVideoTrack = {
    streamId: string;
    track: MediaStreamTrackInfo;
};

/**
 * Settings for {@link createCustomVideoBufferPool}, describing the pool of native
 * surfaces you will render into.
 */
export interface CustomVideoBufferPoolInit {
    /**
     * Width of every pooled surface, in pixels. Becomes the encoded width of any
     * pooled track fed from this pool.
     */
    width: number;
    /**
     * Height of every pooled surface, in pixels. Becomes the encoded height of any
     * pooled track fed from this pool.
     */
    height: number;
    /**
     * Number of in-flight surfaces to allocate (at least `1`, typically `2`–`3`).
     *
     * A frame you push may still be in use — being encoded and delivered — when
     * you want to start drawing the next one. Redrawing a surface that is still
     * being read would tear that frame. With a pool you render into a different
     * slot each time, cycling round-robin over the {@link CustomVideoBufferPool.buffers},
     * so the producer never overwrites a buffer still being consumed.
     */
    poolSize: number;
}

/**
 * One surface in a {@link CustomVideoBufferPool}. Import it into your GPU once, up
 * front, and reference it by {@link CustomVideoBuffer.index} when pushing a frame.
 */
export interface CustomVideoBuffer {
    /**
     * Stable index of this surface within the pool (`0` to `poolSize - 1`). Pass it
     * back as `pushFrame(track, { bufferIndex })`. Never changes for the pool's
     * lifetime.
     */
    index: number;
    /**
     * The 64-bit native surface handle (an `IOSurface` on iOS, an `AHardwareBuffer`
     * on Android), as a `bigint`. Import it into your GPU as a render target (for
     * example react-native-webgpu:
     * `device.importSharedTextureMemory({ handle: surfaceHandle })`). Import each
     * surface once and reuse it for every frame you draw into that slot.
     *
     * **Render into the platform's native surface format**, or the frame comes out
     * garbled (red/blue swapped) or is rejected at import:
     * - **iOS** — `BGRA8` (`bgra8unorm`; the surface is `kCVPixelFormatType_32BGRA`).
     * - **Android** — `RGBA8` (`rgba8unorm`).
     */
    surfaceHandle: bigint;
    /** Width of this surface in pixels; matches {@link CustomVideoBufferPoolInit.width}. */
    width: number;
    /** Height of this surface in pixels; matches {@link CustomVideoBufferPoolInit.height}. */
    height: number;
}

/**
 * A pool of native surfaces you render into, owned independently of any track.
 * Returned by {@link createCustomVideoBufferPool}; attach it to a pooled track via
 * {@link createCustomVideoTrack}, and free it yourself with {@link CustomVideoBufferPool.dispose}
 * once the track has stopped.
 */
export interface CustomVideoBufferPool {
    /** Opaque id identifying this pool to the native layer. */
    poolId: string;
    /** The pooled surfaces, one entry per `poolSize`; import each into your GPU once. */
    buffers: CustomVideoBuffer[];
    /**
     * Free the pool's native surfaces. Call it after the track bound to this pool
     * has stopped. Stopping the track does *not* free the pool — you own it.
     * Rejects while the bound track is still live; safe to retry after stopping
     * the track. After a successful dispose, further calls are no-ops.
     */
    dispose(): Promise<void>;
}

/**
 * An optional GPU completion fence that tells the encoder when your rendering into
 * a surface has finished on the GPU, so it waits for the draw before reading the
 * frame. A standard platform GPU-sync primitive — not tied to any GPU library.
 */
export interface CustomVideoFrameFence {
    /**
     * The native GPU fence object, as a bigint:
     * - **iOS** — an `MTLSharedEvent` handle.
     * - **Android** — a `sync` file descriptor.
     */
    handle: bigint;
    /**
     * The value the fence is signaled to once this frame's GPU work completes. Used
     * on iOS (the encoder waits until the `MTLSharedEvent` reaches it); ignored on
     * Android, where a `sync` fd carries no value (pass `0n`).
     */
    signaledValue: bigint;
}

/**
 * The native per-track push channel handed back on a track handle's `sink`.
 *
 * Backed by a JSI host object, so it is shared *by reference* into a
 * frame-processor worklet — calling `push` there dispatches synchronously on the
 * worklet thread with no hop. You normally don't call this directly; use
 * {@link pushFrame} / {@link forwardFrame}. It is exposed so it can be captured
 * into a worklet setup where the wrappers aren't processed.
 *
 * Push from the *same* worklet runtime the sink was captured into: `push` binds
 * lazily to whichever runtime first resolves it, so importing one sink into two
 * runtimes and pushing from both is unsupported. `push` is stable per runtime, so
 * a hot loop may hoist it once (`const push = track.sink.push`) and reuse it.
 */
export interface CustomVideoSink {
    push(frame: object): void;
}

/**
 * Handle for a **pooled** custom video track (you render frames in JS). Plain and
 * worklet-serializable — store it (not the {@link MediaStream}) in the ref/shared
 * value your render/worklet loop reads, and pass it to {@link pushFrame}.
 */
export interface PooledTrack {
    readonly kind: 'pooled';
    /** Id of the underlying video track. */
    trackId: string;
    /** Native push channel; use {@link pushFrame} rather than calling it directly. */
    readonly sink: CustomVideoSink;
}

/**
 * Handle for a **forwarding** custom video track (frames produced natively). Plain
 * and worklet-serializable — store it (not the {@link MediaStream}) in the
 * ref/shared value your frame processor reads, and pass it to {@link forwardFrame}.
 */
export interface ForwardTrack {
    readonly kind: 'forward';
    /** Id of the underlying video track. */
    trackId: string;
    /** Native push channel; use {@link forwardFrame} rather than calling it directly. */
    readonly sink: CustomVideoSink;
}

/** Result of {@link createCustomVideoTrack}: the stream to publish + the push handle. */
export interface CustomVideoTrackResult<
    Track extends PooledTrack | ForwardTrack,
> {
    /**
     * A {@link MediaStream} containing the single custom video track. Use it as any
     * other stream — publish it (for example via `useCustomSource`), render it
     * locally, and so on. Keep it on the JS thread; it is not worklet-serializable.
     */
    stream: MediaStream;
    /** The worklet-serializable push handle. Pass to {@link pushFrame} / {@link forwardFrame}. */
    track: Track;
}

/** Frame arguments for {@link pushFrame} (pooled mode). */
export interface PushFrameArgs {
    /** {@link CustomVideoBuffer.index} of the pooled surface you rendered into. */
    bufferIndex: number;
    /** Monotonic presentation timestamp, in nanoseconds. Must increase per frame. */
    timestampNs: number;
    /** Clockwise rotation at delivery, in degrees. Defaults to `0`. */
    rotation?: 0 | 90 | 180 | 270;
    /**
     * Optional GPU completion fence: provide it when the frame's GPU work may not
     * be finished, so the encoder waits for your draw. Omit for CPU-filled or
     * already-finished frames.
     */
    fence?: CustomVideoFrameFence;
}

/** Frame arguments for {@link forwardFrame} (forwarding mode). */
export interface ForwardFrameArgs {
    /**
     * Pointer to the native buffer to forward, as a `bigint` — a retainable,
     * IOSurface-backed `CVPixelBufferRef` (iOS) or `AHardwareBuffer*` (Android). The
     * SDK retains it for the duration of encoding, so you may release/dispose your
     * own reference immediately after this call returns.
     */
    nativeBuffer: bigint;
    /**
     * Optional monotonic presentation timestamp, in nanoseconds. A raw buffer
     * pointer carries no timestamp, so when omitted the native layer stamps the
     * frame with a monotonic clock at delivery. Pass the source's real capture
     * timestamp only when you need tight A/V sync with a separate audio track.
     */
    timestampNs?: number;
    /** Clockwise rotation at delivery, in degrees. Defaults to `0`. */
    rotation?: 0 | 90 | 180 | 270;
}

/**
 * Allocate a pool of native surfaces to render into (pooled mode).
 *
 * Call once, import every surface in {@link CustomVideoBufferPool.buffers} into your
 * GPU, then attach the pool to a track with {@link createCustomVideoTrack}. You own
 * the pool: free it with {@link CustomVideoBufferPool.dispose} after the track stops.
 *
 * @param init Pool dimensions and size; see {@link CustomVideoBufferPoolInit}.
 */
export async function createCustomVideoBufferPool(
    init: CustomVideoBufferPoolInit,
): Promise<CustomVideoBufferPool> {
    validatePoolInit(init);

    let data: BridgeCustomVideoBufferPool;
    try {
        data = await WebRTCModule.createCustomVideoBufferPool(init);
    } catch (error) {
        throw new MediaStreamError(error);
    }

    // The bridge carries each surface handle as a decimal string; expose it as a
    // bigint, the public type the GPU import path consumes.
    const buffers: CustomVideoBuffer[] = data.buffers.map((buffer) => ({
        index: buffer.index,
        surfaceHandle: BigInt(buffer.surfaceHandle),
        width: buffer.width,
        height: buffer.height,
    }));

    let disposed = false;
    return {
        poolId: data.poolId,
        buffers,
        async dispose() {
            if (disposed) {
                return;
            }
            // Latch only after the native release succeeds: a rejected release
            // (e.g. the bound track is still live) must stay retryable.
            await WebRTCModule.releaseCustomVideoBufferPool(data.poolId);
            disposed = true;
        },
    };
}

/**
 * Create a custom video track.
 *
 * - Pass `{ pool }` for **pooled** mode (you render into the pool's surfaces and
 *   push by index). The pool binds to exactly one track — attaching an already-used
 *   or disposed pool rejects.
 * - Pass nothing for **forwarding** mode (you forward finished native buffers).
 *
 * Returns the {@link MediaStream} to publish plus a worklet-serializable
 * {@link PooledTrack} / {@link ForwardTrack} handle for pushing frames.
 *
 * New Architecture only: rejects with a clear error on the old architecture.
 */
export async function createCustomVideoTrack(init: {
    pool: CustomVideoBufferPool;
}): Promise<CustomVideoTrackResult<PooledTrack>>;
export async function createCustomVideoTrack(): Promise<
    CustomVideoTrackResult<ForwardTrack>
>;
export async function createCustomVideoTrack(init?: {
    pool: CustomVideoBufferPool;
}): Promise<CustomVideoTrackResult<PooledTrack | ForwardTrack>> {
    // Pooled mode is selected by passing `init`; validate the pool eagerly so a
    // malformed call fails with a clear error instead of a TypeError (or a
    // forwarding track mislabeled as pooled).
    if (init !== undefined && typeof init.pool?.poolId !== 'string') {
        throw invalidInitError(
            'createCustomVideoTrack: init.pool must be a pool returned by createCustomVideoBufferPool.',
        );
    }

    await ensureInstalled();

    let data: BridgeCustomVideoTrack;
    try {
        data = await WebRTCModule.createCustomVideoTrack({
            poolId: init?.pool.poolId,
        });
    } catch (error) {
        throw new MediaStreamError(error);
    }

    const { streamId, track } = data;
    const stream = new MediaStream({
        streamId,
        streamReactTag: streamId,
        tracks: [track],
    });

    const sink = global.__fishjamWebrtcGetCustomVideoSink!(track.id);
    const handle = init
        ? ({ kind: 'pooled', trackId: track.id, sink } as PooledTrack)
        : ({ kind: 'forward', trackId: track.id, sink } as ForwardTrack);

    return { stream, track: handle };
}

/**
 * Hand a rendered frame back to a **pooled** track for encoding (pooled mode).
 *
 * Call once per frame, after rendering into the pooled surface named by
 * {@link PushFrameArgs.bufferIndex}. Worklet-safe: it dispatches synchronously to
 * native on whatever thread you call it from (a frame-processor worklet or the JS
 * thread). Provide a {@link PushFrameArgs.fence} when the GPU work may still be in
 * flight, or omit it to deliver immediately.
 */
export function pushFrame(track: PooledTrack, frame: PushFrameArgs): void {
    'worklet';
    track.sink.push(frame);
}

/**
 * Forward a finished native buffer to a **forwarding** track (forwarding mode).
 *
 * Call once per frame with a retainable IOSurface-backed `CVPixelBufferRef` /
 * `AHardwareBuffer*` pointer (see {@link ForwardFrameArgs.nativeBuffer}). Worklet-safe:
 * it dispatches synchronously to native on whatever thread you call it from, and the
 * SDK retains the buffer before returning, so you may release/dispose your own
 * reference immediately after.
 */
export function forwardFrame(
    track: ForwardTrack,
    frame: ForwardFrameArgs,
): void {
    'worklet';
    track.sink.push(frame);
}
