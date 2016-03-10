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
#import "WebRTCModule.h"

@interface UIView (WebRTCModule)

@property (nonatomic, strong) RTCVideoTrack *currentRenderer;

@end

@implementation UIView (WebRTCModule)

- (RTCVideoTrack *)currentRenderer
{
  return objc_getAssociatedObject(self, _cmd);
}

- (void)setCurrentRenderer:(RTCVideoTrack *)currentRenderer
{
  objc_setAssociatedObject(self, @selector(currentRenderer), currentRenderer, OBJC_ASSOCIATION_COPY_NONATOMIC);
}

@end

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

RCT_CUSTOM_VIEW_PROPERTY(streamURL, NSNumber, RTCEAGLVideoView)
{
  if (json) {
    NSInteger objectID = [json integerValue];

    WebRTCModule *module = [self.bridge moduleForName:@"WebRTCModule"];
    RTCMediaStream *stream = module.mediaStreams[@(objectID)];

    if (stream.videoTracks.count) {
      RTCVideoTrack *localVideoTrack = stream.videoTracks[0];
//      if (view.currentRenderer) {
//        [view.currentRenderer removeRenderer:view];
//      }
//      view.currentRenderer = localVideoTrack;
      [localVideoTrack addRenderer:view];
    }
  }
}

@end
