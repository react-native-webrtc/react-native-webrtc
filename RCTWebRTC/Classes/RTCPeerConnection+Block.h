//
//  RTCPeerConnection+Block.h
//  TestReact
//
//  Created by one on 2015/9/24.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import "RTCPeerConnection.h"
#import "RTCSessionDescription.h"

@interface RTCPeerConnection (Block)

- (void)createOfferWithCallback:(void (^)(RTCSessionDescription *sdp, NSError *error))callback constraints:(RTCMediaConstraints *)constraints;
- (void)createAnswerWithCallback:(void (^)(RTCSessionDescription *sdp, NSError *error))callback constraints:(RTCMediaConstraints *)constraints;
- (void)setLocalDescriptionWithCallback:(void (^)(NSError *error))callback sessionDescription:(RTCSessionDescription *)sdp;
- (void)setRemoteDescriptionWithCallback:(void (^)(NSError *error))callback sessionDescription:(RTCSessionDescription *)sdp;

@end
