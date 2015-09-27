//
//  WebRTCManager+RTCMediaStream.m
//  TestReact
//
//  Created by one on 2015/9/24.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import "WebRTCManager+RTCMediaStream.h"

#import "RTCVideoCapturer.h"
#import "RTCVideoSource.h"
#import "RTCVideoTrack.h"
#import <objc/runtime.h>
#import "RTCPair.h"
#import "RTCMediaConstraints.h"
#import "WebRTCManager+RTCPeerConnection.h"

@implementation RTCMediaStream (React)

- (NSNumber *)reactTag
{
  return objc_getAssociatedObject(self, _cmd);
}

- (void)setReactTag:(NSNumber *)reactTag
{
  objc_setAssociatedObject(self, @selector(reactTag), reactTag, OBJC_ASSOCIATION_COPY_NONATOMIC);
}

@end

@implementation WebRTCManager (RTCMediaStream)

RCT_EXPORT_METHOD(getUserMedia:(NSDictionary *)constraints callback:(RCTResponseSenderBlock)callback)
{
  NSNumber *objectID = @([WebRTCStore sharedInstance].mediaStreamId++);
  
  RTCMediaStream *mediaStream = [self.peerConnectionFactory mediaStreamWithLabel:@"ARDAMS"];
  
  if (constraints[@"audio"] && [constraints[@"audio"] boolValue]) {
    RTCAudioTrack *audioTrack = [self.peerConnectionFactory audioTrackWithID:@"ARDAMSa0"];
    [mediaStream addAudioTrack:audioTrack];
  }
  
  if (constraints[@"video"] && [constraints[@"video"] boolValue]) {
    NSArray *devices = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    AVCaptureDevice *videoDevice = [devices lastObject];
    
    RTCVideoCapturer *capturer = [RTCVideoCapturer capturerWithDeviceName:[videoDevice localizedName]];
    RTCVideoSource *videoSource = [self.peerConnectionFactory videoSourceWithCapturer:capturer constraints:[self defaultMediaStreamConstraints]];
    RTCVideoTrack *videoTrack = [self.peerConnectionFactory videoTrackWithID:@"ARDAMSv0" source:videoSource];
    [mediaStream addVideoTrack:videoTrack];
  }
  
  mediaStream.reactTag = objectID;
  [WebRTCStore sharedInstance].mediaStreams[objectID] = mediaStream;
  callback(@[objectID]);
}
- (RTCMediaConstraints *)defaultMediaStreamConstraints {
  RTCMediaConstraints* constraints =
  [[RTCMediaConstraints alloc]
   initWithMandatoryConstraints:nil
   optionalConstraints:nil];
  return constraints;
}

@end
