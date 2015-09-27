//
//  WebRTCManager+RTCPeerConnection.h
//  TestReact
//
//  Created by one on 2015/9/24.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import "WebRTCManager.h"
#import "RTCPeerConnection.h"

@interface RTCPeerConnection (React)

@property (nonatomic, strong) NSNumber *reactTag;

@end

@interface WebRTCManager (RTCPeerConnection) <RTCPeerConnectionDelegate>

@end
