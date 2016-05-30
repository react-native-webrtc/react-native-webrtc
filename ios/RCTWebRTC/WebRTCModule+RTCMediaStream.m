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

RCT_EXPORT_METHOD(getUserMedia:(NSDictionary *)constraints
               successCallback:(RCTResponseSenderBlock)successCallback
                 errorCallback:(RCTResponseSenderBlock)errorCallback)
{
  NSMutableArray *tracks = [NSMutableArray array];

  // Initialize RTCMediaStream with a unique label in order to allow multiple
  // RTCMediaStream instances initialized by multiple getUserMedia calls to be
  // added to 1 RTCPeerConnection instance. As suggested by
  // https://www.w3.org/TR/mediacapture-streams/#mediastream to be a good
  // practice, use a UUID (conforming to RFC4122).
  NSString *mediaStreamUUID = [[NSUUID UUID] UUIDString];
  RTCMediaStream *mediaStream = [self.peerConnectionFactory mediaStreamWithLabel:mediaStreamUUID];

  // constraints.audio
  id audioConstraints = constraints[@"audio"];
  if (audioConstraints && [audioConstraints boolValue]) {
    NSString *trackUUID = [[NSUUID UUID] UUIDString];
    RTCAudioTrack *audioTrack = [self.peerConnectionFactory audioTrackWithID:trackUUID];
    [mediaStream addAudioTrack:audioTrack];
    self.tracks[trackUUID] = audioTrack;
    [tracks addObject:@{@"id": trackUUID, @"kind": audioTrack.kind, @"label": audioTrack.label, @"enabled": @(audioTrack.isEnabled), @"remote": @(NO), @"readyState": @"live"}];
  }

  // constraints.video
  id videoConstraints = constraints[@"video"];
  if (videoConstraints) {
    AVCaptureDevice *videoDevice;
    if ([videoConstraints isKindOfClass:[NSNumber class]]) {
      if ([videoConstraints boolValue]) {
        videoDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
      }
    } else if ([videoConstraints isKindOfClass:[NSDictionary class]]) {
      // constraints.video.optional
      id optionalVideoConstraints = videoConstraints[@"optional"];
      if (optionalVideoConstraints) {
        if ([optionalVideoConstraints isKindOfClass:[NSArray class]]) {
          NSArray *options = optionalVideoConstraints;
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
        // constraints.video.facingMode
        //
        // https://www.w3.org/TR/mediacapture-streams/#def-constraint-facingMode
        id facingMode = videoConstraints[@"facingMode"];
        if (facingMode && [facingMode isKindOfClass:[NSString class]]) {
          AVCaptureDevicePosition position;
          if ([facingMode isEqualToString:@"environment"]) {
            position = AVCaptureDevicePositionBack;
          } else if ([facingMode isEqualToString:@"user"]) {
            position = AVCaptureDevicePositionFront;
          } else {
            // If the specified facingMode value is not supported, fall back to
            // the default video device.
            position = AVCaptureDevicePositionUnspecified;
          }
          if (AVCaptureDevicePositionUnspecified != position) {
            for (AVCaptureDevice *aVideoDevice in [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo]) {
              if (aVideoDevice.position == position) {
                videoDevice = aVideoDevice;
                break;
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
  successCallback(@[mediaStreamUUID, tracks]);
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
  RTCMediaStream *mediaStream = self.mediaStreams[streamID];
  RTCMediaStreamTrack *track;
  if (mediaStream && (track = self.tracks[trackID])) {
    [track setEnabled:NO];
    if ([track.kind isEqualToString:@"audio"]) {
      [self.tracks removeObjectForKey:trackID];
      [mediaStream removeAudioTrack:(RTCAudioTrack *)track];
    } else if([track.kind isEqualToString:@"video"]) {
      [self.tracks removeObjectForKey:trackID];
      [mediaStream removeVideoTrack:(RTCVideoTrack *)track];
    }
  }
}

RCT_EXPORT_METHOD(mediaStreamRelease:(nonnull NSString *)streamID)
{
  RTCMediaStream *mediaStream = self.mediaStreams[streamID];
  if (mediaStream) {
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
