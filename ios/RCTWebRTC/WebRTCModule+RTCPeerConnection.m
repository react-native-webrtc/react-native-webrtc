//
//  WebRTCModule+RTCPeerConnection.m
//
//  Created by one on 2015/9/24.
//  Copyright © 2015 One. All rights reserved.
//

#import <objc/runtime.h>

#import "RCTLog.h"
#import "RCTUtils.h"
#import "RCTBridge.h"
#import "RCTEventDispatcher.h"

#import "RTCICEServer.h"
#import "RTCPair.h"
#import "RTCMediaConstraints.h"
#import "RTCPeerConnection+Block.h"
#import "RTCICECandidate.h"
#import "RTCStatsReport.h"

#import "WebRTCModule+RTCMediaStream.h"
#import "WebRTCModule+RTCPeerConnection.h"

@implementation RTCPeerConnection (React)

- (NSNumber *)reactTag
{
  return objc_getAssociatedObject(self, _cmd);
}

- (void)setReactTag:(NSNumber *)reactTag
{
  objc_setAssociatedObject(self, @selector(reactTag), reactTag, OBJC_ASSOCIATION_COPY_NONATOMIC);
}

@end

@implementation WebRTCModule (RTCPeerConnection)

RCT_EXPORT_METHOD(peerConnectionInit:(NSDictionary *)configuration objectID:(nonnull NSNumber *)objectID)
{
  NSArray *iceServers = [self createIceServers:configuration[@"iceServers"]];

  RTCPeerConnection *peerConnection = [self.peerConnectionFactory peerConnectionWithICEServers:iceServers constraints:[self defaultPeerConnectionConstraints] delegate:self];
  peerConnection.reactTag = objectID;
  self.peerConnections[objectID] = peerConnection;
}

RCT_EXPORT_METHOD(peerConnectionAddStream:(nonnull NSNumber *)streamID objectID:(nonnull NSNumber *)objectID)
{
  RTCMediaStream *stream = self.mediaStreams[streamID];
  if (!stream) {
    return;
  }
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }
  BOOL result = [peerConnection addStream:stream];
  NSLog(@"result:%i", result);
}

RCT_EXPORT_METHOD(peerConnectionRemoveStream:(nonnull NSNumber *)streamID objectID:(nonnull NSNumber *)objectID)
{
  RTCMediaStream *stream = self.mediaStreams[streamID];
  if (!stream) {
    return;
  }
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }
  [peerConnection removeStream:stream];
}


RCT_EXPORT_METHOD(peerConnectionCreateOffer:(nonnull NSNumber *)objectID callback:(RCTResponseSenderBlock)callback)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  [peerConnection createOfferWithCallback:^(RTCSessionDescription *sdp, NSError *error) {
    if (error) {
      callback(@[@(NO),
                 @{@"type": @"CreateOfferFailed", @"message": error.userInfo[@"error"]}
                 ]);
    } else {
      callback(@[@(YES), @{@"sdp": sdp.description, @"type": sdp.type}]);
    }

  } constraints:nil];
}

- (RTCMediaConstraints *)defaultAnswerConstraints {
  return [self defaultOfferConstraints];
}

- (RTCMediaConstraints *)defaultOfferConstraints {
  NSArray *mandatoryConstraints = @[
                                    [[RTCPair alloc] initWithKey:@"OfferToReceiveAudio" value:@"true"],
                                    [[RTCPair alloc] initWithKey:@"OfferToReceiveVideo" value:@"true"]
                                    ];
  RTCMediaConstraints* constraints =
  [[RTCMediaConstraints alloc]
   initWithMandatoryConstraints:mandatoryConstraints
   optionalConstraints:nil];
  return constraints;
}

- (RTCMediaConstraints *)defaultPeerConnectionConstraints {
  NSArray *optionalConstraints = @[
                                   [[RTCPair alloc] initWithKey:@"DtlsSrtpKeyAgreement" value:@"true"]
                                   ];
  RTCMediaConstraints* constraints =
  [[RTCMediaConstraints alloc]
   initWithMandatoryConstraints:nil
   optionalConstraints:optionalConstraints];
  return constraints;
}

RCT_EXPORT_METHOD(peerConnectionCreateAnswer:(nonnull NSNumber *)objectID callback:(RCTResponseSenderBlock)callback)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  [peerConnection createAnswerWithCallback:^(RTCSessionDescription *sdp, NSError *error) {
    if (error) {
      callback(@[@(NO),
                 @{@"type": @"CreateAnsweFailed", @"message": error.userInfo[@"error"]}
                 ]);
    } else {
      callback(@[@(YES), @{@"sdp": sdp.description, @"type": sdp.type}]);
    }

  } constraints:[self defaultAnswerConstraints]];
}

RCT_EXPORT_METHOD(peerConnectionSetLocalDescription:(NSDictionary *)sdpJSON objectID:(nonnull NSNumber *)objectID callback:(RCTResponseSenderBlock)callback)
{
  RTCSessionDescription *sdp = [[RTCSessionDescription alloc] initWithType:sdpJSON[@"type"] sdp:sdpJSON[@"sdp"]];
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  [peerConnection setLocalDescriptionWithCallback:^(NSError *error) {
    if (error) {
      id errorResponse = @{@"name": @"SetLocalDescriptionFailed",
                           @"message": error.localizedDescription};
      callback(@[@(NO), errorResponse]);
    } else {
      callback(@[@(YES)]);
    }
  } sessionDescription:sdp];
}
RCT_EXPORT_METHOD(peerConnectionSetRemoteDescription:(NSDictionary *)sdpJSON objectID:(nonnull NSNumber *)objectID callback:(RCTResponseSenderBlock)callback)
{
  RTCSessionDescription *sdp = [[RTCSessionDescription alloc] initWithType:sdpJSON[@"type"] sdp:sdpJSON[@"sdp"]];
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  [peerConnection setRemoteDescriptionWithCallback:^(NSError *error) {
    if (error) {
      id errorResponse = @{@"name": @"SetRemoteDescriptionFailed",
                           @"message": error.localizedDescription};
      callback(@[@(NO), errorResponse]);
    } else {
      callback(@[@(YES)]);
    }
  } sessionDescription:sdp];
}

RCT_EXPORT_METHOD(peerConnectionAddICECandidate:(NSDictionary*)candidateJSON objectID:(nonnull NSNumber *)objectID callback:(RCTResponseSenderBlock)callback)
{
  RTCICECandidate *candidate = [[RTCICECandidate alloc] initWithMid:candidateJSON[@"sdpMid"] index:[candidateJSON[@"sdpMLineIndex"] integerValue] sdp:candidateJSON[@"candidate"]];
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  BOOL result = [peerConnection addICECandidate:candidate];
  NSLog(@"addICECandidateresult:%i, %@", result, candidate);
  callback(@[@(result)]);
}

RCT_EXPORT_METHOD(peerConnectionClose:(nonnull NSNumber *)objectID)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  [peerConnection close];
  [self.peerConnections removeObjectForKey:objectID];
}

RCT_EXPORT_METHOD(peerConnectionGetStats:(nonnull NSNumber *)trackID objectID:(nonnull NSNumber *)objectID callback:(RCTResponseSenderBlock)callback)
{
  RTCMediaStreamTrack *track = nil;
  if ([trackID integerValue] >= 0) {
    track = self.tracks[trackID];
  }

  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  BOOL result = [peerConnection getStatsWithCallback:^(NSArray *stats) {
    NSMutableArray *statsCollection = [NSMutableArray new];
    for (RTCStatsReport *statsReport in stats) {
      NSMutableArray *valuesCollection = [NSMutableArray new];
      for (RTCPair *pair in statsReport.values) {
        [valuesCollection addObject:@{pair.key: pair.value}];
      }
      [statsCollection addObject:@{
                                   @"id": statsReport.reportId,
                                   @"type": statsReport.type,
                                   @"timestamp": @(statsReport.timestamp),
                                   @"values": valuesCollection,
                                   }];
    }
    callback(@[statsCollection]);
    //    NSLog(@"getStatsWithCallback: %@, %@", streamID, stats);
  } mediaStreamTrack:track statsOutputLevel:RTCStatsOutputLevelStandard];
  NSLog(@"getStatsResult: %i", result);
}

- (NSArray*)createIceServers:(NSArray*)iceServersConfiguration {
  NSMutableArray *iceServers = [NSMutableArray new];
  if (iceServersConfiguration) {
    for (NSDictionary *iceServerConfiguration in iceServersConfiguration) {

      NSString *username = iceServerConfiguration[@"username"];
      if (!username) {
        username = @"";
      }
      NSString *credential = iceServerConfiguration[@"credential"];
      if (!credential) {
        credential = @"";
      }

      if (iceServerConfiguration[@"url"]) {
        NSString *url = iceServerConfiguration[@"url"];

        RTCICEServer *iceServer = [[RTCICEServer alloc] initWithURI:[NSURL URLWithString:url] username:username password:credential];
        [iceServers addObject:iceServer];
      } else if (iceServerConfiguration[@"urls"]) {
        if ([iceServerConfiguration[@"urls"] isKindOfClass:[NSString class]]) {
          NSString *url = iceServerConfiguration[@"urls"];

          RTCICEServer *iceServer = [[RTCICEServer alloc] initWithURI:[NSURL URLWithString:url] username:username password:credential];
          [iceServers addObject:iceServer];
        } else if ([iceServerConfiguration[@"urls"] isKindOfClass:[NSArray class]]) {
          NSArray *urls = iceServerConfiguration[@"urls"];
          for (NSString *url in urls) {
            RTCICEServer *iceServer = [[RTCICEServer alloc] initWithURI:[NSURL URLWithString:url] username:username password:credential];
            [iceServers addObject:iceServer];
          }
        }
      }
    }
  }
  return iceServers;
}

- (NSString *)stringForICEConnectionState:(RTCICEConnectionState)state {
  switch (state) {
    case RTCICEConnectionNew: return @"new";
    case RTCICEConnectionChecking: return @"checking";
    case RTCICEConnectionConnected: return @"connected";
    case RTCICEConnectionCompleted: return @"completed";
    case RTCICEConnectionFailed: return @"failed";
    case RTCICEConnectionDisconnected: return @"disconnected";
    case RTCICEConnectionClosed: return @"closed";
  }
  return nil;
}

- (NSString *)stringForICEGatheringState:(RTCICEGatheringState)state {
  switch (state) {
    case RTCICEGatheringNew: return @"new";
    case RTCICEGatheringGathering: return @"gathering";
    case RTCICEGatheringComplete: return @"complete";
  }
  return nil;
}

- (NSString *)stringForSignalingState:(RTCSignalingState)state {
  switch (state) {
    case RTCSignalingStable: return @"stable";
    case RTCSignalingHaveLocalOffer: return @"have-local-offer";
    case RTCSignalingHaveLocalPrAnswer: return @"have-local-pranswer";
    case RTCSignalingHaveRemoteOffer: return @"have-remote-offer";
    case RTCSignalingHaveRemotePrAnswer: return @"have-remote-pranswer";
    case RTCSignalingClosed: return @"closed";
  }
  return nil;
}

#pragma mark - RTCPeerConnectionDelegate methods

- (void)peerConnection:(RTCPeerConnection *)peerConnection signalingStateChanged:(RTCSignalingState)newState {
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionSignalingStateChanged" body:
   @{@"id": peerConnection.reactTag, @"signalingState": [self stringForSignalingState:newState]}];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection addedStream:(RTCMediaStream *)stream {
  NSNumber *objectID = @(self.mediaStreamId++);

  stream.reactTag = objectID;
  NSMutableArray *tracks = [NSMutableArray array];
  for (RTCVideoTrack *track in stream.videoTracks) {
    NSNumber *trackId = @(self.trackId++);
    track.reactTag = trackId;
    self.tracks[trackId] = track;
    [tracks addObject:@{@"id": trackId, @"kind": track.kind, @"label": track.label, @"enabled": @(track.isEnabled), @"remote": @(YES), @"readyState": @"live"}];
  }
  for (RTCAudioTrack *track in stream.audioTracks) {
    NSNumber *trackId = @(self.trackId++);
    track.reactTag = trackId;
    self.tracks[trackId] = track;
    [tracks addObject:@{@"id": trackId, @"kind": track.kind, @"label": track.label, @"enabled": @(track.isEnabled), @"remote": @(YES), @"readyState": @"live"}];
  }

  self.mediaStreams[objectID] = stream;
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionAddedStream" body:
   @{@"id": peerConnection.reactTag, @"streamId": stream.reactTag, @"tracks": tracks}];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection removedStream:(RTCMediaStream *)stream {
  // TODO: remove stream from self.mediaStreams
  if (self.mediaStreams[stream.reactTag]) {
    RTCMediaStream *mediaStream = self.mediaStreams[stream.reactTag];
    for (RTCVideoTrack *track in mediaStream.videoTracks) {
      [self.tracks removeObjectForKey:track.reactTag];
    }
    for (RTCAudioTrack *track in mediaStream.audioTracks) {
      [self.tracks removeObjectForKey:track.reactTag];
    }
    [self.mediaStreams removeObjectForKey:stream.reactTag];
  }
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionRemovedStream" body:
   @{@"id": peerConnection.reactTag, @"streamId": stream.reactTag}];
}

- (void)peerConnectionOnRenegotiationNeeded:(RTCPeerConnection *)peerConnection {
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionOnRenegotiationNeeded" body:
   @{@"id": peerConnection.reactTag}];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection iceConnectionChanged:(RTCICEConnectionState)newState {
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionIceConnectionChanged" body:
   @{@"id": peerConnection.reactTag, @"iceConnectionState": [self stringForICEConnectionState:newState]}];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection iceGatheringChanged:(RTCICEGatheringState)newState {
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionIceGatheringChanged" body:
   @{@"id": peerConnection.reactTag, @"iceGatheringState": [self stringForICEGatheringState:newState]}];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection gotICECandidate:(RTCICECandidate *)candidate {
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionGotICECandidate" body:
   @{@"id": peerConnection.reactTag, @"candidate": @{@"candidate": candidate.sdp, @"sdpMLineIndex": @(candidate.sdpMLineIndex), @"sdpMid": candidate.sdpMid}}];
}

- (void)peerConnection:(RTCPeerConnection*)peerConnection didOpenDataChannel:(RTCDataChannel*)dataChannel {
  NSInteger dataChannelId = dataChannel.streamId;
  // XXX RTP data channels are not defined by the WebRTC standard, have been
  // deprecated in Chromium, and Google have decided (in 2015) to no longer
  // support them (in the face of multiple reported issues of breakages).
  if (-1 == dataChannelId) {
    return;
  }

  self.dataChannels[@(dataChannelId)] = dataChannel;
  // WebRTCModule implements the category RTCDataChannel i.e. the protocol
  // RTCDataChannelDelegate.
  dataChannel.delegate = self;

  NSDictionary *body = @{@"id": peerConnection.reactTag,
                        @"dataChannel": @{@"id": @(dataChannelId),
                                          @"label": dataChannel.label}};
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionDidOpenDataChannel"
                                                  body:body];
}

@end
