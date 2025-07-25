
#import <WebRTC/RTCPeerConnection.h>
#import <WebRTC/RTCVideoRenderer.h>
#import "WebRTCModule.h"

@interface VideoDimensionDetector : NSObject<RTCVideoRenderer>

@property(copy, nonatomic) NSNumber *peerConnectionId;
@property(copy, nonatomic) NSString *trackId;
@property(weak, nonatomic) WebRTCModule *module;

- (instancetype)initWith:(NSNumber *)peerConnectionId trackId:(NSString *)trackId webRTCModule:(WebRTCModule *)module;
- (void)dispose;

@end

@interface RTCPeerConnection (VideoTrackAdapter)

@property(nonatomic, strong) NSMutableDictionary<NSString *, id> *videoTrackAdapters;
@property(nonatomic, strong) NSMutableDictionary<NSString *, id> *videoDimensionDetectors;

- (void)addVideoTrackAdapter:(RTCVideoTrack *)track;
- (void)removeVideoTrackAdapter:(RTCVideoTrack *)track;

- (void)addVideoDimensionDetector:(RTCVideoTrack *)track;
- (void)removeVideoDimensionDetector:(RTCVideoTrack *)track;

@end
