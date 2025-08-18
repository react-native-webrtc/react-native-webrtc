#if !TARGET_OS_TV

#import "FileCaptureController.h"
#import "FileCapturer.h"

@interface FileCaptureController ()

@property(nonatomic, retain) FileCapturer *capturer;

@end

@interface FileCaptureController (CapturerEventsDelegate)<CapturerEventsDelegate>
- (void)capturerDidEnd:(RTCVideoCapturer *)capturer;
@end

@implementation FileCaptureController

- (instancetype)initWithCapturer:(nonnull FileCapturer *)capturer {
    self = [super init];
    if (self) {
        self.capturer = capturer;
        self.deviceId = @"file-capture";
    }

    return self;
}

- (void)dealloc {
    [self.capturer stopCapture];
}

- (void)startCapture {
    self.capturer.eventsDelegate = self;
    [self.capturer startCapture];
}

- (void)stopCapture {
    [self.capturer stopCapture];
}

- (NSDictionary *)getSettings {
    return @{@"deviceId" : self.deviceId, @"groupId" : @"", @"frameRate" : @(1)};
}

@end

#endif