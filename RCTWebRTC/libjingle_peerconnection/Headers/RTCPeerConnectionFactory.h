/*
 * libjingle
 * Copyright 2013 Google Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#import <Foundation/Foundation.h>

@class RTCAudioTrack;
@class RTCConfiguration;
@class RTCMediaConstraints;
@class RTCMediaStream;
@class RTCPeerConnection;
@class RTCVideoCapturer;
@class RTCVideoSource;
@class RTCVideoTrack;
@protocol RTCPeerConnectionDelegate;

// RTCPeerConnectionFactory is an ObjectiveC wrapper for PeerConnectionFactory.
// It is the main entry point to the PeerConnection API for clients.
@interface RTCPeerConnectionFactory : NSObject

// Initialize & de-initialize the SSL subsystem.  Failure is fatal.
+ (void)initializeSSL;
+ (void)deinitializeSSL;

// Create an RTCPeerConnection object. RTCPeerConnectionFactory will create
// required libjingle threads, socket and network manager factory classes for
// networking.
- (RTCPeerConnection *)
    peerConnectionWithICEServers:(NSArray *)servers
                     constraints:(RTCMediaConstraints *)constraints
                        delegate:(id<RTCPeerConnectionDelegate>)delegate;

// Creates a peer connection using the default port allocator factory and identity service.
- (RTCPeerConnection *)peerConnectionWithConfiguration:(RTCConfiguration *)configuration
                                           constraints:(RTCMediaConstraints *)constraints
                                              delegate:(id<RTCPeerConnectionDelegate>)delegate;

// Create an RTCMediaStream named |label|.
- (RTCMediaStream *)mediaStreamWithLabel:(NSString *)label;

// Creates a RTCVideoSource. The new source takes ownership of |capturer|.
// |constraints| decides video resolution and frame rate but can be NULL.
- (RTCVideoSource *)videoSourceWithCapturer:(RTCVideoCapturer *)capturer
                                constraints:(RTCMediaConstraints *)constraints;

// Creates a new local VideoTrack. The same |source| can be used in several
// tracks.
- (RTCVideoTrack *)videoTrackWithID:(NSString *)videoId
                             source:(RTCVideoSource *)source;

// Creates an new AudioTrack.
- (RTCAudioTrack *)audioTrackWithID:(NSString *)audioId;

@end
