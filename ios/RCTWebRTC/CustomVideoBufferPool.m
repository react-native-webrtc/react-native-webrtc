#if !TARGET_OS_TV && !TARGET_OS_OSX

#import "CustomVideoBufferPool.h"

@implementation CustomVideoBufferPool {
    // Pool and its pre-allocated, index-stable buffers. The buffers are retained
    // for the whole lifetime of the pool because JS imports each IOSurface exactly
    // once and addresses them by index forever after.
    CVPixelBufferPoolRef _pixelBufferPool;
    NSArray *_pixelBuffers;  // boxed CVPixelBufferRef (NSValue pointerValue)
}

- (nullable instancetype)initWithWidth:(NSInteger)width
                                height:(NSInteger)height
                              poolSize:(NSInteger)poolSize
                                 error:(NSError **)outError {
    self = [super init];
    if (!self) {
        return nil;
    }

    if (width <= 0 || height <= 0 || poolSize <= 0) {
        if (outError) {
            *outError = [NSError
                errorWithDomain:@"react-native-webrtc"
                           code:0
                       userInfo:@{NSLocalizedDescriptionKey : @"width, height and poolSize must all be positive"}];
        }
        return nil;
    }

    _width = width;
    _height = height;
    _attached = NO;

    if (![self buildPoolWithSize:poolSize error:outError]) {
        return nil;
    }

    return self;
}

#pragma mark - Pool construction

- (BOOL)buildPoolWithSize:(NSInteger)poolSize error:(NSError **)outError {
    NSDictionary *pixelBufferAttributes = @{
        (id)kCVPixelBufferPixelFormatTypeKey : @(kCVPixelFormatType_32BGRA),
        (id)kCVPixelBufferWidthKey : @(_width),
        (id)kCVPixelBufferHeightKey : @(_height),
        // IOSurface-backed so the buffer can be imported into the GPU
        // (react-native-webgpu importSharedTextureMemory) and Metal-compatible
        // so the GPU can render into it.
        (id)kCVPixelBufferIOSurfacePropertiesKey : @{},
        (id)kCVPixelBufferMetalCompatibilityKey : @YES,
    };

    // Cap the pool so CVPixelBufferPoolCreatePixelBuffer never recycles a buffer
    // while WebRTC still holds it: we allocate exactly poolSize buffers up front
    // and keep them all, so the pool's min-buffer-count == our pool size.
    NSDictionary *poolAttributes = @{
        (id)kCVPixelBufferPoolMinimumBufferCountKey : @(poolSize),
    };

    CVReturn poolStatus = CVPixelBufferPoolCreate(kCFAllocatorDefault,
                                                  (__bridge CFDictionaryRef)poolAttributes,
                                                  (__bridge CFDictionaryRef)pixelBufferAttributes,
                                                  &_pixelBufferPool);
    if (poolStatus != kCVReturnSuccess || _pixelBufferPool == NULL) {
        if (outError) {
            *outError = [NSError errorWithDomain:@"react-native-webrtc"
                                            code:poolStatus
                                        userInfo:@{
                                            NSLocalizedDescriptionKey : [NSString
                                                stringWithFormat:@"CVPixelBufferPoolCreate failed: %d", poolStatus]
                                        }];
        }
        return NO;
    }

    NSMutableArray *buffers = [NSMutableArray arrayWithCapacity:poolSize];
    for (NSInteger index = 0; index < poolSize; index++) {
        CVPixelBufferRef buffer = NULL;
        CVReturn bufferStatus = CVPixelBufferPoolCreatePixelBuffer(kCFAllocatorDefault, _pixelBufferPool, &buffer);
        if (bufferStatus != kCVReturnSuccess || buffer == NULL) {
            if (outError) {
                *outError = [NSError
                    errorWithDomain:@"react-native-webrtc"
                               code:bufferStatus
                           userInfo:@{
                               NSLocalizedDescriptionKey : [NSString
                                   stringWithFormat:@"CVPixelBufferPoolCreatePixelBuffer failed at index %ld: %d",
                                                    (long)index,
                                                    bufferStatus]
                           }];
            }
            // Release whatever we already allocated before bailing out.
            for (NSValue *boxed in buffers) {
                CVPixelBufferRelease((CVPixelBufferRef)boxed.pointerValue);
            }
            return NO;
        }
        // The NSArray keeps the +1 from CVPixelBufferPoolCreatePixelBuffer; we
        // balance it in dispose / dealloc.
        [buffers addObject:[NSValue valueWithPointer:buffer]];
    }

    _pixelBuffers = [buffers copy];

    // Build the JS-facing descriptors once, now that the surfaces are stable.
    NSMutableArray<NSDictionary *> *descriptors = [NSMutableArray arrayWithCapacity:poolSize];
    for (NSInteger index = 0; index < poolSize; index++) {
        CVPixelBufferRef buffer = (CVPixelBufferRef)[_pixelBuffers[index] pointerValue];
        IOSurfaceRef surface = CVPixelBufferGetIOSurface(buffer);
        if (surface == NULL) {
            // Should be impossible with kCVPixelBufferIOSurfacePropertiesKey set,
            // but fail here with a clear error rather than handing JS a "0" handle
            // that only blows up later, deep inside the GPU import.
            if (outError) {
                *outError =
                    [NSError errorWithDomain:@"react-native-webrtc"
                                        code:0
                                    userInfo:@{
                                        NSLocalizedDescriptionKey : [NSString
                                            stringWithFormat:@"Pixel buffer at index %ld has no IOSurface", (long)index]
                                    }];
            }
            return NO;
        }
        // Emit the handle exactly the way react-native-webgpu interprets it on
        // import: the raw (uintptr_t)IOSurfaceRef, as a decimal string (a 64-bit
        // pointer would lose precision through a JS double).
        uintptr_t handle = (uintptr_t)surface;
        [descriptors addObject:@{
            @"index" : @(index),
            @"surfaceHandle" : [NSString stringWithFormat:@"%lu", (unsigned long)handle],
            @"width" : @(_width),
            @"height" : @(_height),
        }];
    }
    _bufferDescriptors = [descriptors copy];

    return YES;
}

#pragma mark - Accessors

- (NSInteger)count {
    return (NSInteger)_pixelBuffers.count;
}

- (CVPixelBufferRef)pixelBufferAtIndex:(NSInteger)index {
    if (index < 0 || index >= (NSInteger)_pixelBuffers.count) {
        return NULL;
    }
    return (CVPixelBufferRef)[_pixelBuffers[index] pointerValue];
}

#pragma mark - Teardown

- (void)dispose {
    _disposed = YES;
    if (_pixelBuffers) {
        for (NSValue *boxed in _pixelBuffers) {
            CVPixelBufferRef buffer = (CVPixelBufferRef)boxed.pointerValue;
            if (buffer) {
                CVPixelBufferRelease(buffer);
            }
        }
        _pixelBuffers = nil;
    }
    if (_pixelBufferPool != NULL) {
        CVPixelBufferPoolRelease(_pixelBufferPool);
        _pixelBufferPool = NULL;
    }
    _bufferDescriptors = nil;
}

- (void)dealloc {
    [self dispose];
}

@end

#endif
