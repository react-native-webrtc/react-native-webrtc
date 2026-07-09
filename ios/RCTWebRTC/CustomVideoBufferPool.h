#import <CoreVideo/CoreVideo.h>
#import <Foundation/Foundation.h>

@class CustomVideoCaptureController;

NS_ASSUME_NONNULL_BEGIN

/**
 * A fixed-size pool of IOSurface-backed CVPixelBuffers for a "custom video track"
 * in *pooled* mode: the app renders GPU frames directly into these buffers and
 * pushes them back by index.
 *
 * The pool is owned independently of any capture controller (it maps 1:1 to a
 * track, but its lifetime is managed by the JS `CustomVideoBufferPool.dispose`
 * path via `releaseCustomVideoBufferPool`, not by track teardown). It holds:
 *   - the buffer dimensions,
 *   - the CVPixelBufferPoolRef,
 *   - the index-stable buffers (JS imports each IOSurface exactly once and
 *     addresses them by index forever after),
 *   - the JS-facing `bufferDescriptors`.
 */
@interface CustomVideoBufferPool : NSObject

/**
 * Allocates the pool and all its buffers (kCVPixelFormatType_32BGRA,
 * IOSurface-backed, Metal-compatible) so the surface handles are available
 * immediately for the JS resolve.
 *
 * @param width    pixel width of every buffer in the pool.
 * @param height   pixel height of every buffer in the pool.
 * @param poolSize number of buffers to pre-allocate and keep at stable indices.
 * @param outError populated when allocation fails (returns nil).
 */
- (nullable instancetype)initWithWidth:(NSInteger)width
                                height:(NSInteger)height
                              poolSize:(NSInteger)poolSize
                                 error:(NSError **)outError;

/**
 * Stable index -> IOSurface handle map, exposed to JS so each surface can be
 * imported once and reused. Each entry is
 *   @{@"index": NSNumber, @"surfaceHandle": NSString (decimal uintptr_t),
 *     @"width": NSNumber, @"height": NSNumber}.
 * The handle is emitted as a decimal string to avoid losing precision through a
 * JS double (a 64-bit pointer does not fit exactly in a double).
 */
@property(nonatomic, readonly) NSArray<NSDictionary *> *bufferDescriptors;

/** Number of buffers in the pool. */
@property(nonatomic, readonly) NSInteger count;

/** Pixel width of every buffer. */
@property(nonatomic, readonly) NSInteger width;

/** Pixel height of every buffer. */
@property(nonatomic, readonly) NSInteger height;

/**
 * Whether this pool is already bound to a track. Enforces the 1<->1 pool/track
 * relationship: createCustomVideoTrack sets it and rejects a second attach.
 */
@property(nonatomic, assign) BOOL attached;

/**
 * The capture controller of the track this pool is bound to; nil until attached
 * (and again once the controller deallocates). `releaseCustomVideoBufferPool`
 * consults it (together with its `tornDown` state) to refuse disposal while the
 * track is still live — in-flight frame deliveries may hold this pool's
 * CVPixelBuffers.
 */
@property(nonatomic, weak, nullable) CustomVideoCaptureController *attachedController;

/** True once `dispose` has run; the buffers are gone. */
@property(nonatomic, readonly) BOOL disposed;

/** Returns the buffer at index, or NULL when the index is out of range. */
- (CVPixelBufferRef)pixelBufferAtIndex:(NSInteger)index;

/**
 * Permanently releases all buffers and the CVPixelBufferPoolRef. Idempotent: a
 * second call is a no-op.
 */
- (void)dispose;

@end

NS_ASSUME_NONNULL_END
