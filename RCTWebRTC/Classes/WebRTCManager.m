//
//  WebRTCManager.m
//  TestReact
//
//  Created by one on 2015/9/24.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import "WebRTCManager.h"

#import "RCTBridge.h"
#import "RCTEventDispatcher.h"
#import "RCTUtils.h"
#import <UIKit/UIKit.h>

@interface WebRTCManager ()

@end

@implementation WebRTCManager

@synthesize bridge = _bridge;

- (instancetype)init
{
  self = [super init];
  if (self) {
    _peerConnectionFactory = [[RTCPeerConnectionFactory alloc] init];
//    [RTCPeerConnectionFactory initializeSSL];
  }
  return self;
}

RCT_EXPORT_MODULE();

- (dispatch_queue_t)methodQueue
{
  return dispatch_get_main_queue();
}

@end

@implementation RCTConvert (RTCStates)

RCT_ENUM_CONVERTER(RTCICEConnectionState, (@{ @"new" : @(RTCICEConnectionNew),
                                              @"checking" : @(RTCICEConnectionChecking),
                                              @"connected" : @(RTCICEConnectionConnected),
                                              @"completed" : @(RTCICEConnectionCompleted),
                                              @"failed" : @(RTCICEConnectionFailed),
                                              @"disconnected" : @(RTCICEConnectionDisconnected),
                                              @"closed" : @(RTCICEConnectionClosed),
                                              @"max" : @(RTCICEConnectionMax)}),
                                             RTCICEConnectionNew, integerValue)
@end
