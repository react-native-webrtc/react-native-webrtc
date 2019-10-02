#import "RCTConvert+WebRTC.h"
#import <React/RCTLog.h>
#import <WebRTC/RTCDataChannelConfiguration.h>
#import <WebRTC/RTCIceServer.h>
#import <WebRTC/RTCSessionDescription.h>

@implementation RCTConvert (WebRTC)

+ (RTCSessionDescription *)RTCSessionDescription:(id)json
{
  if (!json) {
    RCTLogConvertError(json, @"must not be null");
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
  RTCSdpType sdpType = [RTCSessionDescription typeForString:json[@"type"]];

  return [[RTCSessionDescription alloc] initWithType:sdpType sdp:sdp];
}

+ (RTCIceCandidate *)RTCIceCandidate:(id)json
{
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

  NSString *sdp = json[@"candidate"];
  RCTLogTrace(@"%@ <- candidate", sdp);
  int sdpMLineIndex = [RCTConvert int:json[@"sdpMLineIndex"]];
  NSString *sdpMid = json[@"sdpMid"];


  return [[RTCIceCandidate alloc] initWithSdp:sdp sdpMLineIndex:sdpMLineIndex sdpMid:sdpMid];
}

+ (RTCIceServer *)RTCIceServer:(id)json
{
  if (!json) {
    RCTLogConvertError(json, @"a valid iceServer value");
    return nil;
  }

  if (![json isKindOfClass:[NSDictionary class]]) {
    RCTLogConvertError(json, @"must be an object");
    return nil;
  }

  NSArray<NSString *> *urls;
  if ([json[@"url"] isKindOfClass:[NSString class]]) {
    // TODO: 'url' is non-standard
    urls = @[json[@"url"]];
  } else if ([json[@"urls"] isKindOfClass:[NSString class]]) {
    urls = @[json[@"urls"]];
  } else {
    urls = [RCTConvert NSArray:json[@"urls"]];
  }

  if (json[@"username"] != nil || json[@"credential"] != nil) {
    return [[RTCIceServer alloc]initWithURLStrings:urls
                                        username:json[@"username"]
                                        credential:json[@"credential"]];
  }

  return [[RTCIceServer alloc] initWithURLStrings:urls];
}

+ (nonnull RTCConfiguration *)RTCConfiguration:(id)json
{
  RTCConfiguration *config = [[RTCConfiguration alloc] init];

  if (!json) {
    return config;
  }

  if (![json isKindOfClass:[NSDictionary class]]) {
    RCTLogConvertError(json, @"must be an object");
    return config;
  }

  if (json[@"audioJitterBufferMaxPackets"] != nil && [json[@"audioJitterBufferMaxPackets"] isKindOfClass:[NSNumber class]]) {
    config.audioJitterBufferMaxPackets = [RCTConvert int:json[@"audioJitterBufferMaxPackets"]];
  }

  if (json[@"bundlePolicy"] != nil && [json[@"bundlePolicy"] isKindOfClass:[NSString class]]) {
    NSString *bundlePolicy = json[@"bundlePolicy"];
    if ([bundlePolicy isEqualToString:@"balanced"]) {
      config.bundlePolicy = RTCBundlePolicyBalanced;
    } else if ([bundlePolicy isEqualToString:@"max-compat"]) {
      config.bundlePolicy = RTCBundlePolicyMaxCompat;
    } else if ([bundlePolicy isEqualToString:@"max-bundle"]) {
      config.bundlePolicy = RTCBundlePolicyMaxBundle;
    }
  }

  if (json[@"iceBackupCandidatePairPingInterval"] != nil && [json[@"iceBackupCandidatePairPingInterval"] isKindOfClass:[NSNumber class]]) {
    config.iceBackupCandidatePairPingInterval = [RCTConvert int:json[@"iceBackupCandidatePairPingInterval"]];
  }

  if (json[@"iceConnectionReceivingTimeout"] != nil && [json[@"iceConnectionReceivingTimeout"] isKindOfClass:[NSNumber class]]) {
    config.iceConnectionReceivingTimeout = [RCTConvert int:json[@"iceConnectionReceivingTimeout"]];
  }

  if (json[@"iceServers"] != nil && [json[@"iceServers"] isKindOfClass:[NSArray class]]) {
    NSMutableArray<RTCIceServer *> *iceServers = [NSMutableArray new];
    for (id server in json[@"iceServers"]) {
      RTCIceServer *convert = [RCTConvert RTCIceServer:server];
      if (convert != nil) {
        [iceServers addObject:convert];
      }
    }
    config.iceServers = iceServers;
  }

  if (json[@"iceTransportPolicy"] != nil && [json[@"iceTransportPolicy"] isKindOfClass:[NSString class]]) {
    NSString *iceTransportPolicy = json[@"iceTransportPolicy"];
    if ([iceTransportPolicy isEqualToString:@"all"]) {
      config.iceTransportPolicy = RTCIceTransportPolicyAll;
    } else if ([iceTransportPolicy isEqualToString:@"none"]) {
      config.iceTransportPolicy = RTCIceTransportPolicyNone;
    } else if ([iceTransportPolicy isEqualToString:@"nohost"]) {
      config.iceTransportPolicy = RTCIceTransportPolicyNoHost;
    } else if ([iceTransportPolicy isEqualToString:@"relay"]) {
      config.iceTransportPolicy = RTCIceTransportPolicyRelay;
    }
  }

  if (json[@"rtcpMuxPolicy"] != nil && [json[@"rtcpMuxPolicy"] isKindOfClass:[NSString class]]) {
    NSString *rtcpMuxPolicy = json[@"rtcpMuxPolicy"];
    if ([rtcpMuxPolicy isEqualToString:@"negotiate"]) {
      config.rtcpMuxPolicy = RTCRtcpMuxPolicyNegotiate;
    } else if ([rtcpMuxPolicy isEqualToString:@"require"]) {
      config.rtcpMuxPolicy = RTCRtcpMuxPolicyRequire;
    }
  }

  if (json[@"tcpCandidatePolicy"] != nil && [json[@"tcpCandidatePolicy"] isKindOfClass:[NSString class]]) {
    NSString *tcpCandidatePolicy = json[@"tcpCandidatePolicy"];
    if ([tcpCandidatePolicy isEqualToString:@"enabled"]) {
      config.tcpCandidatePolicy = RTCTcpCandidatePolicyEnabled;
    } else if ([tcpCandidatePolicy isEqualToString:@"disabled"]) {
      config.tcpCandidatePolicy = RTCTcpCandidatePolicyDisabled;
    }
  }

  return config;
}

+ (RTCDataChannelConfiguration *)RTCDataChannelConfiguration:(id)json
{
  if (!json) {
    return nil;
  }
  if ([json isKindOfClass:[NSDictionary class]]) {
    RTCDataChannelConfiguration *init = [RTCDataChannelConfiguration new];

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
