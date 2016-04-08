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

#import "RTCPeerConnectionDelegate.h"

@class RTCDataChannel;
@class RTCDataChannelInit;
@class RTCICECandidate;
@class RTCICEServers;
@class RTCMediaConstraints;
@class RTCMediaStream;
@class RTCMediaStreamTrack;
@class RTCSessionDescription;
@protocol RTCSessionDescriptionDelegate;
@protocol RTCStatsDelegate;

// RTCPeerConnection is an ObjectiveC friendly wrapper around a PeerConnection
// object.  See the documentation in talk/app/webrtc/peerconnectioninterface.h.
// or http://www.webrtc.org/reference/native-apis, which in turn is inspired by
// the JS APIs: http://dev.w3.org/2011/webrtc/editor/webrtc.html and
// http://www.w3.org/TR/mediacapture-streams/
@interface RTCPeerConnection : NSObject

@property(nonatomic, weak) id<RTCPeerConnectionDelegate> delegate;

// Accessor methods to active local streams.
@property(nonatomic, strong, readonly) NSArray *localStreams;

// The local description.
@property(nonatomic, assign, readonly) RTCSessionDescription *localDescription;

// The remote description.
@property(nonatomic, assign, readonly) RTCSessionDescription *remoteDescription;

// The current signaling state.
@property(nonatomic, assign, readonly) RTCSignalingState signalingState;
@property(nonatomic, assign, readonly) RTCICEConnectionState iceConnectionState;
@property(nonatomic, assign, readonly) RTCICEGatheringState iceGatheringState;

// Add a new MediaStream to be sent on this PeerConnection.
// Note that a SessionDescription negotiation is needed before the
// remote peer can receive the stream.
- (BOOL)addStream:(RTCMediaStream *)stream;

// Remove a MediaStream from this PeerConnection.
// Note that a SessionDescription negotiation is need before the
// remote peer is notified.
- (void)removeStream:(RTCMediaStream *)stream;

// Create a data channel.
- (RTCDataChannel*)createDataChannelWithLabel:(NSString*)label
                                       config:(RTCDataChannelInit*)config;

// Create a new offer.
// Success or failure will be reported via RTCSessionDescriptionDelegate.
- (void)createOfferWithDelegate:(id<RTCSessionDescriptionDelegate>)delegate
                    constraints:(RTCMediaConstraints *)constraints;

// Create an answer to an offer.
// Success or failure will be reported via RTCSessionDescriptionDelegate.
- (void)createAnswerWithDelegate:(id<RTCSessionDescriptionDelegate>)delegate
                     constraints:(RTCMediaConstraints *)constraints;

// Sets the local session description.
// Success or failure will be reported via RTCSessionDescriptionDelegate.
- (void)
    setLocalDescriptionWithDelegate:(id<RTCSessionDescriptionDelegate>)delegate
                 sessionDescription:(RTCSessionDescription *)sdp;

// Sets the remote session description.
// Success or failure will be reported via RTCSessionDescriptionDelegate.
- (void)
    setRemoteDescriptionWithDelegate:(id<RTCSessionDescriptionDelegate>)delegate
                  sessionDescription:(RTCSessionDescription *)sdp;

// Restarts or updates the ICE Agent process of gathering local candidates
// and pinging remote candidates.
- (BOOL)updateICEServers:(NSArray *)servers
             constraints:(RTCMediaConstraints *)constraints;

// Provides a remote candidate to the ICE Agent.
- (BOOL)addICECandidate:(RTCICECandidate *)candidate;

// Terminates all media and closes the transport.
- (void)close;

// Gets statistics for the media track. If |mediaStreamTrack| is nil statistics
// are gathered for all tracks.
// Statistics information will be reported via RTCStatsDelegate.
- (BOOL)getStatsWithDelegate:(id<RTCStatsDelegate>)delegate
            mediaStreamTrack:(RTCMediaStreamTrack*)mediaStreamTrack
            statsOutputLevel:(RTCStatsOutputLevel)statsOutputLevel;

#ifndef DOXYGEN_SHOULD_SKIP_THIS
// Disallow init and don't add to documentation
- (id)init __attribute__(
    (unavailable("init is not a supported initializer for this class.")));
#endif /* DOXYGEN_SHOULD_SKIP_THIS */

@end
