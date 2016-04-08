//
//  WebRTCModule.m
//
//  Created by one on 2015/9/24.
//  Copyright © 2015 One. All rights reserved.
//

#import <UIKit/UIKit.h>

#import "RCTBridge.h"
#import "RCTEventDispatcher.h"
#import "RCTUtils.h"

#import "WebRTCModule.h"

@interface WebRTCModule ()

@end

@implementation WebRTCModule

@synthesize bridge = _bridge;

- (instancetype)init
{
  self = [super init];
  if (self) {
    _peerConnectionFactory = [[RTCPeerConnectionFactory alloc] init];
//    [RTCPeerConnectionFactory initializeSSL];
    
    _peerConnections = [NSMutableDictionary new];
    _mediaStreams = [NSMutableDictionary new];
    _tracks = [NSMutableDictionary new];
    _mediaStreamId = 0;
    _trackId = 0;
  }
  return self;
}

- (void)dealloc {
  for (RTCPeerConnection *peerConnection in _peerConnections.allValues) {
    peerConnection.delegate = nil;
    [peerConnection close];
  }
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
