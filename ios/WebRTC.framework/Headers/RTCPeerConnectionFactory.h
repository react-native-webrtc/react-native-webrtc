/*
 *  Copyright 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#import <Foundation/Foundation.h>

#import <WebRTC/RTCMacros.h>

NS_ASSUME_NONNULL_BEGIN

@class RTCAVFoundationVideoSource;
@class RTCAudioTrack;
@class RTCConfiguration;
@class RTCMediaConstraints;
@class RTCMediaStream;
@class RTCPeerConnection;
@class RTCVideoSource;
@class RTCVideoTrack;
@protocol RTCPeerConnectionDelegate;

RTC_EXPORT
@interface RTCPeerConnectionFactory : NSObject

- (instancetype)init NS_DESIGNATED_INITIALIZER;

/** Initialize an RTCAVFoundationVideoSource with constraints. */
- (RTCAVFoundationVideoSource *)avFoundationVideoSourceWithConstraints:
    (nullable RTCMediaConstraints *)constraints;

/** Initialize an RTCAudioTrack with an id. */
- (RTCAudioTrack *)audioTrackWithTrackId:(NSString *)trackId;

/** Initialize an RTCVideoTrack with a source and an id. */
- (RTCVideoTrack *)videoTrackWithSource:(RTCVideoSource *)source
                                trackId:(NSString *)trackId;

/** Initialize an RTCMediaStream with an id. */
- (RTCMediaStream *)mediaStreamWithStreamId:(NSString *)streamId;

/** Initialize an RTCPeerConnection with a configuration, constraints, and
 *  delegate.
 */
- (RTCPeerConnection *)peerConnectionWithConfiguration:
    (RTCConfiguration *)configuration
                                           constraints:
    (RTCMediaConstraints *)constraints
                                              delegate:
    (nullable id<RTCPeerConnectionDelegate>)delegate;

/** Temporary interface. Use at your own risk. See peerconnectioninterface.h for details. */
- (BOOL)startRtcEventLogWithFilePath:(NSString *)filePath
                      maxSizeInBytes:(int64_t)maxSizeInBytes;
- (void)stopRtcEventLog;

@end

NS_ASSUME_NONNULL_END
