#if !TARGET_OS_TV

#import "CaptureController.h"

@implementation CaptureController

- (void)startCapture {
    // subclasses needs to override
}

- (void)stopCapture {
    // subclasses needs to override
}

@end

#endif