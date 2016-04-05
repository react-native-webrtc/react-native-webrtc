//
//  RTCPeerConnectionDelegator.m
//  TestReact
//
//  Created by one on 2015/9/24.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import "RTCPeerConnectionDelegator.h"

@implementation RTCPeerConnectionDelegator

- (void)peerConnection:(RTCPeerConnection *)peerConnection didCreateSessionDescription:(RTCSessionDescription *)sdp error:(NSError *)error {
  self.createSessionDescriptionCallback(sdp, error);
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didSetSessionDescriptionWithError:(NSError *)error {
  self.setSessionDescriptionCallback(error);
}

- (void)peerConnection:(RTCPeerConnection*)peerConnection didGetStats:(NSArray*)stats {
  self.getStatsCallback(stats);
}

@end
