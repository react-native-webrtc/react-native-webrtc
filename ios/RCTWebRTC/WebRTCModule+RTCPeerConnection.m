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
#import <WebRTC/RTCSessionDescription.h>
#import <WebRTC/RTCStatisticsReport.h>

#import "WebRTCModule.h"
#import "WebRTCModule+RTCDataChannel.h"
#import "WebRTCModule+RTCPeerConnection.h"
#import "WebRTCModule+VideoTrackAdapter.h"

@implementation RTCPeerConnection (React)

- (NSMutableDictionary<NSString *, DataChannelWrapper *> *)dataChannels
{
  return objc_getAssociatedObject(self, _cmd);
}

- (void)setDataChannels:(NSMutableDictionary<NSString *, DataChannelWrapper *> *)dataChannels
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

/*
 * This method is synchronous and blocking. This is done so we can implement createDataChannel
 * in the same way (synchronous) since the peer connection needs to exist before.
 */
RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(peerConnectionInit:(RTCConfiguration*)configuration
                                                 objectID:(nonnull NSNumber *)objectID)
{
    dispatch_sync(self.workerQueue, ^{
        NSDictionary *optionalConstraints = @{ @"DtlsSrtpKeyAgreement" : @"true" };
        RTCMediaConstraints* constraints =
            [[RTCMediaConstraints alloc] initWithMandatoryConstraints:nil
                                                  optionalConstraints:optionalConstraints];
          RTCPeerConnection *peerConnection
            = [self.peerConnectionFactory peerConnectionWithConfiguration:configuration
                                                              constraints:constraints
                                                                 delegate:self];
          peerConnection.dataChannels = [NSMutableDictionary new];
          peerConnection.reactTag = objectID;
          peerConnection.remoteStreams = [NSMutableDictionary new];
          peerConnection.remoteTracks = [NSMutableDictionary new];
          peerConnection.videoTrackAdapters = [NSMutableDictionary new];
          peerConnection.webRTCModule = self;

          self.peerConnections[objectID] = peerConnection;
    });

    return nil;
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

RCT_EXPORT_METHOD(peerConnectionCreateAnswer:(nonnull NSNumber *)peerConnectionId
                                     options:(NSDictionary *)options
                                    callback:(RCTResponseSenderBlock)callback)
{
  RTCPeerConnection *peerConnection = self.peerConnections[peerConnectionId];
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

RCT_EXPORT_METHOD(peerConnectionSetLocalDescription:(nonnull NSNumber *)objectID
                                               desc:(RTCSessionDescription *)desc
                                           resolver:(RCTPromiseResolveBlock)resolve
                                           rejecter:(RCTPromiseRejectBlock)reject)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    reject(@"E_INVALID", @"PeerConnection not found", nil);
    return;
  }

  __weak RTCPeerConnection *weakPc = peerConnection;

  RTCSetSessionDescriptionCompletionHandler handler = ^(NSError *error) {
      if (error) {
          reject(@"E_OPERATION_ERROR", error.localizedDescription, nil);
      } else {
        RTCPeerConnection *strongPc = weakPc;
        id newSdp = @{
            @"type": [RTCSessionDescription stringForType:strongPc.localDescription.type],
            @"sdp": strongPc.localDescription.sdp
        };
        resolve(newSdp);
      }
  };

  if (desc == nil) {
    [peerConnection setLocalDescriptionWithCompletionHandler:handler];
  } else {
    [peerConnection setLocalDescription:desc completionHandler:handler];
  }
}

RCT_EXPORT_METHOD(peerConnectionSetRemoteDescription:(RTCSessionDescription *)sdp objectID:(nonnull NSNumber *)objectID callback:(RCTResponseSenderBlock)callback)
{
  RTCPeerConnection __weak *peerConnection = self.peerConnections[objectID];
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
      id newSdp = @{
          @"type": [RTCSessionDescription stringForType:peerConnection.remoteDescription.type],
          @"sdp": peerConnection.remoteDescription.sdp
      };
      callback(@[@(YES), newSdp]);
    }
  }];
}

RCT_EXPORT_METHOD(peerConnectionAddICECandidate:(nonnull NSNumber *)objectID
                                      candidate:(RTCIceCandidate*)candidate
                                       resolver:(RCTPromiseResolveBlock)resolve
                                       rejecter:(RCTPromiseRejectBlock)reject)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    reject(@"E_INVALID", @"PeerConnection not found", nil);
    return;
  }

  __weak RTCPeerConnection *weakPc = peerConnection;
  [peerConnection addIceCandidate:candidate
                completionHandler:^(NSError *error) {
                  if (error) {
                      reject(@"E_OPERATION_ERROR", @"addIceCandidate failed", error);
                  } else {
                      RTCPeerConnection *strongPc = weakPc;
                      id newSdp = @{
                          @"type": [RTCSessionDescription stringForType:strongPc.remoteDescription.type],
                          @"sdp": strongPc.remoteDescription.sdp
                      };
                      resolve(newSdp);
                  }
                }];
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

  // Clean up peerConnection's streams and tracks
  [peerConnection.remoteStreams removeAllObjects];
  [peerConnection.remoteTracks removeAllObjects];

  // Clean up peerConnection's dataChannels.
  NSMutableDictionary<NSString *, DataChannelWrapper *> *dataChannels = peerConnection.dataChannels;
  for (NSString *tag in dataChannels) {
    dataChannels[tag].delegate = nil;
    // There is no need to close the RTCDataChannel because it is owned by the
    // RTCPeerConnection and the latter will close the former.
  }
  [dataChannels removeAllObjects];

  [self.peerConnections removeObjectForKey:objectID];
}

RCT_EXPORT_METHOD(peerConnectionGetStats:(nonnull NSNumber *) objectID
                                resolver:(RCTPromiseResolveBlock)resolve
                                rejecter:(RCTPromiseRejectBlock)reject)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    reject(@"invalid_id", @"PeerConnection ID not found", nil);
    return;
  }

  [peerConnection statisticsWithCompletionHandler:^(RTCStatisticsReport *report) {
    resolve([self statsToJSON:report]);
  }];
}

RCT_EXPORT_METHOD(peerConnectionRestartIce:(nonnull NSNumber *)objectID)
{
  RTCPeerConnection *peerConnection = self.peerConnections[objectID];
  if (!peerConnection) {
    return;
  }

  [peerConnection restartIce];
}

/**
 * Constructs a JSON <tt>NSString</tt> representation of a specific
 * <tt>RTCStatisticsReport</tt>s.
 * <p>
 *
 * @param <tt>RTCStatisticsReport</tt>s
 * @return an <tt>NSString</tt> which represents the specified <tt>report</tt> in
 * JSON format
 */
- (NSString *)statsToJSON:(RTCStatisticsReport *)report
{
  /* 
  The initial capacity matters, of course, because it determines how many
  times the NSMutableString will have grow. But walking through the reports
  to compute an initial capacity which exactly matches the requirements of
  the reports is too much work without real-world bang here. An improvement
  should be caching the required capacity from the previous invocation of the 
  method and using it as the initial capacity in the next invocation. 
  As I didn't want to go even through that,choosing just about any initial 
  capacity is OK because NSMutableCopy doesn't have too bad a strategy of growing.
  */
  NSMutableString *s = [NSMutableString stringWithCapacity:16 * 1024];

  [s appendString:@"["];
  BOOL firstReport = YES;
  for (NSString *key in report.statistics.allKeys) {
    if (firstReport) {
      firstReport = NO;
    } else {
      [s appendString:@","];
    }
  
    [s appendString:@"[\""];
    [s appendString: key];
    [s appendString:@"\",{"];

    RTCStatistics *statistics = report.statistics[key];
    [s appendString:@"\"timestamp\":"];
    [s appendFormat:@"%f", statistics.timestamp_us / 1000.0];
    [s appendString:@",\"type\":\""]; 
    [s appendString:statistics.type];
    [s appendString:@"\",\"id\":\""];
    [s appendString:statistics.id];
    [s appendString:@"\""];

    for (id key in statistics.values) {
        [s appendString:@","];
        [s appendString:@"\""];
        [s appendString:key];
        [s appendString:@"\":"];
        NSObject *statisticsValue = [statistics.values objectForKey:key];
        [self appendValue:statisticsValue toString:s];
    }

    [s appendString:@"}]"];
  } 

  [s appendString:@"]"];

  return s;
}

- (void)appendValue:(NSObject *)statisticsValue toString:(NSMutableString *)s {
    if ([statisticsValue isKindOfClass:[NSArray class]]) {
        [s appendString:@"["];
        BOOL firstValue = YES;
        for (NSObject *element in (NSArray *)statisticsValue) {
            if(firstValue) {
                firstValue = NO;
            } else {
                [s appendString:@","];
            }

            [s appendString:@"\""];
            [s appendString:[NSString stringWithFormat:@"%@", element]];
            [s appendString:@"\""];
      }
    
      [s appendString:@"]"];
    } else if ([statisticsValue isKindOfClass:[NSDictionary class]]) {
        NSError *error;
        NSData *jsonData = [NSJSONSerialization dataWithJSONObject:statisticsValue
                                                           options:0
                                                           error:&error];

        if (!jsonData) {
            [s appendString:@"\""];
            [s appendString:[NSString stringWithFormat:@"%@", statisticsValue]];
            [s appendString:@"\""];
        } else {
            NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
            [s appendString:jsonString];
        }
    } else {
        [s appendString:@"\""];
        [s appendString:[NSString stringWithFormat:@"%@", statisticsValue]];
        [s appendString:@"\""];
    }
}

- (NSString *)stringForPeerConnectionState:(RTCPeerConnectionState)state {
  switch (state) {
    case RTCPeerConnectionStateNew: return @"new";
    case RTCPeerConnectionStateConnecting: return @"connecting";
    case RTCPeerConnectionStateConnected: return @"connected";
    case RTCPeerConnectionStateDisconnected: return @"disconnected";
    case RTCPeerConnectionStateFailed: return @"failed";
    case RTCPeerConnectionStateClosed: return @"closed";
  }
  return nil;
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
  [self sendEventWithName:kEventPeerConnectionSignalingStateChanged
                     body:@{
                       @"id": peerConnection.reactTag,
                       @"signalingState": [self stringForSignalingState:newState]
                     }];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didAddStream:(RTCMediaStream *)stream {
  NSString *streamReactTag = [[NSUUID UUID] UUIDString];
  NSMutableArray *tracks = [NSMutableArray array];
  for (RTCVideoTrack *track in stream.videoTracks) {
    peerConnection.remoteTracks[track.trackId] = track;
    [peerConnection addVideoTrackAdapter:track];
    [tracks addObject:@{@"id": track.trackId, @"kind": track.kind, @"label": track.trackId, @"enabled": @(track.isEnabled), @"remote": @(YES), @"readyState": @"live"}];
  }
  for (RTCAudioTrack *track in stream.audioTracks) {
    peerConnection.remoteTracks[track.trackId] = track;
    [tracks addObject:@{@"id": track.trackId, @"kind": track.kind, @"label": track.trackId, @"enabled": @(track.isEnabled), @"remote": @(YES), @"readyState": @"live"}];
  }

  peerConnection.remoteStreams[streamReactTag] = stream;

  id newSdp = @{
    @"type": [RTCSessionDescription stringForType:peerConnection.remoteDescription.type],
    @"sdp": peerConnection.remoteDescription.sdp
  };

  [self sendEventWithName:kEventPeerConnectionAddedStream
                     body:@{
                       @"id": peerConnection.reactTag,
                       @"streamId": stream.streamId,
                       @"streamReactTag": streamReactTag,
                       @"tracks": tracks,
                       @"sdp": newSdp
                     }];
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

  id newSdp = @{
    @"type": [RTCSessionDescription stringForType:peerConnection.remoteDescription.type],
    @"sdp": peerConnection.remoteDescription.sdp
  };

  [self sendEventWithName:kEventPeerConnectionRemovedStream
                     body:@{
                       @"id": peerConnection.reactTag,
                       @"streamId": streamReactTag,
                       @"sdp": newSdp
                     }];
}

- (void)peerConnectionShouldNegotiate:(RTCPeerConnection *)peerConnection {
  [self sendEventWithName:kEventPeerConnectionOnRenegotiationNeeded
                     body:@{ @"id": peerConnection.reactTag }];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didChangeConnectionState:(RTCPeerConnectionState)newState {
  [self sendEventWithName:kEventPeerConnectionStateChanged
                     body:@{@"id": peerConnection.reactTag, @"connectionState": [self stringForPeerConnectionState:newState]}];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didChangeIceConnectionState:(RTCIceConnectionState)newState {
  [self sendEventWithName:kEventPeerConnectionIceConnectionChanged
                     body:@{
                       @"id": peerConnection.reactTag,
                       @"iceConnectionState": [self stringForICEConnectionState:newState]
                     }];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didChangeIceGatheringState:(RTCIceGatheringState)newState {
  id newSdp = @{};
  if (newState == RTCIceGatheringStateComplete) {
      newSdp = @{
          @"type": [RTCSessionDescription stringForType:peerConnection.localDescription.type],
          @"sdp": peerConnection.localDescription.sdp
      };
  }
  [self sendEventWithName:kEventPeerConnectionIceGatheringChanged
                     body:@{
                       @"id": peerConnection.reactTag,
                       @"iceGatheringState": [self stringForICEGatheringState:newState],
                       @"sdp": newSdp
                     }];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didGenerateIceCandidate:(RTCIceCandidate *)candidate {
  [self sendEventWithName:kEventPeerConnectionGotICECandidate
                     body:@{
                       @"id": peerConnection.reactTag,
                       @"candidate": @{
                           @"candidate": candidate.sdp,
                           @"sdpMLineIndex": @(candidate.sdpMLineIndex),
                           @"sdpMid": candidate.sdpMid
                       },
                       @"sdp": @{
                           @"type": [RTCSessionDescription stringForType:peerConnection.localDescription.type],
                           @"sdp": peerConnection.localDescription.sdp
                       }
                     }];
}

- (void)peerConnection:(RTCPeerConnection*)peerConnection didOpenDataChannel:(RTCDataChannel*)dataChannel {
    NSString *reactTag = [[NSUUID UUID] UUIDString];
    DataChannelWrapper *dcw = [[DataChannelWrapper alloc] initWithChannel:dataChannel reactTag:reactTag];
    dcw.pcId = peerConnection.reactTag;
    peerConnection.dataChannels[reactTag] = dcw;
    dcw.delegate = self;

    NSDictionary *dataChannelInfo = @{
        @"peerConnectionId": peerConnection.reactTag,
        @"reactTag": reactTag,
        @"label": dataChannel.label,
        @"id": @(dataChannel.channelId),
        @"ordered": @(dataChannel.isOrdered),
        @"maxPacketLifeTime": @(dataChannel.maxPacketLifeTime),
        @"maxRetransmits": @(dataChannel.maxRetransmits),
        @"protocol": dataChannel.protocol,
        @"negotiated": @(dataChannel.isNegotiated),
        @"readyState": [self stringForDataChannelState:dataChannel.readyState]
      };
    NSDictionary *body = @{
        @"id": peerConnection.reactTag,
        @"dataChannel": dataChannelInfo
    };
    [self sendEventWithName:kEventPeerConnectionDidOpenDataChannel body:body];
}

- (void)peerConnection:(nonnull RTCPeerConnection *)peerConnection didRemoveIceCandidates:(nonnull NSArray<RTCIceCandidate *> *)candidates {
  // TODO
}

@end
