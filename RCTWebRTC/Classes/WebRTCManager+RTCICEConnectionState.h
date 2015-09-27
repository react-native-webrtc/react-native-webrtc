//
//  WebRTCManager+RTCICEConnectionState.h
//  TestReact
//
//  Created by one on 2015/9/24.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import "WebRTCManager.h"

@interface WebRTCManager (RTCICEConnectionState)

- (NSString *)stringForICEConnectionState:(RTCICEConnectionState)state;

@end
