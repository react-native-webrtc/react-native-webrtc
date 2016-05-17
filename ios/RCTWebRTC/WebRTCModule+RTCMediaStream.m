//
//  WebRTCModule+RTCMediaStream.m
//
//  Created by one on 2015/9/24.
//  Copyright © 2015 One. All rights reserved.
//

#import <objc/runtime.h>

#import "RTCVideoCapturer.h"
#import "RTCVideoSource.h"
#import "RTCVideoTrack.h"
#import "RTCPair.h"
#import "RTCMediaConstraints.h"

#import "WebRTCModule+RTCPeerConnection.h"

@implementation AVCaptureDevice (React)

- (NSString*)positionString {
  switch (self.position) {
    case AVCaptureDevicePositionUnspecified: return @"unspecified";
    case AVCaptureDevicePositionBack: return @"back";
    case AVCaptureDevicePositionFront: return @"front";
  }
  return nil;
}

@end

@implementation WebRTCModule (RTCMediaStream)

RCT_EXPORT_METHOD(getUserMedia:(NSDictionary *)constraints callback:(RCTResponseSenderBlock)callback)
{
  NSMutableArray *tracks = [NSMutableArray array];

  // Initialize RTCMediaStream with a unique label in order to allow multiple
  // RTCMediaStream instances initialized by multiple getUserMedia calls to be
  // added to 1 RTCPeerConnection instance. As suggested by
  // https://www.w3.org/TR/mediacapture-streams/#mediastream to be a good
  // practice, use a UUID (conforming to RFC4122).
  NSString *mediaStreamUUID = [[NSUUID UUID] UUIDString];
  RTCMediaStream *mediaStream = [self.peerConnectionFactory mediaStreamWithLabel:mediaStreamUUID];

  if (constraints[@"audio"] && [constraints[@"audio"] boolValue]) {
    NSString *trackUUID = [[NSUUID UUID] UUIDString];
    RTCAudioTrack *audioTrack = [self.peerConnectionFactory audioTrackWithID:trackUUID];
    [mediaStream addAudioTrack:audioTrack];
    self.tracks[trackUUID] = audioTrack;
    [tracks addObject:@{@"id": trackUUID, @"kind": audioTrack.kind, @"label": audioTrack.label, @"enabled": @(audioTrack.isEnabled), @"remote": @(NO), @"readyState": @"live"}];
  }

  if (constraints[@"video"]) {
    AVCaptureDevice *videoDevice;
    if ([constraints[@"video"] isKindOfClass:[NSNumber class]]) {
      if ([constraints[@"video"] boolValue]) {
        videoDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
      }
    } else if ([constraints[@"video"] isKindOfClass:[NSDictionary class]]) {
      if (constraints[@"video"][@"optional"]) {
        if ([constraints[@"video"][@"optional"] isKindOfClass:[NSArray class]]) {
          NSArray *options = constraints[@"video"][@"optional"];
          for (id item in options) {
            if ([item isKindOfClass:[NSDictionary class]]) {
              NSDictionary *dict = item;
              if (dict[@"sourceId"]) {
                videoDevice = [AVCaptureDevice deviceWithUniqueID:dict[@"sourceId"]];
              }
            }
          }
        }
      }
      if (!videoDevice) {
        videoDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
      }
    }
    
    if (videoDevice) {
      RTCVideoCapturer *capturer = [RTCVideoCapturer capturerWithDeviceName:[videoDevice localizedName]];
      RTCVideoSource *videoSource = [self.peerConnectionFactory videoSourceWithCapturer:capturer constraints:[self defaultMediaStreamConstraints]];
      NSString *trackUUID = [[NSUUID UUID] UUIDString];
      RTCVideoTrack *videoTrack = [self.peerConnectionFactory videoTrackWithID:trackUUID source:videoSource];
      [mediaStream addVideoTrack:videoTrack];
      self.tracks[trackUUID] = videoTrack;
      [tracks addObject:@{@"id": trackUUID, @"kind": videoTrack.kind, @"label": videoTrack.label, @"enabled": @(videoTrack.isEnabled), @"remote": @(NO), @"readyState": @"live"}];

    }
  }

  self.mediaStreams[mediaStreamUUID] = mediaStream;
  callback(@[mediaStreamUUID, tracks]);
}

RCT_EXPORT_METHOD(mediaStreamTrackGetSources:(RCTResponseSenderBlock)callback) {
  NSMutableArray *sources = [NSMutableArray array];
  NSArray *videoDevices = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
  for (AVCaptureDevice *device in videoDevices) {
    [sources addObject:@{
                         @"facing": device.positionString,
                         @"id": device.uniqueID,
                         @"label": device.localizedName,
                         @"kind": @"video",
                         }];
  }
  NSArray *audioDevices = [AVCaptureDevice devicesWithMediaType:AVMediaTypeAudio];
  for (AVCaptureDevice *device in audioDevices) {
    [sources addObject:@{
                         @"facing": @"",
                         @"id": device.uniqueID,
                         @"label": device.localizedName,
                         @"kind": @"audio",
                         }];
  }
  callback(@[sources]);
}

RCT_EXPORT_METHOD(mediaStreamTrackStop:(nonnull NSString *)trackID)
{
  RTCMediaStreamTrack *track = self.tracks[trackID];
  if (track) {
    [track setEnabled:NO];
    [self.tracks removeObjectForKey:trackID];
  }
}

RCT_EXPORT_METHOD(mediaStreamTrackSetEnabled:(nonnull NSString *)trackID : (BOOL *)enabled)
{
  RTCMediaStreamTrack *track = self.tracks[trackID];
  if (track && track.isEnabled != enabled) {
    [track setEnabled:enabled];
  }
}

RCT_EXPORT_METHOD(mediaStreamTrackRelease:(nonnull NSString *)streamID : (nonnull NSString *)trackID)
{
  // what's different to mediaStreamTrackStop? only call mediaStream explicitly?
  if (self.mediaStreams[streamID] && self.tracks[trackID]) {
    RTCMediaStream *mediaStream = self.mediaStreams[streamID];
    RTCMediaStreamTrack *track = self.tracks[trackID];
    [track setEnabled:NO];
    if ([track.kind isEqualToString:@"audio"]) {
      RTCAudioTrack *audioTrack = self.tracks[trackID];
      [self.tracks removeObjectForKey:trackID];
      [mediaStream removeAudioTrack:audioTrack];
    } else if([track.kind isEqualToString:@"video"]) {
      RTCVideoTrack *videoTrack = self.tracks[trackID];
      [self.tracks removeObjectForKey:trackID];
      [mediaStream removeVideoTrack:videoTrack];
    }
  }
}

RCT_EXPORT_METHOD(mediaStreamRelease:(nonnull NSString *)streamID)
{
  if (self.mediaStreams[streamID]) {
    RTCMediaStream *mediaStream = self.mediaStreams[streamID];
    for (RTCVideoTrack *track in mediaStream.videoTracks) {
      [self.tracks removeObjectForKey:track.label];
    }
    for (RTCAudioTrack *track in mediaStream.audioTracks) {
      [self.tracks removeObjectForKey:track.label];
    }
    [self.mediaStreams removeObjectForKey:streamID];
  }
}
- (RTCMediaConstraints *)defaultMediaStreamConstraints {
  RTCMediaConstraints* constraints =
  [[RTCMediaConstraints alloc]
   initWithMandatoryConstraints:nil
   optionalConstraints:nil];
  return constraints;
}

@end
