/*
 * libjingle
 * Copyright 2014 Google Inc.
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

// ObjectiveC wrapper for a DataChannelInit object.
@interface RTCDataChannelInit : NSObject

// Set to YES if ordered delivery is required
@property(nonatomic) BOOL isOrdered;
// Max period in milliseconds in which retransmissions will be sent. After this
// time, no more retransmissions will be sent. -1 if unset.
@property(nonatomic) NSInteger maxRetransmitTimeMs;
// The max number of retransmissions. -1 if unset.
@property(nonatomic) NSInteger maxRetransmits;
// Set to YES if the channel has been externally negotiated and we do not send
// an in-band signalling in the form of an "open" message
@property(nonatomic) BOOL isNegotiated;
// The stream id, or SID, for SCTP data channels. -1 if unset.
@property(nonatomic) NSInteger streamId;
// Set by the application and opaque to the WebRTC implementation.
@property(nonatomic) NSString* protocol;

@end

// ObjectiveC wrapper for a DataBuffer object.
@interface RTCDataBuffer : NSObject

@property(nonatomic, readonly) NSData* data;
@property(nonatomic, readonly) BOOL isBinary;

- (instancetype)initWithData:(NSData*)data isBinary:(BOOL)isBinary;

#ifndef DOXYGEN_SHOULD_SKIP_THIS
// Disallow init and don't add to documentation
- (id)init __attribute__((
    unavailable("init is not a supported initializer for this class.")));
#endif /* DOXYGEN_SHOULD_SKIP_THIS */

@end

// Keep in sync with webrtc::DataChannelInterface::DataState
typedef enum {
  kRTCDataChannelStateConnecting,
  kRTCDataChannelStateOpen,
  kRTCDataChannelStateClosing,
  kRTCDataChannelStateClosed
} RTCDataChannelState;

@class RTCDataChannel;
// Protocol for receving data channel state and message events.
@protocol RTCDataChannelDelegate<NSObject>

// Called when the data channel state has changed.
- (void)channelDidChangeState:(RTCDataChannel*)channel;

// Called when a data buffer was successfully received.
- (void)channel:(RTCDataChannel*)channel
    didReceiveMessageWithBuffer:(RTCDataBuffer*)buffer;

@optional

// Called when the buffered amount has changed.
- (void)channel:(RTCDataChannel*)channel
    didChangeBufferedAmount:(NSUInteger)amount;

@end

// ObjectiveC wrapper for a DataChannel object.
// See talk/app/webrtc/datachannelinterface.h
@interface RTCDataChannel : NSObject

@property(nonatomic, readonly) NSString* label;
@property(nonatomic, readonly) BOOL isReliable;
@property(nonatomic, readonly) BOOL isOrdered;
@property(nonatomic, readonly) NSUInteger maxRetransmitTime;
@property(nonatomic, readonly) NSUInteger maxRetransmits;
@property(nonatomic, readonly) NSString* protocol;
@property(nonatomic, readonly) BOOL isNegotiated;
@property(nonatomic, readonly) NSInteger streamId;
@property(nonatomic, readonly) RTCDataChannelState state;
@property(nonatomic, readonly) NSUInteger bufferedAmount;
@property(nonatomic, weak) id<RTCDataChannelDelegate> delegate;

- (void)close;
- (BOOL)sendData:(RTCDataBuffer*)data;

#ifndef DOXYGEN_SHOULD_SKIP_THIS
// Disallow init and don't add to documentation
- (id)init __attribute__((
    unavailable("init is not a supported initializer for this class.")));
#endif /* DOXYGEN_SHOULD_SKIP_THIS */

@end
