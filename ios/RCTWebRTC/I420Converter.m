//
//  I420Converter.m
//  VideoSampleCaptureRender
//
//  Adapted from:
//  https://github.com/twilio/video-ios-affectiva/blob/ed2e864324c40ad25e5a06cc2b05298b03caed09/EmoCall/I420Converter.m
//  Created by Boisy Pitre on 5/21/16.
//  Copyright © 2016 Twilio. All rights reserved.
//

#import "I420Converter.h"

@interface I420Converter ()

@property(nonatomic, assign) vImage_YpCbCrToARGB *conversionInfo;
@property(nonatomic, assign) CVPixelBufferPoolRef pixelBufferPool;
@property(nonatomic, assign) size_t poolWidth;
@property(nonatomic, assign) size_t poolHeight;
@end

@implementation I420Converter

- (vImage_Error)prepareForAccelerateConversion {
    // Setup the YpCbCr to ARGB conversion.
    if (_conversionInfo != NULL) {
        return kvImageNoError;
    }

    // I420 uses limited range.
    vImage_YpCbCrPixelRange pixelRange = {16, 128, 235, 240, 255, 0, 255, 0};
    vImage_YpCbCrToARGB *outInfo = malloc(sizeof(vImage_YpCbCrToARGB));
    vImageYpCbCrType inType = kvImage420Yp8_Cb8_Cr8;
    vImageARGBType outType = kvImageARGB8888;

    vImage_Error error = vImageConvert_YpCbCrToARGB_GenerateConversion(kvImage_YpCbCrToARGBMatrix_ITU_R_601_4,
                                                                       &pixelRange,
                                                                       outInfo,
                                                                       inType,
                                                                       outType,
                                                                       kvImagePrintDiagnosticsToConsole);

    _conversionInfo = outInfo;

    return error;
}

- (void)unprepareForAccelerateConversion {
    if (_conversionInfo != NULL) {
        free(_conversionInfo);
        _conversionInfo = NULL;
    }
    if (_pixelBufferPool != NULL) {
        CVPixelBufferPoolRelease(_pixelBufferPool);
        _pixelBufferPool = NULL;
    }
}

- (void)createPixelBufferPoolWithWidth:(size_t)width height:(size_t)height {
    if (_pixelBufferPool != NULL) {
        CVPixelBufferPoolRelease(_pixelBufferPool);
    }

    _poolWidth = width;
    _poolHeight = height;
    NSDictionary *pixelBufferAttributes = @{
        (id)kCVPixelBufferPixelFormatTypeKey : @(kCVPixelFormatType_32BGRA),
        (id)kCVPixelBufferWidthKey : @(width),
        (id)kCVPixelBufferHeightKey : @(height),
        (id)kCVPixelBufferIOSurfacePropertiesKey : @{}
    };

    CVReturn ret = CVPixelBufferPoolCreate(
        kCFAllocatorDefault, NULL, (__bridge CFDictionaryRef)pixelBufferAttributes, &_pixelBufferPool);

    if (ret != kCVReturnSuccess) {
        NSLog(@"Error creating pixel buffer pool: %d", ret);
        _pixelBufferPool = NULL;
    }
}

- (CVPixelBufferRef)convertI420ToPixelBuffer:(RTCI420Buffer *)buffer {
    if (_conversionInfo == nil) {
        NSLog(@"%@: not prepared", NSStringFromSelector(_cmd));
        return NULL;
    }

    if (_pixelBufferPool == NULL || _poolWidth != buffer.width || _poolHeight != buffer.height) {
        [self createPixelBufferPoolWithWidth:buffer.width height:buffer.height];
    }

    CVPixelBufferRef pixelBuffer = NULL;
    CVReturn status = CVPixelBufferPoolCreatePixelBuffer(kCFAllocatorDefault, _pixelBufferPool, &pixelBuffer);

    if (status != kCVReturnSuccess) {
        return NULL;
    }

    vImage_Error err = [self convertFrameVImageYUV:buffer toBuffer:pixelBuffer];

    if (err != kvImageNoError) {
        NSLog(@"%@: error during conversion: %ld", NSStringFromSelector(_cmd), err);
        CVPixelBufferRelease(pixelBuffer);
        return NULL;
    }

    return pixelBuffer;
}

- (vImage_Error)convertFrameVImageYUV:(RTCI420Buffer *)buffer toBuffer:(CVPixelBufferRef)pixelBufferRef {
    if (pixelBufferRef == NULL) {
        return kvImageInvalidParameter;
    }

    // Compute info for I420 source.
    vImagePixelCount width = buffer.width;
    vImagePixelCount height = buffer.height;
    vImagePixelCount subsampledWidth = buffer.chromaWidth;
    vImagePixelCount subsampledHeight = buffer.chromaHeight;

    const uint8_t *yPlane = buffer.dataY;
    const uint8_t *uPlane = buffer.dataU;
    const uint8_t *vPlane = buffer.dataV;
    size_t yStride = (size_t)buffer.strideY;
    size_t uStride = (size_t)buffer.strideU;
    size_t vStride = (size_t)buffer.strideV;

    // Create vImage buffers to represent each of the Y, U, and V planes
    vImage_Buffer yPlaneBuffer = {.data = (void *)yPlane, .height = height, .width = width, .rowBytes = yStride};
    vImage_Buffer uPlaneBuffer = {
        .data = (void *)uPlane, .height = subsampledHeight, .width = subsampledWidth, .rowBytes = uStride};
    vImage_Buffer vPlaneBuffer = {
        .data = (void *)vPlane, .height = subsampledHeight, .width = subsampledWidth, .rowBytes = vStride};

    // Create a vImage buffer for the destination pixel buffer.
    CVPixelBufferLockBaseAddress(pixelBufferRef, 0);

    void *pixelBufferData = CVPixelBufferGetBaseAddress(pixelBufferRef);
    size_t rowBytes = CVPixelBufferGetBytesPerRow(pixelBufferRef);
    vImage_Buffer destinationImageBuffer = {
        .data = pixelBufferData, .height = height, .width = width, .rowBytes = rowBytes};

    // Do the conversion.

    uint8_t permuteMap[4] = {3, 2, 1, 0};  // BGRA
    vImage_Error convertError = vImageConvert_420Yp8_Cb8_Cr8ToARGB8888(&yPlaneBuffer,
                                                                       &uPlaneBuffer,
                                                                       &vPlaneBuffer,
                                                                       &destinationImageBuffer,
                                                                       self.conversionInfo,
                                                                       permuteMap,
                                                                       255,
                                                                       kvImageNoFlags);

    CVPixelBufferUnlockBaseAddress(pixelBufferRef, 0);

    return convertError;
}

- (void)dealloc {
    [self unprepareForAccelerateConversion];
    CVPixelBufferPoolRelease(_pixelBufferPool);
}

@end
