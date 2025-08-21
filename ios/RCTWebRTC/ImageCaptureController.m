#if !TARGET_OS_TV

#import "ImageCaptureController.h"
#import "ImageCapturer.h"

@interface ImageCaptureController ()

@property(nonatomic, retain) ImageCapturer *capturer;

@end

@interface ImageCaptureController (CapturerEventsDelegate)<CapturerEventsDelegate>
- (void)capturerDidEnd:(RTCVideoCapturer *)capturer;
@end

@implementation ImageCaptureController

- (instancetype)initWithCapturer:(nonnull ImageCapturer *)capturer {
    self = [super init];
    if (self) {
        self.capturer = capturer;
        self.deviceId = @"image-capture";
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