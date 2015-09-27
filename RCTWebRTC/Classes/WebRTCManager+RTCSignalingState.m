//
//  WebRTCManager+RTCSignalingState.m
//  TestReact
//
//  Created by one on 2015/9/24.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import "WebRTCManager+RTCSignalingState.h"

@implementation WebRTCManager (RTCSignalingState)

- (NSString *)stringForSignalingState:(RTCSignalingState)state {
  NSString *signalingState = nil;
  switch (state) {
    case RTCSignalingStable:
      signalingState = @"stable";
      break;
    case RTCSignalingHaveLocalOffer:
      signalingState = @"have-local-offer";
      break;
    case RTCSignalingHaveLocalPrAnswer:
      signalingState = @"have-remote-offer";
      break;
    case RTCSignalingHaveRemoteOffer:
      signalingState = @"have-local-pranswer";
      break;
    case RTCSignalingHaveRemotePrAnswer:
      signalingState = @"have-remote-pranswer";
      break;
    default:
      signalingState = @"closed";
      break;
  }
  return signalingState;
}

@end
