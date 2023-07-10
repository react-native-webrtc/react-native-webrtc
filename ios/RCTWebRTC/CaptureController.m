#if TARGET_OS_IOS 

#import "CaptureController.h"

@implementation CaptureController

- (void)startCapture {
    // subclasses needs to override
}

- (void)stopCapture {
    // subclasses needs to override
}

@end

#endif  // !TARGET_OS_OSX and !TARGET_OS_TVOS