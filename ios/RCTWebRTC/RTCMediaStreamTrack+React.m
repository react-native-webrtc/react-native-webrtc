#import <objc/runtime.h>

#import "RTCMediaStreamTrack+React.h"

@implementation RTCMediaStreamTrack (React)

- (id<VideoCaptureControllerProtocol>)videoCaptureController {
    return objc_getAssociatedObject(self, @selector(videoCaptureController));
}

- (void)setVideoCaptureController:(id<VideoCaptureControllerProtocol>)videoCaptureController {
    objc_setAssociatedObject(self, @selector(videoCaptureController), videoCaptureController, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

@end
