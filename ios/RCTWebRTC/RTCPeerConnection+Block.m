//
//  RTCPeerConnection+Block.m
//  TestReact
//
//  Created by one on 2015/9/24.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import "RTCPeerConnection+Block.h"
#import "RTCPeerConnectionDelegator.h"

@implementation RTCPeerConnection (Block)

- (void)createOfferWithCallback:(void (^)(RTCSessionDescription *sdp, NSError *error))callback constraints:(RTCMediaConstraints *)constraints {
  dispatch_async(dispatch_get_main_queue(), ^{
    RTCPeerConnectionDelegator *delegator = [RTCPeerConnectionDelegator new];
    delegator.createSessionDescriptionCallback = callback;
    [self createOfferWithDelegate:delegator constraints:constraints];
  });
}
- (void)createAnswerWithCallback:(void (^)(RTCSessionDescription *sdp, NSError *error))callback constraints:(RTCMediaConstraints *)constraints {
  RTCPeerConnectionDelegator *delegator = [RTCPeerConnectionDelegator new];
  delegator.createSessionDescriptionCallback = callback;
  [self createAnswerWithDelegate:delegator constraints:constraints];
}

- (void)setLocalDescriptionWithCallback:(void (^)(NSError *error))callback sessionDescription:(RTCSessionDescription *)sdp {
  RTCPeerConnectionDelegator *delegator = [RTCPeerConnectionDelegator new];
  delegator.setSessionDescriptionCallback = callback;
  [self setLocalDescriptionWithDelegate:delegator sessionDescription:sdp];
}

- (void)setRemoteDescriptionWithCallback:(void (^)(NSError *error))callback sessionDescription:(RTCSessionDescription *)sdp {
  RTCPeerConnectionDelegator *delegator = [RTCPeerConnectionDelegator new];
  delegator.setSessionDescriptionCallback = callback;
  [self setRemoteDescriptionWithDelegate:delegator sessionDescription:sdp];
}

- (BOOL)getStatsWithCallback:(void (^)(NSArray *stats))callback mediaStreamTrack:(RTCMediaStreamTrack*)mediaStreamTrack statsOutputLevel:(RTCStatsOutputLevel)statsOutputLevel {
  RTCPeerConnectionDelegator *delegator = [RTCPeerConnectionDelegator new];
  delegator.getStatsCallback = callback;
  return [self getStatsWithDelegate:delegator mediaStreamTrack:mediaStreamTrack statsOutputLevel:statsOutputLevel];
}
@end
