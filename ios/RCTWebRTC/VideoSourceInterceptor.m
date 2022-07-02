//
//  VideoSourceInterceptor.m
//  react-native-webrtc
//
//  Created by YAVUZ SELIM CAKIR on 18.06.2022.
//

#import "VideoSourceInterceptor.h"
#import <WebRTC/RTCCVPixelBuffer.h>
#import <WebRTC/RTCVideoFrame.h>
#import <WebRTC/RTCNativeI420Buffer.h>
#import <WebRTC/RTCVideoFrameBuffer.h>
#import <SelfieSegmentatorGpu/SelfieSegmentator.h>


@interface VideoSourceInterceptor () <SelfieSegmentatorDelegate>

@property (nonatomic, strong) SelfieSegmentator *selfieSegmentator;
@property (nonatomic) RTCVideoRotation rotation;
@property (nonatomic) int64_t timeStampNs;
@property (nonatomic) RTCVideoCapturer *capturer;
@end

@implementation VideoSourceInterceptor

- (instancetype)initWithVideoSource: (RTCVideoSource*) videoSource {
    if (self = [super init]) {
        _videoSource = videoSource;
        
        // Initialize Selfie Segmentator
        self.selfieSegmentator = [[SelfieSegmentator alloc] init];
        [self.selfieSegmentator startGraph];
        self.selfieSegmentator.delegate = self;
    }
    return self;
}

- (void)capturer:(nonnull RTCVideoCapturer *)capturer didCaptureVideoFrame:(nonnull RTCVideoFrame *)frame {
    
    // convert RTCCVPixelBuffer to CVPixelBufferRef
    RTCCVPixelBuffer* pixelBufferr = (RTCCVPixelBuffer *)frame.buffer;
    CVPixelBufferRef pixelBufferRef = pixelBufferr.pixelBuffer;

    // Keep the rotation and timeStamp values
    self.rotation = frame.rotation;
    self.timeStampNs = frame.timeStampNs;
    
    self.capturer = capturer;
    
    // Call for selfie segmentation
    [self.selfieSegmentator processVideoFrame:pixelBufferRef];
}

- (void)selfieSegmentator: (SelfieSegmentator*)selfieSegmentator didOutputPixelBuffer: (CVPixelBufferRef)pixelBuffer {
    
    // Convert CVPixelBufferRef to RTCVideoFrame
    RTCCVPixelBuffer *rtcPixelBuffer = [[RTCCVPixelBuffer alloc] initWithPixelBuffer:pixelBuffer];
    RTCI420Buffer *i420buffer = [rtcPixelBuffer toI420];
    RTCVideoFrame *frame = [[RTCVideoFrame alloc] initWithBuffer:i420buffer
                                                      rotation:self.rotation
                                                   timeStampNs:self.timeStampNs];
    
    // Call RTCVideoSource object created by PeerConnectionFactory
    [_videoSource capturer:self.capturer didCaptureVideoFrame:frame];
}
@end
