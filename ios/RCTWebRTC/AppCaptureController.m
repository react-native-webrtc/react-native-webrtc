
#if TARGET_OS_IOS

#import "AppCaptureController.h"
#import "AppCapturer.h"

@interface AppCaptureController ()

@property(nonatomic, retain) AppCapturer *capturer;

@end

@implementation AppCaptureController

- (instancetype)initWithCapturer:(nonnull AppCapturer *)capturer {
    self = [super init];
    if (self) {
        self.capturer = capturer;
        self.deviceId = @"app-capture";
    }

    return self;
}

- (void)dealloc {
    [self.capturer stopCapture];
}

- (void)startCaptureWithCompletionHandler:(void (^)(NSError *_Nullable error))completionHandler {
    [self.capturer startCaptureWithCompletionHandler:completionHandler];
}

- (void)stopCapture {
    [self.capturer stopCapture];
}

@end

#endif
