#import <objc/runtime.h>

#import "RTCMediaStreamTrack+React.h"

@implementation RTCMediaStreamTrack (React)

- (VideoCaptureController *)videoCaptureController {
    return objc_getAssociatedObject(self, @selector(videoCaptureController));
}

- (void)setVideoCaptureController:(VideoCaptureController *)videoCaptureController {
    objc_setAssociatedObject(self, @selector(videoCaptureController), videoCaptureController, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

@end
