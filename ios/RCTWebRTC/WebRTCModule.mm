#if !TARGET_OS_OSX
#import <UIKit/UIKit.h>
#endif

#import <React/RCTBridge.h>
#import <React/RCTLog.h>
#import <React/RCTUtils.h>

#import "RTCMediaStreamTrack+React.h"
#import "VideoCaptureController.h"
#import "WebRTCModule+RTCPeerConnection.h"
#import "WebRTCModule+VideoTrackAdapter.h"
#import "WebRTCModule.h"
#import "WebRTCModuleOptions.h"

@interface WebRTCModule ()
@end

@implementation WebRTCModule

static WebRTCModule *sSharedInstance = nil;

+ (instancetype)sharedInstance {
    return sSharedInstance;
}

+ (BOOL)requiresMainQueueSetup {
    return NO;
}

- (void)cleanupResources {
    _destroyed = YES;

    // Dispose all peer connections and stop VideoTrackAdapter timers
    for (NSNumber *peerConnectionId in _peerConnections.allKeys) {
        RTCPeerConnection *peerConnection = _peerConnections[peerConnectionId];
        @try {
            for (NSString *key in peerConnection.remoteTracks.allKeys) {
                RTCMediaStreamTrack *track = peerConnection.remoteTracks[key];
                if (track.kind == kRTCMediaStreamTrackKindVideo) {
                    [peerConnection removeVideoTrackAdapter:(RTCVideoTrack *)track];
                }
            }
            [peerConnection.remoteTracks removeAllObjects];
            [peerConnection.remoteStreams removeAllObjects];

            NSMutableDictionary<NSString *, DataChannelWrapper *> *dataChannels = peerConnection.dataChannels;
            for (NSString *tag in dataChannels) {
                dataChannels[tag].delegate = nil;
            }
            [dataChannels removeAllObjects];

            peerConnection.delegate = nil;
            [peerConnection close];
        } @catch (NSException *exception) {
            RCTLogWarn(@"Error disposing peer connection %@ in cleanup: %@", peerConnectionId, exception);
        }
    }
    [_peerConnections removeAllObjects];
    _peerConnections = nil;

    // Release local tracks (camera/microphone) to free hardware resources
    for (NSString *trackID in _localTracks.allKeys) {
        RTCMediaStreamTrack *track = _localTracks[trackID];
        track.isEnabled = NO;
        if (track.captureController) {
            if ([track.captureController isKindOfClass:[VideoCaptureController class]]) {
                VideoCaptureController *vcc = (VideoCaptureController *)track.captureController;
                if (vcc.capturer) {
                    vcc.capturer.delegate = nil;
                }
            }
            [track.captureController stopCapture];
        }
    }
    [_localTracks removeAllObjects];
    _localTracks = nil;

    [_localStreams removeAllObjects];
    _localStreams = nil;

    _peerConnectionFactory = nil;
    _workerQueue = nil;
}

- (void)dealloc {
    [self cleanupResources];
    sSharedInstance = nil;
}

- (void)invalidate {
    [self cleanupResources];

#ifndef RCT_NEW_ARCH_ENABLED
    [super invalidate];
#endif
}

- (instancetype)init {
    self = [super init];
    if (self) {
        sSharedInstance = self;
        WebRTCModuleOptions *options = [WebRTCModuleOptions sharedInstance];
        id<RTCAudioDevice> audioDevice = options.audioDevice;
        id<RTCVideoDecoderFactory> decoderFactory = options.videoDecoderFactory;
        id<RTCVideoEncoderFactory> encoderFactory = options.videoEncoderFactory;
        NSDictionary *fieldTrials = options.fieldTrials;
        RTCLoggingSeverity loggingSeverity = options.loggingSeverity;

        // Initialize field trials.
        if (fieldTrials == nil) {
            // Fix for dual-sim connectivity:
            // https://bugs.chromium.org/p/webrtc/issues/detail?id=10966
            fieldTrials = @{kRTCFieldTrialUseNWPathMonitor : kRTCFieldTrialEnabledValue};
        }
        RTCInitFieldTrialDictionary(fieldTrials);

        // Initialize logging.
        RTCSetMinDebugLogLevel(loggingSeverity);

        if (encoderFactory == nil) {
            encoderFactory = [[RTCDefaultVideoEncoderFactory alloc] init];
        }
        if (decoderFactory == nil) {
            decoderFactory = [[RTCDefaultVideoDecoderFactory alloc] init];
        }
        _encoderFactory = encoderFactory;
        _decoderFactory = decoderFactory;

        RCTLogInfo(@"Using video encoder factory: %@", NSStringFromClass([encoderFactory class]));
        RCTLogInfo(@"Using video decoder factory: %@", NSStringFromClass([decoderFactory class]));

        _peerConnectionFactory = [[RTCPeerConnectionFactory alloc] initWithEncoderFactory:encoderFactory
                                                                           decoderFactory:decoderFactory
                                                                              audioDevice:audioDevice];

        _peerConnections = [NSMutableDictionary new];
        _localStreams = [NSMutableDictionary new];
        _localTracks = [NSMutableDictionary new];
        _destroyed = NO;

        dispatch_queue_attr_t attributes =
            dispatch_queue_attr_make_with_qos_class(DISPATCH_QUEUE_SERIAL, QOS_CLASS_USER_INITIATED, -1);
        _workerQueue = dispatch_queue_create("WebRTCModule.queue", attributes);
    }

    return self;
}

- (RTCMediaStream *)streamForReactTag:(NSString *)reactTag {
    RTCMediaStream *stream = _localStreams[reactTag];
    if (!stream) {
        for (NSNumber *peerConnectionId in _peerConnections) {
            RTCPeerConnection *peerConnection = _peerConnections[peerConnectionId];
            stream = peerConnection.remoteStreams[reactTag];
            if (stream) {
                break;
            }
        }
    }
    return stream;
}

RCT_EXPORT_MODULE();

- (dispatch_queue_t)methodQueue {
    return _workerQueue;
}

#ifndef RCT_NEW_ARCH_ENABLED
- (NSArray<NSString *> *)supportedEvents {
    return @[
        kEventPeerConnectionSignalingStateChanged,
        kEventPeerConnectionStateChanged,
        kEventPeerConnectionOnRenegotiationNeeded,
        kEventPeerConnectionIceConnectionChanged,
        kEventPeerConnectionIceGatheringChanged,
        kEventPeerConnectionGotICECandidate,
        kEventPeerConnectionDidOpenDataChannel,
        kEventDataChannelDidChangeBufferedAmount,
        kEventDataChannelStateChanged,
        kEventDataChannelReceiveMessage,
        kEventMediaStreamTrackMuteChanged,
        kEventMediaStreamTrackEnded,
        kEventPeerConnectionOnRemoveTrack,
        kEventPeerConnectionOnTrack
    ];
}
#endif

#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params {
    return std::make_shared<facebook::react::NativeWebRTCModuleSpecJSI>(params);
}
#endif

@end
