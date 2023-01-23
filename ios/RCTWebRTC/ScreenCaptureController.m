//
//  ScreenCaptureController.m
//  RCTWebRTC
//
//  Created by Alex-Dan Bumbu on 06/01/2021.
//

#import "ScreenCaptureController.h"
#import "ScreenCapturer.h"
#import "SocketConnection.h"

NSString* const kRTCScreensharingSocketFD = @"rtc_SSFD";
NSString* const kRTCAppGroupIdentifier = @"RTCAppGroupIdentifier";

@interface ScreenCaptureController ()

@property (nonatomic, retain) ScreenCapturer *capturer;
@property (nonatomic, assign) BOOL userStopped;

@end

@interface ScreenCaptureController (CapturerEventsDelegate) <CapturerEventsDelegate>
- (void)capturerDidStart:(RTCVideoCapturer *) capturer;
- (void)capturerDidStop:(RTCVideoCapturer *) capturer;
@end

@interface ScreenCaptureController (Private)

@property (nonatomic, readonly) NSString *appGroupIdentifier;

@end

@implementation ScreenCaptureController

@synthesize userStopped = _userStopped;

- (instancetype)initWithCapturer:(nonnull ScreenCapturer *)capturer {
    self = [super init];
    if (self) {
        self.capturer = capturer;
    }
    
    return self;
}

- (void)dealloc {
    [self.capturer stopCapture];
}

- (void)startCapture {
    self.userStopped = NO;
    if (!self.appGroupIdentifier) {
        return;
    }
    
    self.capturer.eventsDelegate = self;
    NSString *socketFilePath = [self filePathForApplicationGroupIdentifier:self.appGroupIdentifier];
    SocketConnection *connection = [[SocketConnection alloc] initWithFilePath:socketFilePath];
    [self.capturer startCaptureWithConnection:connection];
}

- (void)stopCapture {
    self.userStopped = YES;
    [self.capturer stopCapture];
}

// MARK: CapturerEventsDelegate Methods

- (void)capturerDidStart:(RTCVideoCapturer *) capturer {
    [self.eventsDelegate capturerDidStart:capturer];
}

- (void)capturerDidStop:(RTCVideoCapturer *) capturer {
    [self.eventsDelegate capturerDidStop:capturer];
}

// MARK: Private Methods

- (NSString *)appGroupIdentifier {
    NSDictionary *infoDictionary = [[NSBundle mainBundle] infoDictionary];
    return infoDictionary[kRTCAppGroupIdentifier];
}

- (NSString *)filePathForApplicationGroupIdentifier:(nonnull NSString *)identifier {
    NSURL *sharedContainer = [[NSFileManager defaultManager] containerURLForSecurityApplicationGroupIdentifier:identifier];
    NSString *socketFilePath = [[sharedContainer URLByAppendingPathComponent:kRTCScreensharingSocketFD] path];
    
    return socketFilePath;
}

@end
