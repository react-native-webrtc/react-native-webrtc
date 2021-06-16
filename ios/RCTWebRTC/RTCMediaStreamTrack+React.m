#import <objc/runtime.h>

#import "RTCMediaStreamTrack+React.h"
#import "CaptureController.h"

@implementation RTCMediaStreamTrack (React)

- (CaptureController *)captureController {
    return objc_getAssociatedObject(self, @selector(captureController));
}

- (void)setCaptureController:(CaptureController *)captureController {
    objc_setAssociatedObject(self, @selector(captureController), captureController, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

@end
