//
//  WebRTCManager+RTCICEConnectionState.m
//  TestReact
//
//  Created by one on 2015/9/24.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import "WebRTCManager+RTCICEConnectionState.h"

@implementation WebRTCManager (RTCICEConnectionState)

- (NSString *)stringForICEConnectionState:(RTCICEConnectionState)state {
  NSString *connectionStateString = nil;
  switch (state) {
    case RTCICEConnectionNew:
      connectionStateString = @"new";
      break;
    case RTCICEConnectionChecking:
      connectionStateString = @"checking";
      break;
    case RTCICEConnectionConnected:
      connectionStateString = @"connected";
      break;
    case RTCICEConnectionCompleted:
      connectionStateString = @"completed";
      break;
    case RTCICEConnectionFailed:
      connectionStateString = @"failed";
      break;
    case RTCICEConnectionDisconnected:
      connectionStateString = @"disconnected";
      break;
    case RTCICEConnectionClosed:
      connectionStateString = @"closed";
      break;
    default:
      connectionStateString = @"Other state";
      break;
  }
  return connectionStateString;
}

@end
