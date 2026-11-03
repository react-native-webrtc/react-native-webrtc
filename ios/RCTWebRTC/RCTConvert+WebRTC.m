#import <LiveKitWebRTC/RTCCertificate.h>
#import <LiveKitWebRTC/RTCDataChannelConfiguration.h>
#import <LiveKitWebRTC/RTCIceServer.h>
#import <LiveKitWebRTC/RTCSessionDescription.h>
#import <React/RCTLog.h>
#import "RCTConvert+WebRTC.h"

#import "WebRTCModule+RTCPeerConnection.h"

@implementation RCTConvert (WebRTC)

+ (LKRTCSessionDescription *)LKRTCSessionDescription:(id)json {
    if (!json) {
        return nil;
    }

    if (![json isKindOfClass:[NSDictionary class]]) {
        RCTLogConvertError(json, @"must be an object");
        return nil;
    }

    if (json[@"sdp"] == nil) {
        RCTLogConvertError(json, @".sdp must not be null");
        return nil;
    }

    NSString *sdp = json[@"sdp"];
    LKRTCSdpType sdpType = [LKRTCSessionDescription typeForString:json[@"type"]];

    return [[LKRTCSessionDescription alloc] initWithType:sdpType sdp:sdp];
}

+ (LKRTCIceCandidate *)LKRTCIceCandidate:(id)json {
    if (!json) {
        RCTLogConvertError(json, @"must not be null");
        return nil;
    }

    if (![json isKindOfClass:[NSDictionary class]]) {
        RCTLogConvertError(json, @"must be an object");
        return nil;
    }

    if (json[@"candidate"] == nil) {
        RCTLogConvertError(json, @".candidate must not be null");
        return nil;
    }

    if (json[@"sdpMid"] == nil && json[@"sdpMLineIndex"] == nil) {
        RCTLogConvertError(json, @".sdpMid and .sdpMLineIndex must not be both null");
        return nil;
    }

    NSString *sdp = json[@"candidate"];
    RCTLogTrace(@"%@ <- candidate", sdp);
    int sdpMLineIndex = [RCTConvert int:json[@"sdpMLineIndex"]];
    NSString *sdpMid = json[@"sdpMid"];

    return [[LKRTCIceCandidate alloc] initWithSdp:sdp sdpMLineIndex:sdpMLineIndex sdpMid:sdpMid];
}

+ (LKRTCIceServer *)LKRTCIceServer:(id)json {
    if (!json) {
        RCTLogConvertError(json, @"a valid iceServer value");
        return nil;
    }

    if (![json isKindOfClass:[NSDictionary class]]) {
        RCTLogConvertError(json, @"must be an object");
        return nil;
    }

    NSArray<NSString *> *urls;
    if ([json[@"urls"] isKindOfClass:[NSString class]]) {
        urls = @[ json[@"urls"] ];
    } else {
        urls = [RCTConvert NSArray:json[@"urls"]];
    }

    if (json[@"username"] != nil || json[@"credential"] != nil) {
        return [[LKRTCIceServer alloc] initWithURLStrings:urls
                                                 username:json[@"username"]
                                               credential:json[@"credential"]];
    }

    return [[LKRTCIceServer alloc] initWithURLStrings:urls];
}

+ (nonnull LKRTCConfiguration *)LKRTCConfiguration:(id)json {
    LKRTCConfiguration *config = [[LKRTCConfiguration alloc] init];
    config.sdpSemantics = LKRTCSdpSemanticsUnifiedPlan;

    // Required for perfect negotiation.
    config.enableImplicitRollback = YES;

    // Enable GCM ciphers.
    LKRTCCryptoOptions *cryptoOptions = [[LKRTCCryptoOptions alloc] initWithSrtpEnableGcmCryptoSuites:YES
                                                                            srtpPreferGcmCryptoSuites:NO
                                                                  srtpEnableAes128Sha1_32CryptoCipher:NO
                                                                  srtpEnableAes128Sha1_80CryptoCipher:YES
                                                               srtpEnableEncryptedRtpHeaderExtensions:NO
                                                                         sframeRequireFrameEncryption:NO];
    config.cryptoOptions = cryptoOptions;

    if (!json) {
        return config;
    }

    if (![json isKindOfClass:[NSDictionary class]]) {
        RCTLogConvertError(json, @"must be an object");
        return config;
    }

    if (json[@"audioJitterBufferMaxPackets"] != nil &&
        [json[@"audioJitterBufferMaxPackets"] isKindOfClass:[NSNumber class]]) {
        config.audioJitterBufferMaxPackets = [RCTConvert int:json[@"audioJitterBufferMaxPackets"]];
    }

    if (json[@"bundlePolicy"] != nil && [json[@"bundlePolicy"] isKindOfClass:[NSString class]]) {
        NSString *bundlePolicy = json[@"bundlePolicy"];
        if ([bundlePolicy isEqualToString:@"balanced"]) {
            config.bundlePolicy = LKRTCBundlePolicyBalanced;
        } else if ([bundlePolicy isEqualToString:@"max-compat"]) {
            config.bundlePolicy = LKRTCBundlePolicyMaxCompat;
        } else if ([bundlePolicy isEqualToString:@"max-bundle"]) {
            config.bundlePolicy = LKRTCBundlePolicyMaxBundle;
        }
    }

    if (json[@"enableSctpSnap"] != nil && [json[@"enableSctpSnap"] isKindOfClass:[NSNumber class]]) {
        config.enableSctpSnap = [RCTConvert BOOL:json[@"enableSctpSnap"]];
    }

    if (json[@"iceBackupCandidatePairPingInterval"] != nil &&
        [json[@"iceBackupCandidatePairPingInterval"] isKindOfClass:[NSNumber class]]) {
        config.iceBackupCandidatePairPingInterval = [RCTConvert int:json[@"iceBackupCandidatePairPingInterval"]];
    }

    if (json[@"iceConnectionReceivingTimeout"] != nil &&
        [json[@"iceConnectionReceivingTimeout"] isKindOfClass:[NSNumber class]]) {
        config.iceConnectionReceivingTimeout = [RCTConvert int:json[@"iceConnectionReceivingTimeout"]];
    }

    if (json[@"iceServers"] != nil && [json[@"iceServers"] isKindOfClass:[NSArray class]]) {
        NSMutableArray<LKRTCIceServer *> *iceServers = [NSMutableArray new];
        for (id server in json[@"iceServers"]) {
            LKRTCIceServer *convert = [RCTConvert LKRTCIceServer:server];
            if (convert != nil) {
                [iceServers addObject:convert];
            }
        }
        config.iceServers = iceServers;
    }

    if (json[@"iceTransportPolicy"] != nil && [json[@"iceTransportPolicy"] isKindOfClass:[NSString class]]) {
        NSString *iceTransportPolicy = json[@"iceTransportPolicy"];
        if ([iceTransportPolicy isEqualToString:@"all"]) {
            config.iceTransportPolicy = LKRTCIceTransportPolicyAll;
        } else if ([iceTransportPolicy isEqualToString:@"none"]) {
            config.iceTransportPolicy = LKRTCIceTransportPolicyNone;
        } else if ([iceTransportPolicy isEqualToString:@"nohost"]) {
            config.iceTransportPolicy = LKRTCIceTransportPolicyNoHost;
        } else if ([iceTransportPolicy isEqualToString:@"relay"]) {
            config.iceTransportPolicy = LKRTCIceTransportPolicyRelay;
        }
    }

    if (json[@"rtcpMuxPolicy"] != nil && [json[@"rtcpMuxPolicy"] isKindOfClass:[NSString class]]) {
        NSString *rtcpMuxPolicy = json[@"rtcpMuxPolicy"];
        if ([rtcpMuxPolicy isEqualToString:@"negotiate"]) {
            config.rtcpMuxPolicy = LKRTCRtcpMuxPolicyNegotiate;
        } else if ([rtcpMuxPolicy isEqualToString:@"require"]) {
            config.rtcpMuxPolicy = LKRTCRtcpMuxPolicyRequire;
        }
    }

    if (json[@"tcpCandidatePolicy"] != nil && [json[@"tcpCandidatePolicy"] isKindOfClass:[NSString class]]) {
        NSString *tcpCandidatePolicy = json[@"tcpCandidatePolicy"];
        if ([tcpCandidatePolicy isEqualToString:@"enabled"]) {
            config.tcpCandidatePolicy = LKRTCTcpCandidatePolicyEnabled;
        } else if ([tcpCandidatePolicy isEqualToString:@"disabled"]) {
            config.tcpCandidatePolicy = LKRTCTcpCandidatePolicyDisabled;
        }
    }

    if (json[@"certificates"] != nil && [json[@"certificates"] isKindOfClass:[NSArray class]]) {
        NSArray *certs = json[@"certificates"];
        if (certs.count > 0) {
            id certInfo = certs[0];
            if ([certInfo isKindOfClass:[NSDictionary class]]) {
                NSString *certId = certInfo[@"certificateId"];
                if (certId) {
                    LKRTCCertificate *cert = [WebRTCModule getCertificate:certId];
                    if (cert) {
                        config.certificate = cert;
                    }
                }
            }
        }
    }

    return config;
}

+ (LKRTCDataChannelConfiguration *)LKRTCDataChannelConfiguration:(id)json {
    if (!json) {
        return nil;
    }
    if ([json isKindOfClass:[NSDictionary class]]) {
        LKRTCDataChannelConfiguration *init = [LKRTCDataChannelConfiguration new];

        if (json[@"id"]) {
            [init setChannelId:[RCTConvert int:json[@"id"]]];
        }

        if (json[@"ordered"]) {
            init.isOrdered = [RCTConvert BOOL:json[@"ordered"]];
        }
        if (json[@"maxRetransmits"]) {
            init.maxRetransmits = [RCTConvert int:json[@"maxRetransmits"]];
        }
        if (json[@"negotiated"]) {
            init.isNegotiated = [RCTConvert NSInteger:json[@"negotiated"]];
        }
        if (json[@"protocol"]) {
            init.protocol = [RCTConvert NSString:json[@"protocol"]];
        }
        return init;
    }
    return nil;
}

@end
