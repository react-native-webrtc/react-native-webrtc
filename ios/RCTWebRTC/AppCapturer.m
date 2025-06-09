#import "AppCapturer.h"
#import <ReplayKit/RPScreenRecorder.h>

@implementation AppCapturer {
    RPScreenRecorder* screenRecorder;
    RTCVideoSource* source;
}

- (instancetype)initWithDelegate:(__weak id<RTCVideoCapturerDelegate>)delegate {
  source = delegate;
  return [super initWithDelegate:delegate];
}

- (void)startCaptureWithCompletionHandler:(void (^)(NSError * _Nullable error))completionHandler {
    if (screenRecorder == NULL) {
        screenRecorder = [RPScreenRecorder sharedRecorder];
    }
        
    [screenRecorder setMicrophoneEnabled:NO];
   
    if (![screenRecorder isAvailable]){
        NSLog(@"ScreenRecorder.startCapture screen recording is not isAvailable");
        NSError* error = [NSError errorWithDomain:@"react-native-webrtc"
                                        code:0
                                    userInfo:@{ NSLocalizedDescriptionKey: @"Screen Recorder unavailable."}];
        completionHandler(error);
        return;
    }
    
    [screenRecorder startCaptureWithHandler:^(CMSampleBufferRef _Nonnull sampleBuffer,
                                              RPSampleBufferType bufferType,
                                              NSError * _Nullable error) {

        if (bufferType == RPSampleBufferTypeVideo){
            [self handleSourceBuffer:sampleBuffer sampleType:bufferType];
        }
    } completionHandler:^(NSError * _Nullable error) {
        if (error){
            NSLog(@"ScreenRecorder.startCapture/completionHandler %@", error);
            completionHandler(error);
        } else {
            completionHandler(nil);
        }
    }];
}

- (void)handleSourceBuffer:(CMSampleBufferRef)sampleBuffer sampleType:(RPSampleBufferType)sampleType {
    if (CMSampleBufferGetNumSamples(sampleBuffer) != 1 || !CMSampleBufferIsValid(sampleBuffer) ||
      !CMSampleBufferDataIsReady(sampleBuffer)) {
      return;
    }

    CVPixelBufferRef pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
    if (pixelBuffer == nil) {
      return;
    }

    size_t width = CVPixelBufferGetWidth(pixelBuffer);
    size_t height = CVPixelBufferGetHeight(pixelBuffer);

    [source adaptOutputFormatToWidth:(int)(width / 2) height:(int)(height / 2) fps:8];

    RTCCVPixelBuffer* rtcPixelBuffer = [[RTCCVPixelBuffer alloc] initWithPixelBuffer:pixelBuffer];
    
    int64_t timeStampNs =
      CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(sampleBuffer)) * NSEC_PER_SEC;
    
    RTCVideoFrame* videoFrame = [[RTCVideoFrame alloc] initWithBuffer:rtcPixelBuffer
                                                             rotation:RTCVideoRotation_0
                                                          timeStampNs:timeStampNs];
    
    [self.delegate capturer:self didCaptureVideoFrame:videoFrame];
}

- (void)stopCapture {
    if (![screenRecorder isRecording]){
        NSLog(@"ScreenRecorder.stopCapturer called when not recording.");
        return;
    }
    [screenRecorder stopCaptureWithHandler:^(NSError* _Nullable error) {
        if (error != nil) {
            NSLog(@"ScreenRecorder.stopCapture %@", error);
        }
    }];
}

@end

