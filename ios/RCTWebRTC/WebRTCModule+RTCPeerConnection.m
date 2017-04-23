//
//  WebRTCModule+RTCPeerConnection.m
//
//  Created by one on 2015/9/24.
//  Copyright © 2015 One. All rights reserved.
//

#import <objc/runtime.h>

#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTLog.h>
#import <React/RCTUtils.h>

#import <WebRTC/RTCConfiguration.h>
#import <WebRTC/RTCIceCandidate.h>
#import <WebRTC/RTCIceServer.h>
#import <WebRTC/RTCMediaConstraints.h>
#import <WebRTC/RTCIceCandidate.h>
#import <WebRTC/RTCLegacyStatsReport.h>
#import <WebRTC/RTCSessionDescription.h>

#import "WebRTCModule+RTCDataChannel.h"
#import "WebRTCModule+RTCPeerConnection.h"

@implementation RTCPeerConnection (React)

- (NSMutableDictionary<NSNumber *, RTCDataChannel *> *)dataChannels
{
  return objc_getAssociatedObject(self, _cmd);
}

- (void)setDataChannels:(NSMutableDictionary<NSNumber *, RTCDataChannel *> *)dataChannels
{
  objc_setAssociatedObject(self, @selector(dataChannels), dataChannels, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSNumber *)reactTag
{
  return objc_getAssociatedObject(self, _cmd);
}

- (void)setReactTag:(NSNumber *)reactTag
{
  objc_setAssociatedObject(self, @selector(reactTag), reactTag, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

@end

@implementation WebRTCModule (RTCPeerConnection)

-(NSString*)reactTagForID:(NSString*)streamOrTrackID RTCPC:(RTCPeerConnection*)pc
{
    return [NSString stringWithFormat:@"%@_%@", pc.reactTag, streamOrTrackID];
}

RCT_EXPORT_METHOD(peerConnectionInit:(RTCConfiguration*)configuration
                         constraints:(NSDictionary *)constraints
                            objectID:(nonnull NSNumber *)objectID)
{
  RTCPeerConnection *peerConnection
    = [self.peerConnectionFactory
      peerConnectionWithConfiguration:configuration
			  constraints:[self parseMediaConstraints:constraints]
                             delegate:self];
  peerConnection.dataChannels = [NSMutableDictionary new];
  peerConnection.reactTag = objectID;
  self.peerConnections[objectID] = peerConnection;
}

RCT_EXPORT_METHOD(peerConnectionSetConfiguration:(RTCConfiguration*)configuration objectID:(nonnull NSNumber *)objectID)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }
  [peerConnection setConfiguration:configuration];
}

RCT_EXPORT_METHOD(peerConnectionAddStream:(nonnull NSString *)streamID objectID:(nonnull NSNumber *)objectID)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }
  RTCMediaStream *stream = self.localStreams[streamID];
  if (!stream) {
    return;
  }

  [peerConnection addStream:stream];
}

RCT_EXPORT_METHOD(peerConnectionRemoveStream:(nonnull NSString *)streamID objectID:(nonnull NSNumber *)objectID)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }
  RTCMediaStream *stream = self.localStreams[streamID];
  if (!stream) {
    return;
  }

  [peerConnection removeStream:stream];
}


RCT_EXPORT_METHOD(peerConnectionCreateOffer:(nonnull NSNumber *)objectID
                                constraints:(NSDictionary *)constraints
                                   callback:(RCTResponseSenderBlock)callback)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  [peerConnection
    offerForConstraints:[self parseMediaConstraints:constraints]
      completionHandler:^(RTCSessionDescription *sdp, NSError *error) {
        if (error) {
          callback(@[
            @(NO),
            @{
              @"type": @"CreateOfferFailed",
              @"message": error.userInfo[@"error"]}
          ]);
        } else {
          NSString *type = [RTCSessionDescription stringForType:sdp.type];
          callback(@[@(YES), @{@"sdp": sdp.sdp, @"type": type}]);
        }
      }];
}

RCT_EXPORT_METHOD(peerConnectionCreateAnswer:(nonnull NSNumber *)objectID
                                 constraints:(NSDictionary *)constraints
                                    callback:(RCTResponseSenderBlock)callback)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  [peerConnection
    answerForConstraints:[self parseMediaConstraints:constraints]
       completionHandler:^(RTCSessionDescription *sdp, NSError *error) {
         if (error) {
           callback(@[
             @(NO),
             @{
               @"type": @"CreateAnswerFailed",
               @"message": error.userInfo[@"error"]}
           ]);
         } else {
           NSString *type = [RTCSessionDescription stringForType:sdp.type];
           callback(@[@(YES), @{@"sdp": sdp.sdp, @"type": type}]);
         }
       }];
}

RCT_EXPORT_METHOD(peerConnectionSetLocalDescription:(RTCSessionDescription *)sdp objectID:(nonnull NSNumber *)objectID callback:(RCTResponseSenderBlock)callback)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  [peerConnection setLocalDescription:sdp completionHandler: ^(NSError *error) {
    if (error) {
      id errorResponse = @{@"name": @"SetLocalDescriptionFailed",
                           @"message": error.localizedDescription};
      callback(@[@(NO), errorResponse]);
    } else {
      callback(@[@(YES)]);
    }
  }];
}

RCT_EXPORT_METHOD(peerConnectionSetRemoteDescription:(RTCSessionDescription *)sdp objectID:(nonnull NSNumber *)objectID callback:(RCTResponseSenderBlock)callback)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  [peerConnection setRemoteDescription: sdp completionHandler: ^(NSError *error) {
    if (error) {
      id errorResponse = @{@"name": @"SetRemoteDescriptionFailed",
                           @"message": error.localizedDescription};
      callback(@[@(NO), errorResponse]);
    } else {
      callback(@[@(YES)]);
    }
  }];
}

RCT_EXPORT_METHOD(peerConnectionAddICECandidate:(RTCIceCandidate*)candidate objectID:(nonnull NSNumber *)objectID callback:(RCTResponseSenderBlock)callback)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  [peerConnection addIceCandidate:candidate];
  NSLog(@"addICECandidateresult: %@", candidate);
  callback(@[@true]);
}

RCT_EXPORT_METHOD(peerConnectionClose:(nonnull NSNumber *)objectID)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  NSMutableArray *discardedStreams = [NSMutableArray array];
    
  for (NSString *streamReactTag in self.remoteStreams.keyEnumerator) {
    if ([streamReactTag hasPrefix:[NSString stringWithFormat:@"%@_", peerConnection.reactTag]]) {
      RTCMediaStream *remoteStream = self.remoteStreams[streamReactTag];
      for (RTCMediaStreamTrack *track in remoteStream.audioTracks) {
        NSString *tag = [self reactTagForID: track.trackId RTCPC: peerConnection];
        [self.remoteTracks removeObjectForKey: tag];
      }
      for (RTCMediaStreamTrack *track in remoteStream.videoTracks) {
        NSString *tag = [self reactTagForID: track.trackId RTCPC: peerConnection];
        [self.remoteTracks removeObjectForKey: tag];
      }
      [discardedStreams addObject:streamReactTag];
    }
  }
  [self.remoteStreams removeObjectsForKeys:discardedStreams];

  [peerConnection close];
  [self.peerConnections removeObjectForKey:objectID];

  // Clean up peerConnection's dataChannels.
  NSMutableDictionary<NSNumber *, RTCDataChannel *> *dataChannels
    = peerConnection.dataChannels;
  for (NSNumber *dataChannelId in dataChannels) {
    dataChannels[dataChannelId].delegate = nil;
    // There is no need to close the RTCDataChannel because it is owned by the
    // RTCPeerConnection and the latter will close the former.
  }
  [dataChannels removeAllObjects];
}

RCT_EXPORT_METHOD(peerConnectionGetStats:(nonnull NSString *)trackID objectID:(nonnull NSNumber *)objectID callback:(RCTResponseSenderBlock)callback)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  RTCMediaStreamTrack *track = nil;
  if (trackID && trackID.length > 0) {
    track = self.localTracks[trackID];
    if (!track) {
      track = self.remoteTracks[[self reactTagForID:trackID RTCPC: peerConnection]];
    }
  }

  // FIXME can this crash with track == nil ?
  [peerConnection statsForTrack:track
               statsOutputLevel: RTCStatsOutputLevelStandard
              completionHandler: ^(NSArray<RTCLegacyStatsReport *> *stats) {
    NSMutableArray *statsCollection = [NSMutableArray new];
    for (RTCLegacyStatsReport *statsReport in stats) {
      NSMutableArray *valuesCollection = [NSMutableArray new];
      for (NSString *key in statsReport.values) {
        NSString *value = [statsReport.values objectForKey:key];
        [valuesCollection addObject:@{key: value}];
      }
      [statsCollection addObject:@{
                                   @"id": statsReport.reportId,
                                   @"type": statsReport.type,
                                   @"timestamp": @(statsReport.timestamp),
                                   @"values": valuesCollection,
                                   }];
    }
    callback(@[statsCollection]);
  }];
}

- (NSString *)stringForICEConnectionState:(RTCIceConnectionState)state {
  switch (state) {
    case RTCIceConnectionStateNew: return @"new";
    case RTCIceConnectionStateChecking: return @"checking";
    case RTCIceConnectionStateConnected: return @"connected";
    case RTCIceConnectionStateCompleted: return @"completed";
    case RTCIceConnectionStateFailed: return @"failed";
    case RTCIceConnectionStateDisconnected: return @"disconnected";
    case RTCIceConnectionStateClosed: return @"closed";
    case RTCIceConnectionStateCount: return @"count";
  }
  return nil;
}

- (NSString *)stringForICEGatheringState:(RTCIceGatheringState)state {
  switch (state) {
    case RTCIceGatheringStateNew: return @"new";
    case RTCIceGatheringStateGathering: return @"gathering";
    case RTCIceGatheringStateComplete: return @"complete";
  }
  return nil;
}

- (NSString *)stringForSignalingState:(RTCSignalingState)state {
  switch (state) {
    case RTCSignalingStateStable: return @"stable";
    case RTCSignalingStateHaveLocalOffer: return @"have-local-offer";
    case RTCSignalingStateHaveLocalPrAnswer: return @"have-local-pranswer";
    case RTCSignalingStateHaveRemoteOffer: return @"have-remote-offer";
    case RTCSignalingStateHaveRemotePrAnswer: return @"have-remote-pranswer";
    case RTCSignalingStateClosed: return @"closed";
  }
  return nil;
}

#pragma mark - RTCPeerConnectionDelegate methods

- (void)peerConnection:(RTCPeerConnection *)peerConnection didChangeSignalingState:(RTCSignalingState)newState {
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionSignalingStateChanged" body:
   @{@"id": peerConnection.reactTag, @"signalingState": [self stringForSignalingState:newState]}];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didAddStream:(RTCMediaStream *)stream {
  NSMutableArray *tracks = [NSMutableArray array];
  for (RTCVideoTrack *track in stream.videoTracks) {
    NSString *tag = [self reactTagForID:track.trackId RTCPC:peerConnection];
    self.remoteTracks[tag] = track;
    [tracks addObject:@{@"id": track.trackId, @"kind": track.kind, @"label": track.trackId, @"enabled": @(track.isEnabled), @"remote": @(YES), @"readyState": @"live"}];
  }
  for (RTCAudioTrack *track in stream.audioTracks) {
    NSString *tag = [self reactTagForID:track.trackId RTCPC:peerConnection];
    self.remoteTracks[tag] = track;
    [tracks addObject:@{@"id": track.trackId, @"kind": track.kind, @"label": track.trackId, @"enabled": @(track.isEnabled), @"remote": @(YES), @"readyState": @"live"}];
  }

  NSString *streamReactTag =  [self reactTagForID:stream.streamId RTCPC:peerConnection];
  self.remoteStreams[streamReactTag] = stream;
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionAddedStream"
                                                  body:@{@"id": peerConnection.reactTag, @"streamId": stream.streamId, @"streamReactTag": streamReactTag, @"tracks": tracks}];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didRemoveStream:(RTCMediaStream *)stream {
  NSString *streamReactTag = [self reactTagForID:stream.streamId RTCPC:peerConnection];
  RTCMediaStream *mediaStream = self.remoteStreams[streamReactTag];
  if (mediaStream) {
    for (RTCVideoTrack *track in mediaStream.videoTracks) {
      [self.remoteTracks removeObjectForKey:[self reactTagForID: track.trackId RTCPC: peerConnection]];
    }
    for (RTCAudioTrack *track in mediaStream.audioTracks) {
      [self.remoteTracks removeObjectForKey:[self reactTagForID: track.trackId RTCPC: peerConnection]];
    }
    [self.remoteStreams removeObjectForKey:streamReactTag];
  }
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionRemovedStream" body:
   @{@"id": peerConnection.reactTag, @"streamId": streamReactTag}];
}

- (void)peerConnectionShouldNegotiate:(RTCPeerConnection *)peerConnection {
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionOnRenegotiationNeeded" body:
   @{@"id": peerConnection.reactTag}];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didChangeIceConnectionState:(RTCIceConnectionState)newState {
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionIceConnectionChanged" body:
   @{@"id": peerConnection.reactTag, @"iceConnectionState": [self stringForICEConnectionState:newState]}];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didChangeIceGatheringState:(RTCIceGatheringState)newState {
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionIceGatheringChanged" body:
   @{@"id": peerConnection.reactTag, @"iceGatheringState": [self stringForICEGatheringState:newState]}];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didGenerateIceCandidate:(RTCIceCandidate *)candidate {
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionGotICECandidate" body:
   @{@"id": peerConnection.reactTag, @"candidate": @{@"candidate": candidate.sdp, @"sdpMLineIndex": @(candidate.sdpMLineIndex), @"sdpMid": candidate.sdpMid}}];
}

- (void)peerConnection:(RTCPeerConnection*)peerConnection didOpenDataChannel:(RTCDataChannel*)dataChannel {
  // XXX RTP data channels are not defined by the WebRTC standard, have been
  // deprecated in Chromium, and Google have decided (in 2015) to no longer
  // support them (in the face of multiple reported issues of breakages).
  if (-1 == dataChannel.channelId) {
    return;
  }

  NSNumber *dataChannelId = [NSNumber numberWithInteger:dataChannel.channelId];
  dataChannel.peerConnectionId = peerConnection.reactTag;
  peerConnection.dataChannels[dataChannelId] = dataChannel;
  // WebRTCModule implements the category RTCDataChannel i.e. the protocol
  // RTCDataChannelDelegate.
  dataChannel.delegate = self;

  NSDictionary *body = @{@"id": peerConnection.reactTag,
                        @"dataChannel": @{@"id": dataChannelId,
                                          @"label": dataChannel.label}};
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionDidOpenDataChannel"
                                                  body:body];
}

/**
 * Parses the constraint keys and values of a specific JavaScript object into
 * a specific <tt>NSMutableDictionary</tt> in a format suitable for the
 * initialization of a <tt>RTCMediaConstraints</tt> instance.
 *
 * @param src The JavaScript object which defines constraint keys and values and
 * which is to be parsed into the specified <tt>dst</tt>.
 * @param dst The <tt>NSMutableDictionary</tt> into which the constraint keys
 * and values defined by <tt>src</tt> are to be written in a format suitable for
 * the initialization of a <tt>RTCMediaConstraints</tt> instance.
 */
- (void)parseJavaScriptConstraints:(NSDictionary *)src
             intoWebRTCConstraints:(NSMutableDictionary<NSString *, NSString *> *)dst {
  for (id srcKey in src) {
    id srcValue = src[srcKey];
    NSString *dstValue;

    if ([srcValue isKindOfClass:[NSNumber class]]) {
      dstValue = [srcValue boolValue] ? @"true" : @"false";
    } else {
      dstValue = [srcValue description];
    }
    dst[[srcKey description]] = dstValue;
  }
}

/**
 * Parses a JavaScript object into a new <tt>RTCMediaConstraints</tt> instance.
 *
 * @param constraints The JavaScript object to parse into a new
 * <tt>RTCMediaConstraints</tt> instance.
 * @returns A new <tt>RTCMediaConstraints</tt> instance initialized with the
 * mandatory and optional constraint keys and values specified by
 * <tt>constraints</tt>.
 */
- (RTCMediaConstraints *)parseMediaConstraints:(NSDictionary *)constraints {
  id mandatory = constraints[@"mandatory"];
  NSMutableDictionary<NSString *, NSString *> *mandatory_
    = [NSMutableDictionary new];

  if ([mandatory isKindOfClass:[NSDictionary class]]) {
    [self parseJavaScriptConstraints:(NSDictionary *)mandatory
               intoWebRTCConstraints:mandatory_];
  }

  id optional = constraints[@"optional"];
  NSMutableDictionary<NSString *, NSString *> *optional_
    = [NSMutableDictionary new];

  if ([optional isKindOfClass:[NSArray class]]) {
    for (id o in (NSArray *)optional) {
      if ([o isKindOfClass:[NSDictionary class]]) {
        [self parseJavaScriptConstraints:(NSDictionary *)o
                   intoWebRTCConstraints:optional_];
      }
    }
  }

  return [[RTCMediaConstraints alloc] initWithMandatoryConstraints:mandatory_
                                               optionalConstraints:optional_];
}

@end
