//
//  WebRTCModule.m
//
//  Created by one on 2015/9/24.
//  Copyright © 2015 One. All rights reserved.
//

#import <UIKit/UIKit.h>

#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTUtils.h>

#import <WebRTC/RTCDefaultVideoDecoderFactory.h>
#import <WebRTC/RTCDefaultVideoEncoderFactory.h>

#import "WebRTCModule.h"
#import "WebRTCModule+RTCPeerConnection.h"

@interface WebRTCModule ()
@end

@implementation WebRTCModule

+ (BOOL)requiresMainQueueSetup
{
    return NO;
}

- (void)dealloc
{
  [_localTracks removeAllObjects];
  _localTracks = nil;
  [_localStreams removeAllObjects];
  _localStreams = nil;

  for (NSNumber *peerConnectionId in _peerConnections) {
    RTCPeerConnection *peerConnection = _peerConnections[peerConnectionId];
    peerConnection.delegate = nil;
    [peerConnection close];
  }
  [_peerConnections removeAllObjects];

  _peerConnectionFactory = nil;
}

- (instancetype)init
{
    return [self initWithEncoderFactory:nil decoderFactory:nil];
}

- (instancetype)initWithEncoderFactory:(nullable id<RTCVideoEncoderFactory>)encoderFactory
                        decoderFactory:(nullable id<RTCVideoDecoderFactory>)decoderFactory
{
  self = [super init];
  if (self) {
    if (encoderFactory == nil) {
      encoderFactory = [[RTCDefaultVideoEncoderFactory alloc] init];
    }
    if (decoderFactory == nil) {
      decoderFactory = [[RTCDefaultVideoDecoderFactory alloc] init];
    }
    _peerConnectionFactory
      = [[RTCPeerConnectionFactory alloc] initWithEncoderFactory:encoderFactory
                                                  decoderFactory:decoderFactory];

    _peerConnections = [NSMutableDictionary new];
    _localStreams = [NSMutableDictionary new];
    _localTracks = [NSMutableDictionary new];

    dispatch_queue_attr_t attributes =
    dispatch_queue_attr_make_with_qos_class(DISPATCH_QUEUE_SERIAL,
                                            QOS_CLASS_USER_INITIATED, -1);
    _workerQueue = dispatch_queue_create("WebRTCModule.queue", attributes);
  }
  return self;
}

- (RTCMediaStream*)streamForReactTag:(NSString*)reactTag
{
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

- (dispatch_queue_t)methodQueue
{
  return _workerQueue;
}

- (NSArray<NSString *> *)supportedEvents {
  return @[
    kEventPeerConnectionSignalingStateChanged,
    kEventPeerConnectionStateChanged,
    kEventPeerConnectionAddedStream,
    kEventPeerConnectionRemovedStream,
    kEventPeerConnectionOnRenegotiationNeeded,
    kEventPeerConnectionIceConnectionChanged,
    kEventPeerConnectionIceGatheringChanged,
    kEventPeerConnectionGotICECandidate,
    kEventPeerConnectionDidOpenDataChannel,
    kEventDataChannelStateChanged,
    kEventDataChannelReceiveMessage,
    kEventMediaStreamTrackMuteChanged
  ];
}

@end
