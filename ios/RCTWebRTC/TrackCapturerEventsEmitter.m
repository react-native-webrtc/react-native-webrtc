#import "WebRTCModule.h"
#import "TrackCapturerEventsEmitter.h"
#import "CapturerEventsDelegate.h"

NS_ASSUME_NONNULL_BEGIN

@interface TrackCapturerEventsEmitter ()

@property (copy, nonatomic) NSString *trackId;
@property (weak, nonatomic) WebRTCModule *module;
@property (weak, nonatomic) CaptureController *captureController;

@end

@implementation TrackCapturerEventsEmitter

- (instancetype)initWith:(NSString *)trackId
            webRTCModule:(WebRTCModule *)module
       captureController:(CaptureController *)captureController{
    self = [super init];
    if (self) {
        self.trackId = trackId;
        self.module = module;
        self.captureController = captureController;
    }

    return self;
}

- (void)capturerDidStart:(RTCVideoCapturer *)capturer {
    // Do nothing.
}

- (void)capturerDidStop:(RTCVideoCapturer *)capturer {
    if (!self.captureController.userStopped) {
        [self.module sendEventWithName:kEventMediaStreamTrackEnded
                                  body:@{
                                    @"trackId": self.trackId,
                                  }];

        RCTLog(@"[TrackCapturerEventsEmitter] ended event for track %@", self.trackId);
    }
}

@end

NS_ASSUME_NONNULL_END
