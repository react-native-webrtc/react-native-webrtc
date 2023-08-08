#if !TARGET_OS_TV

#import <Foundation/Foundation.h>

    #import <WebRTC/RTCCameraVideoCapturer.h>
#endif

#import "CaptureController.h"

@interface VideoCaptureController : CaptureController
#if !TARGET_OS_TV
    @property(nonatomic, readonly, strong) AVCaptureDeviceFormat *selectedFormat;
#endif
@property(nonatomic, readonly, assign) int frameRate;

#if !TARGET_OS_TV
    - (instancetype)initWithCapturer:(RTCCameraVideoCapturer *)capturer andConstraints:(NSDictionary *)constraints;
    - (void)startCapture;
    - (void)stopCapture;
    - (void)switchCamera;

@end
#endif
