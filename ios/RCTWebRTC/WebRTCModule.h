#import <AVFoundation/AVFoundation.h>
#import <Foundation/Foundation.h>

#import <React/RCTConvert.h>

#import <WebRTC/WebRTC.h>

#ifdef RCT_NEW_ARCH_ENABLED
#import <RNWebRTCSpec/RNWebRTCSpec.h>
#else
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#endif

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

#ifdef RCT_NEW_ARCH_ENABLED
@interface WebRTCModule : NativeWebRTCModuleSpecBase<NativeWebRTCModuleSpec>
#else
@interface WebRTCModule : RCTEventEmitter<RCTBridgeModule>
#endif

@property(nonatomic, strong) dispatch_queue_t workerQueue;

@property(nonatomic, strong) RTCPeerConnectionFactory *peerConnectionFactory;
@property(nonatomic, strong) id<RTCVideoDecoderFactory> decoderFactory;
@property(nonatomic, strong) id<RTCVideoEncoderFactory> encoderFactory;

@property(nonatomic, strong) NSMutableDictionary<NSNumber *, RTCPeerConnection *> *peerConnections;
@property(nonatomic, strong) NSMutableDictionary<NSString *, RTCMediaStream *> *localStreams;
@property(nonatomic, strong) NSMutableDictionary<NSString *, RTCMediaStreamTrack *> *localTracks;

@property(nonatomic, assign) BOOL destroyed;

- (RTCMediaStream *)streamForReactTag:(NSString *)reactTag;

+ (nullable instancetype)sharedInstance;

@end
