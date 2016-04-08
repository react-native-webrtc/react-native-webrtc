//
//  WebRTCModule+RTCSignalingState.h
//
//  Created by one on 2015/9/24.
//  Copyright © 2015 One. All rights reserved.
//

#import "WebRTCModule.h"

@interface WebRTCModule (RTCSignalingState)

- (NSString *)stringForSignalingState:(RTCSignalingState)state;

@end
