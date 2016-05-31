/*
 * libjingle
 * Copyright 2015 Google Inc.
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

// See talk/app/webrtc/peerconnectioninterface.h.

#import <Foundation/Foundation.h>

typedef NS_ENUM(NSInteger, RTCIceTransportsType) {
  kRTCIceTransportsTypeNone,
  kRTCIceTransportsTypeRelay,
  kRTCIceTransportsTypeNoHost,
  kRTCIceTransportsTypeAll,
};

// https://tools.ietf.org/html/draft-ietf-rtcweb-jsep-08#section-4.1.1
typedef NS_ENUM(NSInteger, RTCBundlePolicy) {
  kRTCBundlePolicyBalanced,
  kRTCBundlePolicyMaxBundle,
  kRTCBundlePolicyMaxCompat,
};

// https://tools.ietf.org/html/draft-ietf-rtcweb-jsep-09#section-4.1.1
typedef NS_ENUM(NSInteger, RTCRtcpMuxPolicy) {
  kRTCRtcpMuxPolicyNegotiate,
  kRTCRtcpMuxPolicyRequire,
};

typedef NS_ENUM(NSInteger, RTCTcpCandidatePolicy) {
  kRTCTcpCandidatePolicyEnabled,
  kRTCTcpCandidatePolicyDisabled,
};

// Configuration object used for creating a peer connection.
@interface RTCConfiguration : NSObject

@property(nonatomic, assign) RTCIceTransportsType iceTransportsType;
@property(nonatomic, copy) NSArray *iceServers;
@property(nonatomic, assign) RTCBundlePolicy bundlePolicy;
@property(nonatomic, assign) RTCRtcpMuxPolicy rtcpMuxPolicy;
@property(nonatomic, assign) RTCTcpCandidatePolicy tcpCandidatePolicy;
@property(nonatomic, assign) int audioJitterBufferMaxPackets;
@property(nonatomic, assign) int iceConnectionReceivingTimeout;
@property(nonatomic, assign) int iceBackupCandidatePairPingInterval;

- (instancetype)initWithIceTransportsType:(RTCIceTransportsType)iceTransportsType
                             bundlePolicy:(RTCBundlePolicy)bundlePolicy
                            rtcpMuxPolicy:(RTCRtcpMuxPolicy)rtcpMuxPolicy
                       tcpCandidatePolicy:(RTCTcpCandidatePolicy)tcpCandidatePolicy
              audioJitterBufferMaxPackets:(int)audioJitterBufferMaxPackets
            iceConnectionReceivingTimeout:(int)iceConnectionReceivingTimeout
       iceBackupCandidatePairPingInterval:(int)iceBackupCandidatePairPingInterval;

@end
