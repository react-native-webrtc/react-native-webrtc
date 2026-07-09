#if !TARGET_OS_TV && !TARGET_OS_OSX

#import <Metal/Metal.h>

#import <WebRTC/RTCCVPixelBuffer.h>
#import <WebRTC/RTCVideoFrameBuffer.h>

#import "CustomVideoCaptureController.h"

static NSTimeInterval const kCustomVideoDrainTimeoutSeconds = 2.0;

@implementation CustomVideoCaptureController {
    RTCVideoSource *_videoSource;

    // The buffer pool for pooled mode; nil in forwarding mode. Held strongly, but
    // owned by the JS dispose path (releaseCustomVideoBufferPool), not by us.
    CustomVideoBufferPool *_pool;

    // Single listener + dedicated serial queue for all fence callbacks. Created
    // only in pooled mode; nil in forwarding mode (no fence to wait on).
    MTLSharedEventListener *_sharedEventListener API_AVAILABLE(ios(13.0));
    dispatch_queue_t _fenceCallbackQueue;

    // Lifecycle / drain bookkeeping. A single NSCondition, _drainCondition, is the
    // one mutual-exclusion lock guarding _accepting, _inFlightCount, _generation
    // and _tornDown. stopCapture pauses by bumping _generation; completion blocks
    // from an older generation then drop their frames instead of delivering stale
    // buffers after a pause/resume or final release.
    NSCondition *_drainCondition;
    BOOL _accepting;
    NSInteger _inFlightCount;
    NSInteger _generation;
    // Set only by releaseCaptureResources. Any notify/no-fence completion that runs
    // after this is set must not deliver, because the CVPixelBuffers it would ship
    // may have been released (the pool is disposed independently).
    BOOL _tornDown;
}

- (instancetype)initPooledWithVideoSource:(RTCVideoSource *)videoSource pool:(CustomVideoBufferPool *)pool {
    self = [super init];
    if (!self) {
        return nil;
    }

    _videoSource = videoSource;
    _pool = pool;

    [self setUpDrainState];

    // Pooled frames may be guarded by a Metal shared-event fence, so arm a
    // listener + dedicated serial queue for all fence callbacks.
    _fenceCallbackQueue = dispatch_queue_create("com.fishjam.webrtc.customVideoTrack.fence", DISPATCH_QUEUE_SERIAL);
    if (@available(iOS 13.0, *)) {
        _sharedEventListener = [[MTLSharedEventListener alloc] initWithDispatchQueue:_fenceCallbackQueue];
    }

    return self;
}

- (instancetype)initForwardingWithVideoSource:(RTCVideoSource *)videoSource {
    self = [super init];
    if (!self) {
        return nil;
    }

    _videoSource = videoSource;
    _pool = nil;

    [self setUpDrainState];

    // Forwarding delivers synchronously on the calling thread with no fence, so no
    // listener/queue is created.

    return self;
}

// Common accept/teardown state shared by both modes.
- (void)setUpDrainState {
    _drainCondition = [[NSCondition alloc] init];
    _accepting = NO;
    _inFlightCount = 0;
    _generation = 0;
    _tornDown = NO;
}

#pragma mark - CaptureController overrides

- (void)startCapture {
    [_drainCondition lock];
    if (!_tornDown) {
        _generation++;
        _accepting = YES;
    }
    [_drainCondition unlock];
}

- (void)stopCapture {
    [_drainCondition lock];
    if (_tornDown) {
        [_drainCondition unlock];
        return;
    }

    // Pause only: mediaStreamTrackSetEnabled(false) calls stopCapture(), and a
    // later startCapture() must resume using the same IOSurface pool.
    _accepting = NO;
    _generation++;

    // Bound the drain so a never-signaled MTLSharedEvent cannot block the RN
    // method queue forever. Late completions from older generations drop.
    NSDate *deadline = [NSDate dateWithTimeIntervalSinceNow:kCustomVideoDrainTimeoutSeconds];
    while (_inFlightCount > 0) {
        if (![_drainCondition waitUntilDate:deadline]) {
            break;
        }
    }
    [_drainCondition unlock];
}

- (NSDictionary *)getSettings {
    return @{
        @"deviceId" : self.deviceId ?: @"custom-video",
        @"groupId" : @"",
        @"width" : @(_pool.width),
        @"height" : @(_pool.height),
    };
}

#pragma mark - Frame push

- (void)pushFrameForBufferIndex:(NSInteger)bufferIndex
                    fenceHandle:(uint64_t)fenceHandle
             fenceSignaledValue:(uint64_t)fenceSignaledValue
                    timestampNs:(int64_t)timestampNs
                       rotation:(RTCVideoRotation)rotation {
    CVPixelBufferRef buffer = NULL;
    NSInteger frameGeneration = 0;

    [_drainCondition lock];
    if (bufferIndex < 0 || bufferIndex >= _pool.count) {
        [_drainCondition unlock];
        NSLog(@"[CustomVideoCaptureController] pushFrame: bufferIndex %ld out of range", (long)bufferIndex);
        return;
    }
    if (!_accepting || _tornDown) {
        [_drainCondition unlock];
        return;
    }
    frameGeneration = _generation;
    buffer = [_pool pixelBufferAtIndex:bufferIndex];
    _inFlightCount++;
    [_drainCondition unlock];

    // Decide whether we have a usable fence to wait on. A handle of 0 means JS
    // supplied no fence (the JSI core passes 0/0 in that case); pre-iOS-13 has no
    // MTLSharedEvent. In those cases we deliver without waiting on the GPU.
    BOOL hasFence = (fenceHandle != 0);
    if (@available(iOS 13.0, *)) {
        // MTLSharedEvent available.
    } else {
        hasFence = NO;
    }

    if (!hasFence) {
        // No-fence delivery must NOT run on the calling (JS) thread: hop onto the
        // fence callback queue so JS is never blocked by source-side work, and so
        // this path shares the serialisation and torn-down guard of the fenced one.
        // The in-flight slot was reserved above; it is released inside.
        __weak __typeof__(self) weakSelf = self;
        dispatch_async(_fenceCallbackQueue, ^{
            __typeof__(self) strongSelf = weakSelf;
            if (strongSelf == nil) {
                return;
            }
            [strongSelf completeInFlightDeliveringBuffer:buffer
                                              generation:frameGeneration
                                             timestampNs:timestampNs
                                                rotation:rotation];
        });
        return;
    }

    if (@available(iOS 13.0, *)) {
        // The fence handle is the exported id<MTLSharedEvent> object pointer
        // reinterpreted as uint64_t (confirmed in react-native-webgpu
        // GPUSharedFence.cpp — reinterpret_cast<uint64_t> of the id<MTLSharedEvent>
        // held by SharedFenceMTLSharedEventExportInfo). The cast is __bridge (no
        // ARC ownership transfer); on its own that reference is non-owning, so if
        // JS drops the originating fence before the GPU signals, the event could
        // deallocate and we'd miss the callback / message a freed object.
        id<MTLSharedEvent> event = (__bridge id<MTLSharedEvent>)(void *)(uintptr_t)fenceHandle;
        if (event == nil) {
            // Reserved a slot but have nothing to wait on; complete off-thread.
            __weak __typeof__(self) weakSelf = self;
            dispatch_async(_fenceCallbackQueue, ^{
                __typeof__(self) strongSelf = weakSelf;
                if (strongSelf == nil) {
                    return;
                }
                [strongSelf completeInFlightDeliveringBuffer:buffer
                                                  generation:frameGeneration
                                                 timestampNs:timestampNs
                                                    rotation:rotation];
            });
            return;
        }

        // Take an owning reference for the armed listener's lifetime so the event
        // cannot deallocate out from under the notify block. Balanced by exactly
        // one CFRelease inside the block, on every path out of it.
        CFRetain((__bridge CFTypeRef)event);

        __weak __typeof__(self) weakSelf = self;
        [event notifyListener:_sharedEventListener
                      atValue:fenceSignaledValue
                        block:^(id<MTLSharedEvent> _Nonnull notifyingEvent, uint64_t signaledValue) {
                            __typeof__(self) strongSelf = weakSelf;
                            if (strongSelf != nil) {
                                [strongSelf completeInFlightDeliveringBuffer:buffer
                                                                  generation:frameGeneration
                                                                 timestampNs:timestampNs
                                                                    rotation:rotation];
                            }
                            CFRelease((__bridge CFTypeRef)event);
                        }];
    }
}

- (void)pushExternalPixelBuffer:(CVPixelBufferRef)pixelBuffer
                    timestampNs:(int64_t)timestampNs
                       rotation:(RTCVideoRotation)rotation {
    if (pixelBuffer == NULL) {
        return;
    }

    // Reserve an in-flight slot under the drain lock, then deliver OUTSIDE the lock
    // (never hold it across the WebRTC delivery call). Counting forwarding
    // deliveries in-flight — like the pooled path, and like Android — means
    // stopCapture/releaseCaptureResources actually drain a synchronous forward that
    // is mid-flight on the worklet thread instead of tearing down underneath it.
    // Forwarding has no pool retain and no fence: the buffer is app-owned and
    // delivered synchronously on the calling (worklet) thread. RTCCVPixelBuffer
    // retains the buffer during initWithPixelBuffer:, so the caller may release its
    // own reference immediately after this returns.
    [_drainCondition lock];
    if (!_accepting || _tornDown) {
        [_drainCondition unlock];
        return;
    }
    _inFlightCount++;
    [_drainCondition unlock];

    [self deliverBuffer:pixelBuffer timestampNs:timestampNs rotation:rotation];

    [_drainCondition lock];
    if (_inFlightCount > 0) {
        _inFlightCount--;
    }
    if (_inFlightCount <= 0) {
        [_drainCondition broadcast];
    }
    [_drainCondition unlock];
}

- (void)deliverBuffer:(CVPixelBufferRef)buffer timestampNs:(int64_t)timestampNs rotation:(RTCVideoRotation)rotation {
    if (buffer == NULL) {
        return;
    }
    // Ship the BGRA RTCCVPixelBuffer directly (no I420 conversion): I420 is only
    // needed for the local Metal preview of screen sharing, not for sending.
    RTCCVPixelBuffer *rtcPixelBuffer = [[RTCCVPixelBuffer alloc] initWithPixelBuffer:buffer];
    RTCVideoFrame *frame = [[RTCVideoFrame alloc] initWithBuffer:rtcPixelBuffer
                                                        rotation:rotation
                                                     timeStampNs:timestampNs];
    // The RTCVideoSource is an RTCVideoCapturerDelegate; deliver straight to it.
    // No RTCVideoCapturer subclass needed.
    [_videoSource capturer:nil didCaptureVideoFrame:frame];
}

// Single completion path for an in-flight frame, shared by the fenced and
// no-fence routes. We decide-and-retain under _drainCondition, then deliver
// OUTSIDE the lock — never holding it across the WebRTC delivery call (which can
// apply back-pressure and would otherwise stall pushFrame on the JS thread).
// A completion delivers only if its generation is still current and capture is
// still accepting. Pause/resume/release all bump the generation, so stale
// completions only decrement the in-flight count. Retaining the CVPixelBuffer
// under the lock keeps it alive through delivery.
- (void)completeInFlightDeliveringBuffer:(CVPixelBufferRef)buffer
                              generation:(NSInteger)frameGeneration
                             timestampNs:(int64_t)timestampNs
                                rotation:(RTCVideoRotation)rotation {
    CVPixelBufferRef bufferToDeliver = NULL;
    [_drainCondition lock];
    if (!_tornDown && _accepting && _generation == frameGeneration && buffer != NULL) {
        bufferToDeliver = buffer;
        CVPixelBufferRetain(bufferToDeliver);
    }
    [_drainCondition unlock];

    if (bufferToDeliver != NULL) {
        [self deliverBuffer:bufferToDeliver timestampNs:timestampNs rotation:rotation];
        CVPixelBufferRelease(bufferToDeliver);
    }

    [_drainCondition lock];
    if (_inFlightCount > 0) {
        _inFlightCount--;
    }
    if (_inFlightCount <= 0) {
        [_drainCondition broadcast];
    }
    [_drainCondition unlock];
}

#pragma mark - Teardown

- (BOOL)isTornDown {
    [_drainCondition lock];
    BOOL tornDown = _tornDown;
    [_drainCondition unlock];
    return tornDown;
}

- (void)releaseCaptureResources {
    [_drainCondition lock];
    if (_tornDown) {
        [_drainCondition unlock];
        return;
    }

    _accepting = NO;
    _generation++;

    NSDate *deadline = [NSDate dateWithTimeIntervalSinceNow:kCustomVideoDrainTimeoutSeconds];
    while (_inFlightCount > 0) {
        if (![_drainCondition waitUntilDate:deadline]) {
            break;
        }
    }

    _tornDown = YES;
    [_drainCondition unlock];

    // The buffer pool is owned separately (CustomVideoBufferPool) and released via
    // releaseCustomVideoBufferPool; nothing to free here. A completion starting
    // after _tornDown is set observes it and skips delivery; an armed notify block
    // still fires and balances its CFRetain.
}

- (void)dealloc {
    [self releaseCaptureResources];
}

@end

#endif
