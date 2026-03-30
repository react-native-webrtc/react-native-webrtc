#import <objc/runtime.h>

#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>
#import <WebRTC/RTCDataChannelConfiguration.h>

#import "WebRTCModule+RTCDataChannel.h"
#import "WebRTCModule+RTCPeerConnection.h"

@implementation WebRTCModule (RTCDataChannel)

/*
 * Thuis methos is implemented synchronously since we need to create the DataChannel on the spot
 * and where is no good way to report an error at creation time.
 */
RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(createDataChannel : (nonnull NSNumber *)peerConnectionId label : (NSString *)
                                           label config : (RTCDataChannelConfiguration *)config) {
    __block id channelInfo;

    dispatch_sync(self.workerQueue, ^{
        RTCPeerConnection *peerConnection = self.peerConnections[peerConnectionId];

        if (peerConnection == nil) {
            RCTLogWarn(@"PeerConnection %@ not found", peerConnectionId);
            channelInfo = @{};
            return;
        }

        RTCDataChannel *dataChannel = [peerConnection dataChannelForLabel:label configuration:config];

        if (dataChannel == nil) {
            channelInfo = @{};
            return;
        }

        NSString *reactTag = [[NSUUID UUID] UUIDString];
        DataChannelWrapper *dcw = [[DataChannelWrapper alloc] initWithChannel:dataChannel reactTag:reactTag];
        dcw.pcId = peerConnectionId;
        peerConnection.dataChannels[reactTag] = dcw;
        dcw.delegate = self;

        channelInfo = @{
            @"peerConnectionId" : peerConnectionId,
            @"reactTag" : reactTag,
            @"label" : dataChannel.label,
            @"id" : @(dataChannel.channelId),
            @"ordered" : @(dataChannel.isOrdered),
            @"maxPacketLifeTime" : @(dataChannel.maxPacketLifeTime),
            @"maxRetransmits" : @(dataChannel.maxRetransmits),
            @"protocol" : dataChannel.protocol,
            @"negotiated" : @(dataChannel.isNegotiated),
            @"readyState" : [self stringForDataChannelState:dataChannel.readyState]
        };
    });

    return channelInfo;
}

RCT_EXPORT_METHOD(dataChannelClose : (nonnull NSNumber *)peerConnectionId reactTag : (nonnull NSString *)tag {
    dispatch_sync(self.workerQueue, ^{
        if (self.destroyed) {
            return;
        }
        RTCPeerConnection *peerConnection = self.peerConnections[peerConnectionId];
        if (!peerConnection) {
            return;
        }
        DataChannelWrapper *dcw = peerConnection.dataChannels[tag];
        if (dcw) {
            [dcw.channel close];
        }
    });
})

RCT_EXPORT_METHOD(dataChannelDispose : (nonnull NSNumber *)peerConnectionId reactTag : (nonnull NSString *)tag {
    dispatch_sync(self.workerQueue, ^{
        RTCPeerConnection *peerConnection = self.peerConnections[peerConnectionId];
        if (!peerConnection) {
            return;
        }
        DataChannelWrapper *dcw = peerConnection.dataChannels[tag];
        if (dcw) {
            dcw.delegate = nil;
            [peerConnection.dataChannels removeObjectForKey:tag];
        }
    });
})

RCT_EXPORT_METHOD(dataChannelSend : (nonnull NSNumber *)peerConnectionId reactTag : (nonnull NSString *)
                      tag data : (NSString *)data type : (NSString *)type {
                          dispatch_sync(self.workerQueue, ^{
                              if (self.destroyed) {
                                  return;
                              }
                              RTCPeerConnection *peerConnection = self.peerConnections[peerConnectionId];
                              if (!peerConnection) {
                                  return;
                              }
                              DataChannelWrapper *dcw = peerConnection.dataChannels[tag];
                              if (dcw) {
                                  BOOL isBinary = [type isEqualToString:@"binary"];
                                  NSData *bytes = isBinary ? [[NSData alloc] initWithBase64EncodedString:data options:0]
                                                           : [data dataUsingEncoding:NSUTF8StringEncoding];
                                  RTCDataBuffer *buffer = [[RTCDataBuffer alloc] initWithData:bytes isBinary:isBinary];
                                  [dcw.channel sendData:buffer];
                              }
                          });
                      })

- (NSString *)stringForDataChannelState:(RTCDataChannelState)state {
    switch (state) {
        case RTCDataChannelStateConnecting:
            return @"connecting";
        case RTCDataChannelStateOpen:
            return @"open";
        case RTCDataChannelStateClosing:
            return @"closing";
        case RTCDataChannelStateClosed:
            return @"closed";
    }
    return @"unknown";
}

#pragma mark - DataChannelWrapperDelegate methods

// Called when the data channel state has changed.
- (void)dataChannelDidChangeState:(DataChannelWrapper *)dcw {
    if (self.destroyed) {
        return;
    }
    RTCDataChannel *channel = dcw.channel;
    NSDictionary *event = @{
        @"reactTag" : dcw.reactTag,
        @"peerConnectionId" : dcw.pcId,
        @"id" : @(channel.channelId),
        @"state" : [self stringForDataChannelState:channel.readyState]
    };
#ifdef RCT_NEW_ARCH_ENABLED
    [self emitDataChannelStateChanged:event];
#else
    [self sendEventWithName:kEventDataChannelStateChanged body:event];
#endif
}

// Called when a data buffer was successfully received.
- (void)dataChannel:(DataChannelWrapper *)dcw didReceiveMessageWithBuffer:(RTCDataBuffer *)buffer {
    if (self.destroyed) {
        return;
    }
    NSString *type;
    NSString *data;
    if (buffer.isBinary) {
        type = @"binary";
        data = [buffer.data base64EncodedStringWithOptions:0];
    } else {
        type = @"text";
        // XXX NSData has a length property which means that, when it represents
        // text, the value of its bytes property does not have to be terminated by
        // null. In such a case, NSString's stringFromUTF8String may fail and return
        // nil (which would crash the process when inserting data into NSDictionary
        // without the nil protection implemented below).
        data = [[NSString alloc] initWithData:buffer.data encoding:NSUTF8StringEncoding];
    }
    NSDictionary *event = @{
        @"reactTag" : dcw.reactTag,
        @"peerConnectionId" : dcw.pcId,
        @"type" : type,
        // XXX NSDictionary will crash the process upon
        // attempting to insert nil. Such behavior is
        // unacceptable given that protection in such a
        // scenario is extremely simple.
        @"data" : (data ? data : [NSNull null])
    };
#ifdef RCT_NEW_ARCH_ENABLED
    [self emitDataChannelReceiveMessage:event];
#else
    [self sendEventWithName:kEventDataChannelReceiveMessage body:event];
#endif
}

- (void)dataChannel:(DataChannelWrapper *)dcw didChangeBufferedAmount:(uint64_t)amount {
    if (self.destroyed) {
        return;
    }
    NSDictionary *event = @{
        @"reactTag" : dcw.reactTag,
        @"peerConnectionId" : dcw.pcId,
        @"bufferedAmount" : [NSNumber numberWithUnsignedLongLong:amount]
    };
#ifdef RCT_NEW_ARCH_ENABLED
    [self emitDataChannelDidChangeBufferedAmount:event];
#else
    [self sendEventWithName:kEventDataChannelDidChangeBufferedAmount body:event];
#endif
}

@end
