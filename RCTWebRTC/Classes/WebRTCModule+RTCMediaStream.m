//
//  WebRTCModule+RTCMediaStream.m
//
//  Created by one on 2015/9/24.
//  Copyright © 2015 One. All rights reserved.
//

#import "WebRTCModule+RTCMediaStream.h"

#import "RTCVideoCapturer.h"
#import "RTCVideoSource.h"
#import "RTCVideoTrack.h"
#import <objc/runtime.h>
#import "RTCPair.h"
#import "RTCMediaConstraints.h"
#import "WebRTCModule+RTCPeerConnection.h"

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

@implementation WebRTCModule (RTCMediaStream)

RCT_EXPORT_METHOD(getUserMedia:(NSDictionary *)constraints callback:(RCTResponseSenderBlock)callback)
{
  NSNumber *objectID = @(self.mediaStreamId++);

  RTCMediaStream *mediaStream = [self.peerConnectionFactory mediaStreamWithLabel:@"ARDAMS"];

  if (constraints[@"audio"] && [constraints[@"audio"] boolValue]) {
    RTCAudioTrack *audioTrack = [self.peerConnectionFactory audioTrackWithID:@"ARDAMSa0"];
    [mediaStream addAudioTrack:audioTrack];
  }

  if (constraints[@"video"] && [constraints[@"video"] boolValue]) {
    NSArray *devices = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    AVCaptureDevice *videoDevice;
    
    if (constraints[@"videoType"]) {
      NSNumber *positionObject = [self captureDevicePositionFrom:constraints[@"videoType"]];
      if (positionObject == nil) {
        videoDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
      } else {
        AVCaptureDevicePosition position = [positionObject integerValue];
        
        for (AVCaptureDevice *device in devices) {
          if (device.position == position) {
            videoDevice = device;
            break;
          }
        }
        
        if (!videoDevice) {
          videoDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
        }
      }
    } else {
      videoDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    }

    RTCVideoCapturer *capturer = [RTCVideoCapturer capturerWithDeviceName:[videoDevice localizedName]];
    RTCVideoSource *videoSource = [self.peerConnectionFactory videoSourceWithCapturer:capturer constraints:[self defaultMediaStreamConstraints]];
    RTCVideoTrack *videoTrack = [self.peerConnectionFactory videoTrackWithID:@"ARDAMSv0" source:videoSource];
    [mediaStream addVideoTrack:videoTrack];
  }

  mediaStream.reactTag = objectID;
  self.mediaStreams[objectID] = mediaStream;
  callback(@[objectID]);
}

RCT_EXPORT_METHOD(mediaStreamRelease:(nonnull NSNumber *)streamID)
{
  [self.mediaStreams removeObjectForKey:streamID];
}
- (RTCMediaConstraints *)defaultMediaStreamConstraints {
  RTCMediaConstraints* constraints =
  [[RTCMediaConstraints alloc]
   initWithMandatoryConstraints:nil
   optionalConstraints:nil];
  return constraints;
}

- (NSNumber*)captureDevicePositionFrom:(NSString*)string {
  if ([string isEqualToString:@"front"]) {
    return @(AVCaptureDevicePositionFront);
  } else if ([string isEqualToString:@"back"]) {
    return @(AVCaptureDevicePositionBack);
  } else {
    return nil;
  }
}

@end
