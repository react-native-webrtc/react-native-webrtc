#import <AVFoundation/AVFoundation.h>
#import <Foundation/Foundation.h>

#import <React/RCTBridgeModule.h>
#import <React/RCTConvert.h>
#import <React/RCTEventEmitter.h>

#import <WebRTC/WebRTC.h>

static NSString *const kEventPeerConnectionSignalingStateChanged = @"peerConnectionSignalingStateChanged";
static NSString *const kEventPeerConnectionStateChanged = @"peerConnectionStateChanged";
static NSString *const kEventPeerConnectionOnRenegotiationNeeded = @"peerConnectionOnRenegotiationNeeded";
static NSString *const kEventPeerConnectionIceConnectionChanged = @"peerConnectionIceConnectionChanged";
static NSString *const kEventPeerConnectionIceGatheringChanged = @"peerConnectionIceGatheringChanged";
static NSString *const kEventPeerConnectionGotICECandidate = @"peerConnectionGotICECandidate";
static NSString *const kEventPeerConnectionDidOpenDataChannel = @"peerConnectionDidOpenDataChannel";
static NSString *const kEventDataChannelDidChangeBufferedAmount = @"dataChannelDidChangeBufferedAmount";
static NSString *const kEventDataChannelStateChanged = @"dataChannelStateChanged";
static NSString *const kEventDataChannelReceiveMessage = @"dataChannelReceiveMessage";
static NSString *const kEventMediaStreamTrackMuteChanged = @"mediaStreamTrackMuteChanged";
static NSString *const kEventMediaStreamTrackEnded = @"mediaStreamTrackEnded";
static NSString *const kEventPeerConnectionOnRemoveTrack = @"peerConnectionOnRemoveTrack";
static NSString *const kEventPeerConnectionOnTrack = @"peerConnectionOnTrack";

@interface WebRTCModule : RCTEventEmitter<RCTBridgeModule>

@property(nonatomic, strong) dispatch_queue_t workerQueue;

@property(nonatomic, strong) RTCPeerConnectionFactory *peerConnectionFactory;
@property(nonatomic, strong) id<RTCVideoDecoderFactory> decoderFactory;
@property(nonatomic, strong) id<RTCVideoEncoderFactory> encoderFactory;

@property(nonatomic, strong) NSMutableDictionary<NSNumber *, RTCPeerConnection *> *peerConnections;
@property(nonatomic, strong) NSMutableDictionary<NSString *, RTCMediaStream *> *localStreams;
@property(nonatomic, strong) NSMutableDictionary<NSString *, RTCMediaStreamTrack *> *localTracks;

- (RTCMediaStream *)streamForReactTag:(NSString *)reactTag;

@end
