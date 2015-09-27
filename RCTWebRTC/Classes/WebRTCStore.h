//
//  WebRTCStore.h
//  TestReact
//
//  Created by one on 2015/9/25.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "RCTSparseArray.h"

@interface WebRTCStore : NSObject

@property (nonatomic, strong) RCTSparseArray *peerConnections;
@property (nonatomic, strong) RCTSparseArray *mediaStreams;
@property (nonatomic) NSInteger mediaStreamId;

+ (WebRTCStore *)sharedInstance;

@end
