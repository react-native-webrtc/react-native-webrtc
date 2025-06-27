#import <objc/runtime.h>

#import <React/RCTBridge.h>
#import <React/RCTBridgeModule.h>

#import <WebRTC/RTCRtpCodecCapability.h>
#import <WebRTC/RTCRtpReceiver.h>
#import <WebRTC/RTCRtpSender.h>

#import "SerializeUtils.h"
#import "WebRTCModule.h"

@implementation WebRTCModule (Transceivers)

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(senderGetCapabilities : (NSString *)kind) {
    __block id params;

    dispatch_sync(self.workerQueue, ^{
        RTCRtpCapabilities *capabilities = [self.peerConnectionFactory rtpSenderCapabilitiesForKind:kind];
        params = [SerializeUtils capabilitiesToJSON:capabilities];
    });

    return params;
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(receiverGetCapabilities : (NSString *)kind) {
    __block id params;

    dispatch_sync(self.workerQueue, ^{
        RTCRtpCapabilities *capabilities = [self.peerConnectionFactory rtpReceiverCapabilitiesForKind:kind];
        params = [SerializeUtils capabilitiesToJSON:capabilities];
    });

    return params;
}

RCT_EXPORT_METHOD(senderReplaceTrack : (nonnull NSNumber *)objectID senderId : (NSString *)senderId trackId : (
    NSString *)trackId resolver : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
    RTCPeerConnection *peerConnection = self.peerConnections[objectID];

    if (peerConnection == nil) {
        RCTLogWarn(@"PeerConnection %@ not found in senderReplaceTrack()", objectID);
        reject(@"E_INVALID", @"Peer Connection is not initialized", nil);
    }

    RTCRtpTransceiver *transceiver = nil;
    for (RTCRtpTransceiver *t in peerConnection.transceivers) {
        if ([senderId isEqual:t.sender.senderId]) {
            transceiver = t;
            break;
        }
    }

    if (transceiver == nil) {
        RCTLogWarn(@"senderReplaceTrack() transceiver is null");
        reject(@"E_INVALID", @"Could not get transceive", nil);
    }

    RTCRtpSender *sender = transceiver.sender;
    RTCMediaStreamTrack *track = self.localTracks[trackId];
    [sender setTrack:track];
    resolve(@true);
}

RCT_EXPORT_METHOD(senderSetParameters : (nonnull NSNumber *)objectID senderId : (NSString *)senderId options : (
    NSDictionary *)options resolver : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
    RTCPeerConnection *peerConnection = self.peerConnections[objectID];

    if (peerConnection == nil) {
        RCTLogWarn(@"PeerConnection %@ not found in senderSetParameters()", objectID);
        reject(@"E_INVALID", @"Peer Connection is not initialized", nil);
        return;
    }

    RTCRtpTransceiver *transceiver = nil;
    for (RTCRtpTransceiver *t in peerConnection.transceivers) {
        if ([senderId isEqual:t.sender.senderId]) {
            transceiver = t;
            break;
        }
    }

    if (transceiver == nil) {
        RCTLogWarn(@"senderSetParameters() transceiver is null");
        reject(@"E_INVALID", @"Could not get transceiver", nil);
        return;
    }

    RTCRtpSender *sender = transceiver.sender;
    RTCRtpParameters *parameters = sender.parameters;
    [sender setParameters:[self updateParametersWithOptions:options params:parameters]];

    resolve([SerializeUtils parametersToJSON:sender.parameters]);
}

RCT_EXPORT_METHOD(transceiverSetDirection : (nonnull NSNumber *)objectID senderId : (NSString *)senderId direction : (
    NSString *)direction resolver : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
    RTCPeerConnection *peerConnection = self.peerConnections[objectID];

    if (peerConnection == nil) {
        RCTLogWarn(@"transceiverSetDirection() PeerConnection %@ not found in transceiverSetDirection()", objectID);
        reject(@"E_INVALID", @"Peer Connection is not initialized", nil);
        return;
    }

    RTCRtpTransceiver *transceiver = nil;
    for (RTCRtpTransceiver *t in peerConnection.transceivers) {
        if ([senderId isEqual:t.sender.senderId]) {
            transceiver = t;
            break;
        }
    }

    if (transceiver == nil) {
        RCTLogWarn(@"transceiverSetDirection() transceiver is null");
        reject(@"E_INVALID", @"Could not get transceiver", nil);
        return;
    }

    NSError *error;
    [transceiver setDirection:[SerializeUtils parseDirection:direction] error:&error];

    if (error) {
        reject(@"E_SET_DIRECTION", @"Could not set direction", error);
    } else {
        resolve(@true);
    }
}

RCT_EXPORT_METHOD(transceiverStop : (nonnull NSNumber *)objectID senderId : (NSString *)
                      senderId resolver : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
    RTCPeerConnection *peerConnection = self.peerConnections[objectID];

    if (peerConnection == nil) {
        RCTLogWarn(@"PeerConnection %@ not found in transceiverStop()", objectID);
        reject(@"E_INVALID", @"Peer Connection is not initialized", nil);
        return;
    }

    RTCRtpTransceiver *transceiver = nil;
    for (RTCRtpTransceiver *t in peerConnection.transceivers) {
        if ([senderId isEqual:t.sender.senderId]) {
            transceiver = t;
            break;
        }
    }

    if (transceiver == nil) {
        RCTLogWarn(@"senderSetParameters() transceiver is null");
        reject(@"E_INVALID", @"Could not get transceiver", nil);
        return;
    }

    [transceiver stopInternal];

    resolve(@true);
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(transceiverSetCodecPreferences : (nonnull NSNumber *)objectID senderId : (
    NSString *)senderId codecPreferences : (NSArray *)codecPreferences) {
    RTCPeerConnection *peerConnection = self.peerConnections[objectID];

    if (peerConnection == nil) {
        RCTLogWarn(@"PeerConnection %@ not found in transceiverSetCodecPreferences()", objectID);
        return nil;
    }

    RTCRtpTransceiver *transceiver = nil;
    for (RTCRtpTransceiver *t in peerConnection.transceivers) {
        if ([senderId isEqual:t.sender.senderId]) {
            transceiver = t;
            break;
        }
    }

    if (transceiver == nil) {
        RCTLogWarn(@"transceiverSetCodecPreferences() transceiver is null");
        return nil;
    }

    // Get the available codecs
    RTCRtpTransceiverDirection direction = transceiver.direction;
    NSMutableArray *availableCodecs = [NSMutableArray new];
    NSString *kind = transceiver.mediaType == RTCRtpMediaTypeAudio ? @"audio" : @"video";
    if (direction == RTCRtpTransceiverDirectionSendRecv || direction == RTCRtpTransceiverDirectionSendOnly) {
        RTCRtpCapabilities *capabilities = [self.peerConnectionFactory rtpSenderCapabilitiesForKind:kind];
        for (RTCRtpCodecCapability *codec in capabilities.codecs) {
            NSDictionary *codecDict = [SerializeUtils codecCapabilityToJSON:codec];
            [availableCodecs addObject:@{
                @"dict" : codecDict,
                @"codec" : codec,
            }];
        }
    }
    if (direction == RTCRtpTransceiverDirectionSendRecv || direction == RTCRtpTransceiverDirectionRecvOnly) {
        RTCRtpCapabilities *capabilities = [self.peerConnectionFactory rtpReceiverCapabilitiesForKind:kind];
        for (RTCRtpCodecCapability *codec in capabilities.codecs) {
            NSDictionary *codecDict = [SerializeUtils codecCapabilityToJSON:codec];
            [availableCodecs addObject:@{
                @"dict" : codecDict,
                @"codec" : codec,
            }];
        }
    }

    // Convert JSON codec capabilities to the actual objects.
    // Codec preferences is order sensitive.
    NSMutableArray *codecsToSet = [NSMutableArray new];

    for (NSDictionary *codecDict in codecPreferences) {
        for (NSDictionary *entry in availableCodecs) {
            NSDictionary *availableCodecDict = [entry objectForKey:@"dict"];
            if ([codecDict isEqualToDictionary:availableCodecDict]) {
                [codecsToSet addObject:[entry objectForKey:@"codec"]];
                break;
            }
        }
    }

    [transceiver setCodecPreferences:codecsToSet];

    return nil;
}

- (RTCRtpParameters *)updateParametersWithOptions:(NSDictionary *)options params:(RTCRtpParameters *)params {
    NSArray *encodingsArray = options[@"encodings"];
    NSArray *encodings = params.encodings;

    if ([encodingsArray count] != [encodings count]) {
        return nil;
    }

    for (int i = 0; i < [encodingsArray count]; i++) {
        NSDictionary *encodingUpdate = encodingsArray[i];
        RTCRtpEncodingParameters *encoding = encodings[i];

        encoding.isActive = [encodingUpdate[@"active"] boolValue];
        encoding.rid = encodingUpdate[@"rid"];
        encoding.maxBitrateBps = encodingUpdate[@"maxBitrate"];
        encoding.maxFramerate = encodingUpdate[@"maxFramerate"];
        encoding.scaleResolutionDownBy = encodingUpdate[@"scaleResolutionDownBy"];
    }

    if ([options objectForKey:@"degradationPreference"]) {
        params.degradationPreference = [options objectForKey:@"degradationPreference"];
    }

    return params;
}

@end
