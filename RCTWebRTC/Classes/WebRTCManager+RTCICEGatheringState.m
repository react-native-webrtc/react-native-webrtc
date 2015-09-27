//
//  WebRTCManager+RTCICEGatheringState.m
//  TestReact
//
//  Created by one on 2015/9/24.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import "WebRTCManager+RTCICEGatheringState.h"

@implementation WebRTCManager (RTCICEGatheringState)

- (NSString *)stringForICEGatheringState:(RTCICEGatheringState)state {
  NSString *gatheringState = nil;
  switch (state) {
    case RTCICEGatheringNew:
      gatheringState = @"New";
      break;
    case RTCICEGatheringGathering:
      gatheringState = @"Gathering";
      break;
    case RTCICEGatheringComplete:
      gatheringState = @"Complete";
      break;
    default:
      gatheringState = @"Other state";
      break;
  }
  return gatheringState;
}

@end
