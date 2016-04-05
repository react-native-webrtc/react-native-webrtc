//
//  RTCPeerConnectionDelegator.h
//  TestReact
//
//  Created by one on 2015/9/24.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "RTCSessionDescription.h"
#import "RTCSessionDescriptionDelegate.h"
#import "RTCStatsDelegate.h"

@interface RTCPeerConnectionDelegator : NSObject <RTCSessionDescriptionDelegate, RTCStatsDelegate>

@property (nonatomic, copy) void (^createSessionDescriptionCallback)(RTCSessionDescription *sdp, NSError *error);
@property (nonatomic, copy) void (^setSessionDescriptionCallback)(NSError *error);
@property (nonatomic, copy) void (^getStatsCallback)(NSArray *stats);

@end
