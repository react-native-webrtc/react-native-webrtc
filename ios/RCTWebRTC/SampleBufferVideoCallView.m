#import "SampleBufferVideoCallView.h"
#import <Accelerate/Accelerate.h>
#import <WebRTC/WebRTC.h>
#import "I420Converter.h"

@protocol SampleBufferRendering<AVQueuedSampleBufferRendering>

- (BOOL)requiresFlushToResumeDecoding;

@end

/** These classes already implement the required methods. */
@interface AVSampleBufferDisplayLayer ()<SampleBufferRendering>
@end
@interface AVSampleBufferVideoRenderer ()<SampleBufferRendering>
@end

@interface SampleBufferVideoCallView ()

@property(nonatomic, retain) I420Converter *i420Converter;
@property(nonatomic, strong) id<SampleBufferRendering> renderer;
@property(nonatomic, assign) NSInteger currentRotation;

@end

@implementation SampleBufferVideoCallView

+ (Class)layerClass {
    return [AVSampleBufferDisplayLayer class];
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(layerFailedToDecode:)
                                                     name:AVSampleBufferDisplayLayerFailedToDecodeNotification
                                                   object:self.sampleBufferLayer];
        if (@available(iOS 17.0, *)) {
            _renderer = self.sampleBufferLayer.sampleBufferRenderer;
        } else {
            _renderer = self.sampleBufferLayer;
        }
    }
    return self;
}

- (void)layerFailedToDecode:(NSNotification *)note {
    NSError *error = [[note userInfo] valueForKey:AVSampleBufferDisplayLayerFailedToDecodeNotificationErrorKey];
    NSLog(@"layerFailedToDecode, error: %@", [error localizedDescription]);
}

- (AVSampleBufferDisplayLayer *)sampleBufferLayer {
    return (AVSampleBufferDisplayLayer *)self.layer;
}

- (void)requestScaleRecalculation {
    _currentRotation = -1;
}

- (void)recalculateScale:(RTCVideoRotation)rotation {
    if (self.currentRotation != rotation) {
        self.currentRotation = rotation;

        CGFloat scale = 1;
        if (rotation == 90 || rotation == 270) {
            CGSize size = self.bounds.size;
            scale = size.height / size.width;
        }

        self.sampleBufferLayer.transform = CATransform3DConcat(
            CATransform3DMakeRotation(rotation / 180.0 * M_PI, 0.0, 0.0, 1.0), CATransform3DMakeScale(scale, scale, 1));
    }
}

/** The size of the video frame. */
- (void)setSize:(CGSize)size {
}

/** The frame to be displayed. */
- (void)renderFrame:(nullable RTC_OBJC_TYPE(RTCVideoFrame) *)frame {
    if (!_shouldRender) {
        return;
    }

    // Convert RTCVideoFrame to CMSampleBuffer
    CMSampleBufferRef sampleBuffer = [self sampleBufferFrom:frame];
    if (sampleBuffer == nil) {
        return;
    }

    __weak typeof(self) weakSelf = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        __strong typeof(weakSelf) strongSelf = weakSelf;
        if (!strongSelf) {
            CFRelease(sampleBuffer);
            return;
        }
        if (strongSelf.renderer.requiresFlushToResumeDecoding) {
            [strongSelf.renderer flush];
        }

        if (!strongSelf.renderer.readyForMoreMediaData) {
            CFRelease(sampleBuffer);
            return;
        }

        [strongSelf recalculateScale:frame.rotation];

        // Display the CMSampleBuffer using AVSampleBufferDisplayLayer
        [strongSelf.renderer enqueueSampleBuffer:sampleBuffer];
        CFRelease(sampleBuffer);
    });
}

- (CMSampleBufferRef)sampleBufferFrom:(RTCVideoFrame *)rtcVideoFrame {
    // Convert RTCVideoFrame to CMSampleBuffer

    // Assuming your RTCVideoFrame contains pixelBuffer
    CVPixelBufferRef pixelBuffer = [self pixelBufferFrom:rtcVideoFrame];
    if (!pixelBuffer) {
        return nil;
    }

    // Create a CMVideoFormatDescription
    CMVideoFormatDescriptionRef formatDescription;
    OSStatus status = CMVideoFormatDescriptionCreateForImageBuffer(kCFAllocatorDefault, pixelBuffer, &formatDescription);
    if (status != noErr || formatDescription == NULL) {
        CVPixelBufferRelease(pixelBuffer);
        return nil;
    }

    // Create CMSampleTimingInfo
    // Timescale is 90khz according to RTCVideoFrame.h
    CMSampleTimingInfo timingInfo;
    timingInfo.presentationTimeStamp = CMTimeMake(rtcVideoFrame.timeStamp, 90000);
    timingInfo.decodeTimeStamp = CMTimeMake(rtcVideoFrame.timeStamp, 90000);

    // Create CMSampleBuffer
    CMSampleBufferRef sampleBuffer;
    CMSampleBufferCreateForImageBuffer(
        kCFAllocatorDefault, pixelBuffer, true, nil, nil, formatDescription, &timingInfo, &sampleBuffer);

    CFArrayRef attachments = CMSampleBufferGetSampleAttachmentsArray(sampleBuffer, YES);
    CFMutableDictionaryRef dict = (CFMutableDictionaryRef)CFArrayGetValueAtIndex(attachments, 0);

    CFDictionarySetValue(dict, kCMSampleAttachmentKey_DisplayImmediately, kCFBooleanTrue);
    CVPixelBufferRelease(pixelBuffer);
    CFRelease(formatDescription);
    return sampleBuffer;
}

/**
 * The CVPixelBufferRef returned from this function must be released when finished using it.
 */
- (CVPixelBufferRef)pixelBufferFrom:(RTCVideoFrame *)videoFrame {
    if ([videoFrame.buffer isKindOfClass:[RTCCVPixelBuffer class]]) {
        CVPixelBufferRef pixelBuffer = [((RTCCVPixelBuffer *)videoFrame.buffer) pixelBuffer];
        CVPixelBufferRetain(pixelBuffer);
        return pixelBuffer;
    } else {
        return [self pixelBufferFromI420:[videoFrame.buffer toI420]];
    }
}

/**
 * The CVPixelBufferRef returned from this function must be released when finished using it.
 */
- (CVPixelBufferRef)pixelBufferFromI420:(RTCI420Buffer *)i420Buffer {
    if (_i420Converter == nil) {
        I420Converter *converter = [[I420Converter alloc] init];
        vImage_Error err = [converter prepareForAccelerateConversion];

        if (err != kvImageNoError) {
            NSLog(@"Error when preparing i420Converter: %ld", err);
            return NULL;
        }

        _i420Converter = converter;
    }

    CVPixelBufferRef convertedPixelBuffer = [_i420Converter convertI420ToPixelBuffer:i420Buffer];

    return convertedPixelBuffer;
}

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self
                                                    name:AVSampleBufferDisplayLayerFailedToDecodeNotification
                                                  object:self.sampleBufferLayer];
    [_i420Converter unprepareForAccelerateConversion];
}

@end
