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

#import "WebRTCModule.h"
#import "WebRTCModule+RTCDataChannel.h"
#import "WebRTCModule+RTCPeerConnection.h"
#import "WebRTCModule+VideoTrackAdapter.h"

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

- (NSMutableDictionary<NSString *, RTCMediaStream *> *)remoteStreams
{
    return objc_getAssociatedObject(self, _cmd);
}

- (void)setRemoteStreams:(NSMutableDictionary<NSString *,RTCMediaStream *> *)remoteStreams
{
    objc_setAssociatedObject(self, @selector(remoteStreams), remoteStreams, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSMutableDictionary<NSString *, RTCMediaStreamTrack *> *)remoteTracks
{
    return objc_getAssociatedObject(self, _cmd);
}

- (void)setRemoteTracks:(NSMutableDictionary<NSString *,RTCMediaStreamTrack *> *)remoteTracks
{
    objc_setAssociatedObject(self, @selector(remoteTracks), remoteTracks, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (id)webRTCModule
{
    return objc_getAssociatedObject(self, _cmd);
}

- (void)setWebRTCModule:(id)webRTCModule
{
    objc_setAssociatedObject(self, @selector(webRTCModule), webRTCModule, OBJC_ASSOCIATION_ASSIGN);
}

@end

@implementation WebRTCModule (RTCPeerConnection)

RCT_EXPORT_METHOD(peerConnectionInit:(RTCConfiguration*)configuration
                            objectID:(nonnull NSNumber *)objectID)
{
  NSDictionary *optionalConstraints = @{ @"DtlsSrtpKeyAgreement" : @"true" };
  RTCMediaConstraints* constraints =
      [[RTCMediaConstraints alloc] initWithMandatoryConstraints:nil
                                            optionalConstraints:optionalConstraints];
  RTCPeerConnection *peerConnection
    = [self.peerConnectionFactory
      peerConnectionWithConfiguration:configuration
			  constraints:constraints
                             delegate:self];

  peerConnection.dataChannels = [NSMutableDictionary new];
  peerConnection.reactTag = objectID;
  peerConnection.remoteStreams = [NSMutableDictionary new];
  peerConnection.remoteTracks = [NSMutableDictionary new];
  peerConnection.videoTrackAdapters = [NSMutableDictionary new];
  peerConnection.webRTCModule = self;
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
                                    options:(NSDictionary *)options
                                   callback:(RCTResponseSenderBlock)callback)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  RTCMediaConstraints *constraints =
    [[RTCMediaConstraints alloc] initWithMandatoryConstraints:options
                                          optionalConstraints:nil];

  [peerConnection
    offerForConstraints:constraints
      completionHandler:^(RTCSessionDescription *sdp, NSError *error) {
        if (error) {
          callback(@[
            @(NO),
            @{
              @"type": @"CreateOfferFailed",
              @"message": error.localizedDescription ?: [NSNull null]
            }
          ]);
        } else {
          NSString *type = [RTCSessionDescription stringForType:sdp.type];
          callback(@[@(YES), @{@"sdp": sdp.sdp, @"type": type}]);
        }
      }];
}

RCT_EXPORT_METHOD(peerConnectionCreateAnswer:(nonnull NSNumber *)objectID
                                     options:(NSDictionary *)options
                                    callback:(RCTResponseSenderBlock)callback)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  RTCMediaConstraints *constraints =
    [[RTCMediaConstraints alloc] initWithMandatoryConstraints:options
                                          optionalConstraints:nil];

  [peerConnection
    answerForConstraints:constraints
       completionHandler:^(RTCSessionDescription *sdp, NSError *error) {
         if (error) {
           callback(@[
             @(NO),
             @{
               @"type": @"CreateAnswerFailed",
               @"message": error.localizedDescription ?: [NSNull null]
             }
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
      id errorResponse = @{
        @"name": @"SetLocalDescriptionFailed",
        @"message": error.localizedDescription ?: [NSNull null]
      };
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
      id errorResponse = @{
        @"name": @"SetRemoteDescriptionFailed",
        @"message": error.localizedDescription ?: [NSNull null]
      };
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
  RCTLogTrace(@"addICECandidateresult: %@", candidate);
  callback(@[@true]);
}

RCT_EXPORT_METHOD(peerConnectionClose:(nonnull NSNumber *)objectID)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  // Remove video track adapters
  for(RTCMediaStream *stream in [peerConnection.remoteStreams allValues]) {
    for (RTCVideoTrack *track in stream.videoTracks) {
      [peerConnection removeVideoTrackAdapter:track];
    }
  }

  [peerConnection close];
  [self.peerConnections removeObjectForKey:objectID];

  // Clean up peerConnection's streams and tracks
  [peerConnection.remoteStreams removeAllObjects];
  [peerConnection.remoteTracks removeAllObjects];

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

RCT_EXPORT_METHOD(peerConnectionGetStats:(nonnull NSString *)trackID
                                objectID:(nonnull NSNumber *)objectID
                                callback:(RCTResponseSenderBlock)callback)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    callback(@[@(NO), @"PeerConnection ID not found"]);
    return;
  }

  RTCMediaStreamTrack *track = nil;
  if (!trackID
      || !trackID.length
      || (track = self.localTracks[trackID])
      || (track = peerConnection.remoteTracks[trackID])) {
    [peerConnection statsForTrack:track
                 statsOutputLevel:RTCStatsOutputLevelStandard
                completionHandler:^(NSArray<RTCLegacyStatsReport *> *stats) {
                  callback(@[@(YES), [self statsToJSON:stats]]);
                }];
  } else {
    callback(@[@(NO), @"Track not found"]);
  }
}

/**
 * Constructs a JSON <tt>NSString</tt> representation of a specific array of
 * <tt>RTCLegacyStatsReport</tt>s.
 * <p>
 * On iOS it is faster to (1) construct a single JSON <tt>NSString</tt>
 * representation of an array of <tt>RTCLegacyStatsReport</tt>s and (2) have it
 * pass through the React Native bridge rather than the array of
 * <tt>RTCLegacyStatsReport</tt>s.
 *
 * @param reports the array of <tt>RTCLegacyStatsReport</tt>s to represent in
 * JSON format
 * @return an <tt>NSString</tt> which represents the specified <tt>stats</tt> in
 * JSON format
 */
- (NSString *)statsToJSON:(NSArray<RTCLegacyStatsReport *> *)reports
{
  // XXX The initial capacity matters, of course, because it determines how many
  // times the NSMutableString will have grow. But walking through the reports
  // to compute an initial capacity which exactly matches the requirements of
  // the reports is too much work without real-world bang here. A better
  // approach is what the Android counterpart does i.e. cache the
  // NSMutableString and preferably with a Java-like soft reference. If that is
  // too much work, then an improvement should be caching the required capacity
  // from the previous invocation of the method and using it as the initial
  // capacity in the next invocation. As I didn't want to go even through that,
  // choosing just about any initial capacity is OK because NSMutableCopy
  // doesn't have too bad a strategy of growing.
  NSMutableString *s = [NSMutableString stringWithCapacity:8 * 1024];

  [s appendString:@"["];
  BOOL firstReport = YES;
  for (RTCLegacyStatsReport *report in reports) {
    if (firstReport) {
      firstReport = NO;
    } else {
      [s appendString:@","];
    }
    [s appendString:@"{\"id\":\""]; [s appendString:report.reportId];
    [s appendString:@"\",\"type\":\""]; [s appendString:report.type];
    [s appendString:@"\",\"timestamp\":"];
    [s appendFormat:@"%f", report.timestamp];
    [s appendString:@",\"values\":["];
    __block BOOL firstValue = YES;
    [report.values enumerateKeysAndObjectsUsingBlock:^(
        NSString *key,
        NSString *value,
        BOOL *stop) {
      if (firstValue) {
        firstValue = NO;
      } else {
        [s appendString:@","];
      }
      [s appendString:@"{\""]; [s appendString:key];
      [s appendString:@"\":\""]; [s appendString:value];
      [s appendString:@"\"}"];
    }];
    [s appendString:@"]}"];
  }
  [s appendString:@"]"];

  return s;
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
  NSString *streamReactTag = [[NSUUID UUID] UUIDString];
  NSMutableArray *tracks = [NSMutableArray array];
  for (RTCVideoTrack *track in stream.videoTracks) {
    peerConnection.remoteTracks[track.trackId] = track;
    [peerConnection addVideoTrackAdapter:streamReactTag track:track];
    [tracks addObject:@{@"id": track.trackId, @"kind": track.kind, @"label": track.trackId, @"enabled": @(track.isEnabled), @"remote": @(YES), @"readyState": @"live"}];
  }
  for (RTCAudioTrack *track in stream.audioTracks) {
    peerConnection.remoteTracks[track.trackId] = track;
    [tracks addObject:@{@"id": track.trackId, @"kind": track.kind, @"label": track.trackId, @"enabled": @(track.isEnabled), @"remote": @(YES), @"readyState": @"live"}];
  }

  peerConnection.remoteStreams[streamReactTag] = stream;
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"peerConnectionAddedStream"
                                                  body:@{@"id": peerConnection.reactTag, @"streamId": stream.streamId, @"streamReactTag": streamReactTag, @"tracks": tracks}];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didRemoveStream:(RTCMediaStream *)stream {
  // XXX Find the stream by comparing the 'streamId' values. It turns out that WebRTC (as of M69) creates new wrapper
  // instance for the native media stream before invoking the 'didRemoveStream' callback. This means it's a different
  // RTCMediaStream instance passed to 'didAddStream' and 'didRemoveStream'.
  NSString *streamReactTag = nil;
  for (NSString *aReactTag in peerConnection.remoteStreams) {
    RTCMediaStream *aStream = peerConnection.remoteStreams[aReactTag];
    if ([aStream.streamId isEqualToString:stream.streamId]) {
      streamReactTag = aReactTag;
      break;
    }
  }
  if (!streamReactTag) {
    RCTLogWarn(@"didRemoveStream - stream not found, id: %@", stream.streamId);
    return;
  }
  for (RTCVideoTrack *track in stream.videoTracks) {
    [peerConnection removeVideoTrackAdapter:track];
    [peerConnection.remoteTracks removeObjectForKey:track.trackId];
  }
  for (RTCAudioTrack *track in stream.audioTracks) {
    [peerConnection.remoteTracks removeObjectForKey:track.trackId];
  }
  [peerConnection.remoteStreams removeObjectForKey:streamReactTag];
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

- (void)peerConnection:(nonnull RTCPeerConnection *)peerConnection didRemoveIceCandidates:(nonnull NSArray<RTCIceCandidate *> *)candidates {
  // TODO
}

@end
