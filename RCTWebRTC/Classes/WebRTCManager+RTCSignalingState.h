//
//  WebRTCManager+RTCSignalingState.h
//  TestReact
//
//  Created by one on 2015/9/24.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import "WebRTCManager.h"

@interface WebRTCManager (RTCSignalingState)

- (NSString *)stringForSignalingState:(RTCSignalingState)state;

@end
