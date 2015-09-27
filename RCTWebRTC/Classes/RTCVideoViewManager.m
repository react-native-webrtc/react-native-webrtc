//
//  RTCVideoViewManager.m
//  TestReact
//
//  Created by one on 2015/9/25.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import "RTCVideoViewManager.h"
#import "RTCEAGLVideoView.h"
#import "RTCVideoTrack.h"
#import "RTCMediaStream.h"
#import "WebRTCStore.h"

@implementation RTCVideoViewManager

RCT_EXPORT_MODULE()

- (UIView *)view
{
  return [[RTCEAGLVideoView alloc] init];
}

- (dispatch_queue_t)methodQueue
{
  return dispatch_get_main_queue();
}

RCT_CUSTOM_VIEW_PROPERTY(src, NSNumber, RTCEAGLVideoView)
{
  if (json) {
    NSInteger objectID = [json integerValue];
    RTCMediaStream *stream = [WebRTCStore sharedInstance].mediaStreams[objectID];

    RTCVideoTrack *localVideoTrack = stream.videoTracks[0];
    
    [localVideoTrack addRenderer:view];
  }
}

@end
