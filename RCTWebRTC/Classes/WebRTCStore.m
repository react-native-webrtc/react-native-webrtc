//
//  WebRTCStore.m
//  TestReact
//
//  Created by one on 2015/9/25.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import "WebRTCStore.h"

@implementation WebRTCStore

+ (WebRTCStore *)sharedInstance {
  static WebRTCStore *_sharedClient = nil;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    _sharedClient = [[WebRTCStore alloc] init];
  });
  return _sharedClient;
}

- (instancetype)init
{
  self = [super init];
  if (self) {
    _peerConnections = [RCTSparseArray new];
    _mediaStreams = [RCTSparseArray new];
    _mediaStreamId = 0;
  }
  return self;
}
@end
