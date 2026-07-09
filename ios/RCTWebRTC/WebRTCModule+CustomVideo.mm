#import <Foundation/Foundation.h>
#import <mach/mach_time.h>
#import <objc/runtime.h>

#import <WebRTC/RTCVideoTrack.h>

#if __has_include(<React/RCTCallInvokerModule.h>)
#import <React/RCTCallInvoker.h>
#define FJ_HAS_CALL_INVOKER 1
#endif

#import "CustomVideoBufferPool.h"
#import "CustomVideoCaptureController.h"
#import "FJVideoPushJSI.h"
#import "RTCMediaStreamTrack+React.h"
#import "WebRTCModule.h"

#include <memory>
#include <string>

#if !TARGET_OS_TV && !TARGET_OS_OSX
// Monotonic wall-clock in nanoseconds, used to stamp forwarded frames whose raw
// buffer pointer carries no presentation time (JS passed timestampNs == 0).
static int64_t FJMonotonicTimeNs(void) {
    static mach_timebase_info_data_t timebase;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        mach_timebase_info(&timebase);
    });
    uint64_t machTime = mach_absolute_time();
    return (int64_t)(machTime * timebase.numer / timebase.denom);
}
#endif

#pragma mark - FJVideoPushBox

// ObjC holder for the C++ shared_ptr<FJVideoPush>, stored on the module as an
// associated object.
@interface FJVideoPushBox : NSObject {
   @public
    std::shared_ptr<FJVideoPush> push;
}
@end

@implementation FJVideoPushBox
@end

#pragma mark - WebRTCModule (CustomVideo)

@implementation WebRTCModule (CustomVideo)

// Lazily builds the push channel from the JS CallInvoker. Returns nil when there
// is no CallInvoker (i.e. the old architecture), which makes per-frame push
// unsupported.
- (FJVideoPushBox *)fj_videoPushBox {
#if FJ_HAS_CALL_INVOKER
    static const void *key = &key;
    FJVideoPushBox *box = objc_getAssociatedObject(self, key);
    if (box != nil) {
        return box;
    }
    RCTCallInvoker *invoker = self.callInvoker;
    if (invoker == nil) {
        return nil;  // old architecture: no CallInvoker, push unsupported
    }
    std::shared_ptr<facebook::react::CallInvoker> jsInvoker = [invoker callInvoker];
    if (!jsInvoker) {
        return nil;
    }
    box = [FJVideoPushBox new];
    box->push = std::make_shared<FJVideoPush>(jsInvoker);
    objc_setAssociatedObject(self, key, box, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    return box;
#else
    return nil;
#endif
}

#pragma mark - CustomVideoRegistry

// Lazily-created trackId -> CustomVideoCaptureController registry, stored as an
// associated object on the module. Strong keys, weak values: when the track and
// its controller are released, the entry zeroes itself, so no explicit removal is
// needed on the stop/dispose paths. NSMapTable is not thread-safe, so every access
// is serialised; the registry object itself is the lock token (its pointer is
// stable once created under @synchronized(self)).
- (NSMapTable<NSString *, CustomVideoCaptureController *> *)customVideoControllerRegistry {
    static const void *key = &key;
    @synchronized(self) {
        NSMapTable *registry = objc_getAssociatedObject(self, key);
        if (registry == nil) {
            registry = [NSMapTable strongToWeakObjectsMapTable];
            objc_setAssociatedObject(self, key, registry, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
        }
        return registry;
    }
}

- (void)registerCustomVideoController:(CustomVideoCaptureController *)controller
                           forTrackId:(NSString *)trackId {
    if (controller == nil || trackId == nil) {
        return;
    }
    NSMapTable *registry = [self customVideoControllerRegistry];
    @synchronized(registry) {
        [registry setObject:controller forKey:trackId];
    }
}

- (CustomVideoCaptureController *)registeredCustomVideoControllerForTrackId:(NSString *)trackId {
    if (trackId == nil) {
        return nil;
    }
    NSMapTable *registry = [self customVideoControllerRegistry];
    @synchronized(registry) {
        return [registry objectForKey:trackId];
    }
}

// Looks up the custom-video capture controller registered for trackId. Backed by
// the lock-guarded registry above so the per-frame deliver callback never touches
// the unsynchronised localTracks dictionary from the JS thread.
- (CustomVideoCaptureController *)customVideoCaptureControllerForTrackId:(NSString *)trackId {
    return [self registeredCustomVideoControllerForTrackId:trackId];
}

#pragma mark - CustomVideoBufferPool registry

// Lazily-created poolId -> CustomVideoBufferPool registry, stored as an associated
// object on the module. Strong values (the pool must outlive the JS handle until
// disposed), lock-guarded on the registry object. Pools are owned by JS: an entry
// is added by createCustomVideoBufferPool and removed by releaseCustomVideoBufferPool.
- (NSMutableDictionary<NSString *, CustomVideoBufferPool *> *)customVideoBufferPoolRegistry {
    static const void *key = &key;
    @synchronized(self) {
        NSMutableDictionary *registry = objc_getAssociatedObject(self, key);
        if (registry == nil) {
            registry = [NSMutableDictionary dictionary];
            objc_setAssociatedObject(self, key, registry, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
        }
        return registry;
    }
}

- (CustomVideoBufferPool *)registeredCustomVideoBufferPoolForPoolId:(NSString *)poolId {
    if (poolId == nil) {
        return nil;
    }
    NSMutableDictionary *registry = [self customVideoBufferPoolRegistry];
    @synchronized(registry) {
        return registry[poolId];
    }
}

RCT_EXPORT_METHOD(createCustomVideoBufferPool
                  : (NSDictionary *)init resolver
                  : (RCTPromiseResolveBlock)resolve rejecter
                  : (RCTPromiseRejectBlock)reject) {
#if TARGET_OS_TV || TARGET_OS_OSX
    reject(@"E_UNSUPPORTED_PLATFORM", @"Custom video tracks are only supported on iOS and Android.", nil);
    return;
#else
    NSInteger width = [init[@"width"] integerValue];
    NSInteger height = [init[@"height"] integerValue];
    NSInteger poolSize = [init[@"poolSize"] integerValue];
    if (width <= 0 || height <= 0 || poolSize <= 0) {
        reject(@"E_INVALID_CUSTOM_VIDEO_BUFFER_POOL_INIT",
               @"Custom video buffer pool width, height and poolSize must be positive integers.",
               nil);
        return;
    }

    NSError *error = nil;
    CustomVideoBufferPool *pool = [[CustomVideoBufferPool alloc] initWithWidth:width
                                                                       height:height
                                                                     poolSize:poolSize
                                                                        error:&error];
    if (pool == nil) {
        reject(@"E_CUSTOM_VIDEO_BUFFER_POOL_FAILED",
               error.localizedDescription ?: @"Failed to create custom video buffer pool",
               error);
        return;
    }

    NSString *poolId = [[NSUUID UUID] UUIDString];
    NSMutableDictionary *registry = [self customVideoBufferPoolRegistry];
    @synchronized(registry) {
        registry[poolId] = pool;
    }

    resolve(@{
        @"poolId" : poolId,
        @"buffers" : pool.bufferDescriptors,
    });
#endif
}

RCT_EXPORT_METHOD(releaseCustomVideoBufferPool
                  : (NSString *)poolId resolver
                  : (RCTPromiseResolveBlock)resolve rejecter
                  : (RCTPromiseRejectBlock)reject) {
    if (poolId == nil) {
        resolve(nil);
        return;
    }
    NSMutableDictionary *registry = [self customVideoBufferPoolRegistry];
    CustomVideoBufferPool *pool = nil;
    @synchronized(registry) {
        pool = registry[poolId];
    }
    if (pool == nil) {
        // Never registered / already released.
        resolve(nil);
        return;
    }
    // Refuse to free buffers a live track may still be delivering: in-flight
    // pushes hold CVPixelBufferRefs from this pool, so disposing here would be a
    // use-after-free. The controller reference is weak and tornDown flips after
    // the final drain, so this clears once the track is properly stopped.
    CustomVideoCaptureController *attachedController = pool.attachedController;
    if (attachedController != nil && !attachedController.isTornDown) {
        reject(@"E_CUSTOM_VIDEO_POOL_IN_USE",
               @"Cannot release a custom video buffer pool while its track is live. Stop the track first.",
               nil);
        return;
    }
    @synchronized(registry) {
        [registry removeObjectForKey:poolId];
    }
    // Dispose outside the lock.
    [pool dispose];
    resolve(nil);
}

RCT_REMAP_METHOD(installCustomVideoJSI,
                 installCustomVideoJSIWithResolver : (RCTPromiseResolveBlock)resolve
                 rejecter : (RCTPromiseRejectBlock)reject) {
#if TARGET_OS_TV || TARGET_OS_OSX
    reject(@"E_UNSUPPORTED_PLATFORM", @"Custom video tracks are only supported on iOS and Android.", nil);
    return;
#else
    FJVideoPushBox *box = [self fj_videoPushBox];
    if (box == nil) {
        reject(@"E_NO_JSI", @"Custom video frame push requires the New Architecture.", nil);
        return;
    }

    // The deliver callback runs on the JS thread (synchronously inside the JS
    // push function). Look up the controller for the frame's trackId and forward
    // the resolved uint64 fence. __weak self avoids a retain cycle through the
    // associated-object box (self -> box -> push -> deliver_ -> self).
    __weak WebRTCModule *weakSelf = self;
    box->push->setDeliver([weakSelf](const std::string &trackId, int bufferIndex, uint64_t nativeBuffer,
                                     uint64_t timestampNs, int rotation, uint64_t fenceHandle,
                                     uint64_t fenceSignaledValue) {
        WebRTCModule *strongSelf = weakSelf;
        if (strongSelf == nil) {
            return;
        }
        NSString *trackIdString = [NSString stringWithUTF8String:trackId.c_str()];
        CustomVideoCaptureController *captureController =
            [strongSelf customVideoCaptureControllerForTrackId:trackIdString];
        if (captureController == nil) {
            return;
        }
        if (nativeBuffer != 0) {
            // Forwarding: wrap the app-provided CVPixelBufferRef and deliver it
            // synchronously. A raw buffer pointer carries no presentation time, so
            // stamp a monotonic timestamp when JS did not supply one.
            int64_t stampNs = (int64_t)timestampNs;
            if (stampNs == 0) {
                stampNs = FJMonotonicTimeNs();
            }
            [captureController pushExternalPixelBuffer:(CVPixelBufferRef)(uintptr_t)nativeBuffer
                                           timestampNs:stampNs
                                              rotation:(RTCVideoRotation)rotation];
        } else {
            // Pooled: deliver bufferIndex with the resolved fence, as before.
            [captureController pushFrameForBufferIndex:bufferIndex
                                           fenceHandle:fenceHandle
                                    fenceSignaledValue:fenceSignaledValue
                                           timestampNs:(int64_t)timestampNs
                                              rotation:(RTCVideoRotation)rotation];
        }
    });

    box->push->install([resolve]() { resolve(@YES); });
#endif
}

@end
