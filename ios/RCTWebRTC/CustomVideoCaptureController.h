#import <Foundation/Foundation.h>

#import <WebRTC/RTCVideoFrame.h>
#import <WebRTC/RTCVideoSource.h>

#import "CaptureController.h"
#import "CustomVideoBufferPool.h"
#import "WebRTCModule.h"

NS_ASSUME_NONNULL_BEGIN

/**
 * Capture controller for a "custom video track": an app-owned source of frames.
 * Instead of capturing from a camera or the screen, the app supplies the frames
 * and this controller pushes them into the WebRTC pipeline. Two modes:
 *
 *   * Pooled — the app renders GPU frames into the IOSurface-backed
 *     CVPixelBuffers of a CustomVideoBufferPool and pushes them back by index,
 *     optionally guarded by a Metal shared-event fence. The controller holds a
 *     strong reference to the pool but does not own it; the pool is disposed
 *     separately via releaseCustomVideoBufferPool.
 *   * Forwarding — the frames are already produced natively (a camera,
 *     VisionCamera, a native ML pipeline) and the app forwards a finished
 *     CVPixelBufferRef; no pool and no fence are involved.
 *
 * Pooled data flow:
 *   1. createCustomVideoBufferPool({width, height, poolSize}) allocates a pool
 *      of CVPixelBuffers (kCVPixelFormatType_32BGRA, IOSurface-backed) and hands
 *      each IOSurface handle to JS, which imports each one once
 *      (react-native-webgpu importSharedTextureMemory).
 *   2. createCustomVideoTrack({poolId}) builds this controller bound to that
 *      pool together with an RTCVideoSource.
 *   3. Per frame, JS renders into buffer[index], submits, and exports a Metal
 *      shared-event fence; it then pushes the frame through the JSI channel,
 *      which resolves the fence bigints and calls pushFrameForBufferIndex:...
 *      with the shared-event handle and the value the GPU will signal once the
 *      render is complete.
 *   4. This controller arms an MTLSharedEventListener on that value; when the
 *      GPU signals, the listener block wraps buffer[index] in an RTCVideoFrame
 *      and delivers it to the RTCVideoSource.
 */
@interface CustomVideoCaptureController : CaptureController

/**
 * Builds a pooled controller bound to an app-allocated buffer pool. Holds a
 * strong reference to the pool (which stays owned by the JS dispose path).
 *
 * @param videoSource the source that frames are delivered to.
 * @param pool        the buffer pool this track renders into and pushes by index.
 */
- (instancetype)initPooledWithVideoSource:(RTCVideoSource *)videoSource pool:(CustomVideoBufferPool *)pool;

/**
 * Builds a forwarding controller with no pool. Frames arrive as finished native
 * CVPixelBufferRefs via pushExternalPixelBuffer:.
 *
 * @param videoSource the source that frames are delivered to.
 */
- (instancetype)initForwardingWithVideoSource:(RTCVideoSource *)videoSource;

/**
 * Forwards an already-produced native buffer (forwarding mode). Delivered
 * synchronously on the calling (worklet) thread: RTCCVPixelBuffer retains the
 * buffer during initWithPixelBuffer:, so the caller may release its own
 * reference right after this returns. No pool retain, no fence.
 *
 * @param pixelBuffer  the finished CVPixelBufferRef to wrap and deliver.
 * @param timestampNs  frame timestamp in nanoseconds.
 * @param rotation     frame rotation.
 */
- (void)pushExternalPixelBuffer:(CVPixelBufferRef)pixelBuffer
                    timestampNs:(int64_t)timestampNs
                       rotation:(RTCVideoRotation)rotation;

/**
 * Pushes the frame currently rendered into buffer[bufferIndex] once the GPU
 * signals the fence. Fire-and-forget: there is no completion callback because
 * this is called once per frame and must stay cheap.
 *
 * The fence is delivered as a raw 64-bit handle plus a signaled value: the JSI
 * channel has already parsed the JS bigints, so no string parsing happens here.
 *
 * @param bufferIndex        pool index that JS rendered into.
 * @param fenceHandle        the exported MTLSharedEvent handle reinterpreted as
 *                          uint64_t. A handle of 0 means no fence was supplied
 *                          and the frame is delivered immediately.
 * @param fenceSignaledValue the value the GPU will signal on the shared event.
 * @param timestampNs        frame timestamp in nanoseconds.
 * @param rotation           frame rotation.
 */
- (void)pushFrameForBufferIndex:(NSInteger)bufferIndex
                    fenceHandle:(uint64_t)fenceHandle
             fenceSignaledValue:(uint64_t)fenceSignaledValue
                    timestampNs:(int64_t)timestampNs
                       rotation:(RTCVideoRotation)rotation;

/**
 * Whether releaseCaptureResources has completed. Once true, no in-flight
 * delivery can reference the pool's CVPixelBuffers, so the pool may be disposed
 * safely (releaseCustomVideoBufferPool consults this via the pool's
 * attachedController).
 */
@property(nonatomic, readonly, getter=isTornDown) BOOL tornDown;

/**
 * Drains any in-flight deliveries and marks the controller torn down so no more
 * frames are pushed. Does NOT release the buffer pool: in pooled mode the pool is
 * owned separately and freed via releaseCustomVideoBufferPool.
 *
 * `stopCapture` is only a pause hook because it is used by
 * mediaStreamTrackSetEnabled(false). Call this from true track disposal paths
 * after no more frames should be pushed.
 */
- (void)releaseCaptureResources;

@end

/**
 * Thread-safe registry mapping a custom-video trackId to its capture controller.
 *
 * The per-frame deliver callback runs on the JS thread; resolving the controller
 * through `self.localTracks` there raced with the RN thread mutating that
 * dictionary (unsynchronised NSMutableDictionary access). This registry decouples
 * delivery from `localTracks`: it is O(1), guarded by its own lock, and holds the
 * controller weakly, so the entry clears itself when the track and its controller
 * are released — no explicit unregister on the teardown paths is required.
 */
@interface WebRTCModule (CustomVideoRegistry)

/** Registers (or replaces) the controller for trackId. */
- (void)registerCustomVideoController:(CustomVideoCaptureController *)controller forTrackId:(NSString *)trackId;

/** Looks up the live controller for trackId, or nil if none/already released. */
- (nullable CustomVideoCaptureController *)registeredCustomVideoControllerForTrackId:(NSString *)trackId;

/**
 * Looks up the buffer pool registered under poolId, or nil if none. The pool
 * registry holds strong references; entries are added by createCustomVideoBufferPool
 * and removed by releaseCustomVideoBufferPool.
 */
- (nullable CustomVideoBufferPool *)registeredCustomVideoBufferPoolForPoolId:(NSString *)poolId;

@end

NS_ASSUME_NONNULL_END
