#if TARGET_OS_IOS

#import "ScreenCaptureController.h"
#import <ReplayKit/ReplayKit.h>
#import "ScreenCapturePickerViewManager.h"
#import "ScreenCapturer.h"
#import "SocketConnection.h"

NSString *const kRTCScreensharingSocketFD = @"rtc_SSFD";
NSString *const kRTCAppGroupIdentifier = @"RTCAppGroupIdentifier";

@interface ScreenCaptureController ()

@property(nonatomic, retain) ScreenCapturer *capturer;

@end

@interface ScreenCaptureController (CapturerEventsDelegate)<CapturerEventsDelegate>
- (void)capturerDidEnd:(RTCVideoCapturer *)capturer;
@end

@interface ScreenCaptureController (Private)

@property(nonatomic, readonly) NSString *appGroupIdentifier;

@end

@implementation ScreenCaptureController

- (instancetype)initWithCapturer:(nonnull ScreenCapturer *)capturer {
    self = [super init];
    if (self) {
        self.capturer = capturer;
        self.deviceId = @"screen-capture";
    }

    return self;
}

- (void)dealloc {
    [self.capturer stopCapture];
}

- (void)startCapture:(BOOL)presentBroadcastPicker {
    if (!self.appGroupIdentifier) {
        return;
    }

    if (presentBroadcastPicker) {
        [self performRPSystemBroadcastPickerViewPressed];
    }

    self.capturer.eventsDelegate = self;
    NSString *socketFilePath = [self filePathForApplicationGroupIdentifier:self.appGroupIdentifier];
    SocketConnection *connection = [[SocketConnection alloc] initWithFilePath:socketFilePath];
    [self.capturer startCaptureWithConnection:connection];
}

- (void)stopCapture {
    [self.capturer stopCapture];
}

- (NSDictionary *)getSettings {
    return @{@"deviceId" : self.deviceId, @"groupId" : @"", @"frameRate" : @(30)};
}
// MARK: CapturerEventsDelegate Methods

- (void)capturerDidEnd:(RTCVideoCapturer *)capturer {
    [self.eventsDelegate capturerDidEnd:capturer];
}

// MARK: Private Methods

- (NSString *)appGroupIdentifier {
    NSDictionary *infoDictionary = [[NSBundle mainBundle] infoDictionary];
    return infoDictionary[kRTCAppGroupIdentifier];
}

- (NSString *)filePathForApplicationGroupIdentifier:(nonnull NSString *)identifier {
    NSURL *sharedContainer =
        [[NSFileManager defaultManager] containerURLForSecurityApplicationGroupIdentifier:identifier];
    NSString *socketFilePath = [[sharedContainer URLByAppendingPathComponent:kRTCScreensharingSocketFD] path];

    return socketFilePath;
}

- (NSString *)preferredExtension {
    NSDictionary *infoDictionary = [[NSBundle mainBundle] infoDictionary];
    return infoDictionary[kRTCScreenSharingExtension];
}

- (void)performRPSystemBroadcastPickerViewPressed {
    dispatch_async(dispatch_get_main_queue(), ^{
        RPSystemBroadcastPickerView *picker = [[RPSystemBroadcastPickerView alloc] init];
        picker.showsMicrophoneButton = NO;

        NSString *extension = [self preferredExtension];

        if (extension) {
            picker.preferredExtension = extension;
        } else {
            NSLog(@"No RTCScreenSharingExtension found in Info.plist");
        }

        SEL selector = NSSelectorFromString(@"buttonPressed:");

        if ([picker respondsToSelector:selector]) {
            [picker performSelector:selector withObject:nil];
        }
    });
}

@end

#endif
