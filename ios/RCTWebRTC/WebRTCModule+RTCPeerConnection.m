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
#import <WebRTC/RTCMediaStreamTrack.h>
#import <WebRTC/RTCRtpReceiver.h>
#import <WebRTC/RTCRtpTransceiver.h>

#import "WebRTCModule.h"
#import "WebRTCModule+RTCDataChannel.h"
#import "WebRTCModule+RTCPeerConnection.h"
#import "WebRTCModule+VideoTrackAdapter.h"
#import "SerializeUtils.h"

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

 int _transceiverNextId = 0;

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
          callback(@[@(YES), @{
                               @"sdpInfo": @{
                                 @"sdp": sdp.sdp,
                                 @"type": type
                               },
                               @"transceiversInfo": [SerializeUtils constructTransceiversInfoArrayWithPeerConnection:peerConnection peerConnectionId: objectID]
          }]);
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
           callback(@[@(YES), @{
                                @"sdpInfo": @{@"sdp": sdp.sdp,
                                              @"type": type},
                                @"transceiversInfo": [SerializeUtils constructTransceiversInfoArrayWithPeerConnection:peerConnection peerConnectionId: objectID]
           }]);
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

            @"sdpInfo": @{@"type": [RTCSessionDescription stringForType:strongPc.localDescription.type],
                          @"sdp": strongPc.localDescription.sdp
            },
            @"transceiversInfo": [SerializeUtils constructTransceiversInfoArrayWithPeerConnection:peerConnection peerConnectionId: objectID]
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

  NSMutableArray *receiversIds = [NSMutableArray new];
  for (RTCRtpTransceiver *transceiver in peerConnection.transceivers) {
      [receiversIds addObject:transceiver.receiver.receiverId];
  }
  
  [peerConnection setRemoteDescription: sdp completionHandler: ^(NSError *error) {
      if (error) {
        id errorResponse = @{
          @"name": @"SetRemoteDescriptionFailed",
          @"message": error.localizedDescription ?: [NSNull null]
        };
        callback(@[@(NO), errorResponse]);
      } else {
        NSMutableArray *newTransceivers = [NSMutableArray new];
        for (RTCRtpTransceiver *transceiver in peerConnection.transceivers) {
            if (![receiversIds containsObject:transceiver.receiver.receiverId]) {
                NSMutableDictionary *newTransceiver = [NSMutableDictionary new];
                newTransceiver[@"transceiverOrder"] = [NSNumber numberWithInt:_transceiverNextId++];
                newTransceiver[@"transceiver"] = [SerializeUtils transceiverToJSONWithPeerConnectionId:objectID transceiver:transceiver];
                [newTransceivers addObject:newTransceiver];
            }
        }
          
        id newSdp = @{
            @"sdpInfo": @{
                          @"type": [RTCSessionDescription stringForType:peerConnection.remoteDescription.type],
                          @"sdp": peerConnection.remoteDescription.sdp},
            @"transceiversInfo": [SerializeUtils constructTransceiversInfoArrayWithPeerConnection:peerConnection peerConnectionId: objectID],
            @"newTransceivers": newTransceivers
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
  for (NSString *key in peerConnection.remoteTracks.allKeys) {
      RTCMediaStreamTrack *track = peerConnection.remoteTracks[key];
      if (track.kind == kRTCMediaStreamTrackKindVideo) {
        [peerConnection removeVideoTrackAdapter: (RTCVideoTrack*) track];
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

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(peerConnectionAddTrack:(nonnull NSNumber *)objectID
                                                      trackId:(NSString *)trackId
                                                      options:(NSDictionary *)options)
{
   __block id params = nil;

    dispatch_sync(self.workerQueue, ^{
        RTCPeerConnection *peerConnection = self.peerConnections[objectID];
        if (!peerConnection) {
          RCTLogWarn(@"PeerConnection %@ not found in peerConnectionAddTrack()", objectID);
          return;
        }

        RTCMediaStreamTrack *track = self.localTracks[trackId];

        NSArray *streamIds = [options objectForKey:@"streamIds"];
        RTCRtpSender *sender = [peerConnection addTrack: track streamIds: streamIds];
        RTCRtpTransceiver *transceiver = nil;
        
        for (RTCRtpTransceiver *t in peerConnection.transceivers) {
          if([t.sender.senderId isEqual: sender.senderId]) {
            transceiver = t;
            break;
          }
        }
        
        if (!transceiver) {
            RCTLogWarn(@"Transceiver not found in peerConnectionAddTrack()");
            return;
        }
        
        params = @{
            @"transceiverOrder": [NSNumber numberWithInt:_transceiverNextId++],
            @"transceiver": [SerializeUtils transceiverToJSONWithPeerConnectionId: objectID transceiver: transceiver],
            @"sender": [SerializeUtils senderToJSONWithPeerConnectionId: objectID sender: sender]
        };
    });

    return params;
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(peerConnectionAddTransceiver:(nonnull NSNumber *)objectID
                                                            options:(NSDictionary *)options)
{
    __block id params = nil;

    dispatch_sync(self.workerQueue, ^{
        RTCPeerConnection *peerConnection = self.peerConnections[objectID];
        if (!peerConnection) {
          RCTLogWarn(@"PeerConnection %@ not found in peerConnectionAddTransceiver()", objectID);
          return;
        }

        RTCRtpTransceiver *transceiver = nil;
        NSString *kind = [options objectForKey:@"type"];
        NSString *trackId = [options objectForKey:@"trackId"];
        RTCRtpMediaType type = RTCRtpMediaTypeUnsupported;
        
        if (kind) {
            if ([kind  isEqual: @"audio"]) {
                type = RTCRtpMediaTypeAudio;
            } else if ([kind  isEqual: @"video"]) {
                type = RTCRtpMediaTypeVideo;
            }
            transceiver = [peerConnection addTransceiverOfType:type];
        } else if (trackId) {
            RTCMediaStreamTrack *track = self.localTracks[trackId];
            
            if(!track) {
                track = peerConnection.remoteTracks[trackId];
            }
            
            NSDictionary *initOptions = [options objectForKey:@"init"];
            RTCRtpTransceiverInit *transceiverInit = [RTCRtpTransceiverInit new];
           
            if (initOptions) {
                NSString *dirrection = [initOptions objectForKey:@"direction"];
                if ([dirrection isEqual: @"sendrecv"]) {
                    [transceiverInit setDirection:RTCRtpTransceiverDirectionSendRecv];
                } else if ([dirrection isEqual: @"sendonly"]) {
                     [transceiverInit setDirection:RTCRtpTransceiverDirectionSendOnly];
                } else if ([dirrection isEqual: @"recvonly"]) {
                     [transceiverInit setDirection:RTCRtpTransceiverDirectionRecvOnly];
                } else if ([dirrection isEqual: @"inactive"]) {
                     [transceiverInit setDirection:RTCRtpTransceiverDirectionInactive];
                }
                
                NSArray *streamIds = [initOptions objectForKey:@"streamIds"];
                if (streamIds) {
                    [transceiverInit setStreamIds:streamIds];
                }

                NSArray *encodingsArray = [initOptions objectForKey:@"sendEncodings"];
                if (encodingsArray) {
                    NSMutableArray<RTCRtpEncodingParameters *> *sendEncodings = [NSMutableArray new];
                    for (NSDictionary* encoding in encodingsArray) {
                        [sendEncodings addObject:[self parseEncoding:encoding]];
                    }
                    [transceiverInit setSendEncodings:sendEncodings];
                }
            }

            transceiver = [peerConnection addTransceiverWithTrack:track init:transceiverInit];
        } else {
            RCTLogWarn(@"peerConnectionAddTransceiver() no type nor trackId provided in options");
            return;
        }
        
        if (transceiver == nil) {
            RCTLogWarn(@"peerConnectionAddTransceiver() Error adding transceiver");
            return;
        }
        
        params = @{
           @"transceiverOrder": [NSNumber numberWithInt:_transceiverNextId++],
           @"transceiver": [SerializeUtils transceiverToJSONWithPeerConnectionId: objectID transceiver: transceiver ]
        };
    });

    return params;
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(peerConnectionRemoveTrack:(nonnull NSNumber *)objectID
                                                        senderId:(nonnull NSString *)senderId)
{
    __block BOOL ret = NO;

    dispatch_sync(self.workerQueue, ^{
        RTCPeerConnection *peerConnection = self.peerConnections[objectID];
        if (!peerConnection) {
          RCTLogWarn(@"PeerConnection %@ not found in peerConnectionRemoveTrack()", objectID);
          return;
        }

        RTCRtpSender *sender = nil;

        for (RTCRtpSender *s in peerConnection.senders) {
            if ([s.senderId isEqual: senderId]) {
                sender = s;
                break;
            }
        }

        if (!sender) {
            RCTLogWarn(@"Sender not found in peerConnectionRemoveTrack()");
            return;
        }

        ret = [peerConnection removeTrack: sender];
    });

    return @(ret);
}

// TODO: move these below to some SerializeUrils file

- (RTCRtpEncodingParameters*)parseEncoding:(NSDictionary*)params
{
    RTCRtpEncodingParameters *encoding = [RTCRtpEncodingParameters new];

    if (params[@"rid"] != nil) {
      [encoding setRid:params[@"rid"]];
    }
    if (params[@"active"] != nil) {
      [encoding setIsActive:((NSNumber*)params[@"active"]).boolValue];
    }
    if (params[@"maxBitrate"] != nil) {
      [encoding setMaxBitrateBps:(NSNumber*)params[@"maxBitrate"]];
    }
    if (params[@"maxFramerate"] != nil) {
      [encoding setMaxFramerate:(NSNumber*)params[@"maxFramerate"]];
    }
    if (params[@"scaleResolutionDownBy"] != nil) {
      [encoding setScaleResolutionDownBy:(NSNumber*)params[@"scaleResolutionDownBy"]];
    }

    return encoding;
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
    } else if ([statisticsValue isKindOfClass:[NSNumber class]]) {
        [s appendString:[NSString stringWithFormat:@"%@", statisticsValue]];
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
                       @"pcId": peerConnection.reactTag,
                       @"signalingState": [self stringForSignalingState:newState]
                     }];
}

- (void)peerConnectionShouldNegotiate:(RTCPeerConnection *)peerConnection {
  [self sendEventWithName:kEventPeerConnectionOnRenegotiationNeeded
                     body:@{ @"pcId": peerConnection.reactTag }];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didChangeConnectionState:(RTCPeerConnectionState)newState {
  [self sendEventWithName:kEventPeerConnectionStateChanged
                     body:@{@"pcId": peerConnection.reactTag, @"connectionState": [self stringForPeerConnectionState:newState]}];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didChangeIceConnectionState:(RTCIceConnectionState)newState {
  [self sendEventWithName:kEventPeerConnectionIceConnectionChanged
                     body:@{
                       @"pcId": peerConnection.reactTag,
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
                       @"pcId": peerConnection.reactTag,
                       @"iceGatheringState": [self stringForICEGatheringState:newState],
                       @"sdp": newSdp
                     }];
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didGenerateIceCandidate:(RTCIceCandidate *)candidate {
  [self sendEventWithName:kEventPeerConnectionGotICECandidate
                     body:@{
                       @"pcId": peerConnection.reactTag,
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
        @"pcId": peerConnection.reactTag,
        @"dataChannel": dataChannelInfo
    };
    [self sendEventWithName:kEventPeerConnectionDidOpenDataChannel body:body];
}

- (void)peerConnection:(RTC_OBJC_TYPE(RTCPeerConnection) *)peerConnection didAddReceiver:(RTC_OBJC_TYPE(RTCRtpReceiver) *)rtpReceiver
               streams:(NSArray<RTC_OBJC_TYPE(RTCMediaStream) *> *)mediaStreams {
    dispatch_async(self.workerQueue, ^{
        RTCRtpTransceiver *transceiver = nil;
        for (RTCRtpTransceiver *t in peerConnection.transceivers) {
            if ([rtpReceiver.receiverId isEqual: t.receiver.receiverId]) {
                transceiver = t;
                break;
            }
        }
        
        if (!transceiver) {
            RCTLogWarn(@"Transceiver not found in didAddReceiver()");
            return;
        }
        
        RTCMediaStreamTrack *track = rtpReceiver.track;
        
        // We need to fire this event for an existing track sometimes, like
        // when the transceiver direction (on the sending side) switches from
        // sendrecv to recvonly and then back.
        BOOL existingTrack = peerConnection.remoteTracks[track.trackId] != nil;

        if (!existingTrack) {
            if (track.kind == kRTCMediaStreamTrackKindVideo) {
                RTCVideoTrack *videoTrack = (RTCVideoTrack *)track;
                [peerConnection addVideoTrackAdapter: videoTrack];
            }
            
            peerConnection.remoteTracks[track.trackId] = track;
        }
        
        NSMutableDictionary *params = [NSMutableDictionary new];
        NSMutableArray *streams = [NSMutableArray new];
        
        for (RTCMediaStream * stream in mediaStreams) {
            NSString *streamReactTag = nil;

            for (NSString * key in [peerConnection.remoteStreams allKeys]) {
                if ([[peerConnection.remoteStreams objectForKey:key] isEqual:stream]) {
                    streamReactTag = key;
                    break;
                }
            }
            
            if (!peerConnection.remoteStreams[stream.streamId]) {
                peerConnection.remoteStreams[stream.streamId] = stream;
            }
            
            if (!streamReactTag) {
                NSUUID *uuid = [NSUUID UUID];
                streamReactTag = [uuid UUIDString];
                peerConnection.remoteStreams[streamReactTag] = stream;
            }
            
            [streams addObject:[SerializeUtils streamToJSONWithPeerConnectionId:peerConnection.reactTag stream:stream streamReactTag:streamReactTag]];
        }
        
        params[@"streams"] = streams;
        params[@"receiver"] = [SerializeUtils receiverToJSONWithPeerConnectionId: peerConnection.reactTag receiver: rtpReceiver];
        params[@"transceiverOrder"] = [NSNumber numberWithInt:_transceiverNextId++];
        params[@"transceiver"] = [SerializeUtils transceiverToJSONWithPeerConnectionId: peerConnection.reactTag transceiver: transceiver];
        params[@"pcId"] = peerConnection.reactTag;
        
        [self sendEventWithName: kEventPeerConnectionOnTrack body: params];
    });
}

- (void)peerConnection:(RTC_OBJC_TYPE(RTCPeerConnection) *)peerConnection
     didRemoveReceiver:(RTC_OBJC_TYPE(RTCRtpReceiver) *)rtpReceiver {
    dispatch_async(self.workerQueue, ^{
        RTCMediaStreamTrack *track = rtpReceiver.track;

        NSMutableDictionary *params = [NSMutableDictionary new];

        params[@"pcId"] = peerConnection.reactTag;
        params[@"trackId"] = track.trackId;

        [self sendEventWithName: kEventPeerConnectionOnRemoveTrack body: params];
    });
}

- (void)peerConnection:(nonnull RTCPeerConnection *)peerConnection didRemoveIceCandidates:(nonnull NSArray<RTCIceCandidate *> *)candidates {
    // Unimplemented, there is no matching web API.
}

- (void)peerConnection:(nonnull RTCPeerConnection *)peerConnection didAddStream:(nonnull RTCMediaStream *)stream {
    // Unused in Unified Plan.
}

- (void)peerConnection:(nonnull RTCPeerConnection *)peerConnection didRemoveStream:(nonnull RTCMediaStream *)stream {
    // Unused in Unified Plan.
}

@end
