#import "TrackCapturerEventsEmitter.h"

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
    if (self.module.destroyed) {
        return;
    }
    NSDictionary *body = @{@"trackId" : self.trackId};
#ifdef RCT_NEW_ARCH_ENABLED
    [self.module emitMediaStreamTrackEnded:body];
#else
    [self.module sendEventWithName:kEventMediaStreamTrackEnded body:body];
#endif

    RCTLog(@"[TrackCapturerEventsEmitter] ended event for track %@", self.trackId);
}

@end

NS_ASSUME_NONNULL_END