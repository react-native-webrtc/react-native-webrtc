//
//  WebRTCModule+RTCICEGatheringState.m
//
//  Created by one on 2015/9/24.
//  Copyright © 2015 One. All rights reserved.
//

#import "WebRTCModule+RTCICEGatheringState.h"

@implementation WebRTCModule (RTCICEGatheringState)

- (NSString *)stringForICEGatheringState:(RTCICEGatheringState)state {
  NSString *gatheringState = nil;
  switch (state) {
    case RTCICEGatheringNew:
      gatheringState = @"new";
      break;
    case RTCICEGatheringGathering:
      gatheringState = @"gathering";
      break;
    case RTCICEGatheringComplete:
      gatheringState = @"complete";
      break;
    default:
      gatheringState = @"other-state";
      break;
  }
  return gatheringState;
}

@end
