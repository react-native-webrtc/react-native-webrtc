#import "TrackCapturerEventsEmitter.h"
#import "CapturerEventsDelegate.h"
#import "WebRTCModule.h"

NS_ASSUME_NONNULL_BEGIN

@interface TrackCapturerEventsEmitter ()

@property(copy, nonatomic) NSString *trackId;
@property(weak, nonatomic) WebRTCModule *module;

@end

@implementation TrackCapturerEventsEmitter

- (instancetype)initWith:(NSString *)trackId webRTCModule:(WebRTCModule *)module {
    self = [super init];
    if (self) {
        self.trackId = trackId;
        self.module = module;
    }

    return self;
}

- (void)capturerDidEnd:(RTCVideoCapturer *)capturer {
    [self.module sendEventWithName:kEventMediaStreamTrackEnded
                              body:@{
                                  @"trackId" : self.trackId,
                              }];

    RCTLog(@"[TrackCapturerEventsEmitter] ended event for track %@", self.trackId);
}

@end

NS_ASSUME_NONNULL_END