#import <WebRTC/RTCPeerConnection.h>
#import "DataChannelObserver.h"
#import "WebRTCModule.h"

@interface RTCPeerConnection (React)

@property(nonatomic, strong) NSNumber *reactTag;
@property(nonatomic, strong) NSMutableDictionary<NSString *, DataChannelObserver *> *dataChannels;
@property(nonatomic, strong) NSMutableDictionary<NSString *, RTCMediaStream *> *remoteStreams;
@property(nonatomic, strong) NSMutableDictionary<NSString *, RTCMediaStreamTrack *> *remoteTracks;
@property(nonatomic, weak) id webRTCModule;

@end

@interface WebRTCModule (RTCPeerConnection)<RTCPeerConnectionDelegate>

@end
