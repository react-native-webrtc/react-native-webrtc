#import <objc/runtime.h>

#import <React/RCTBridge.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventDispatcher.h>

#import <WebRTC/RTCDataChannelConfiguration.h>
#import "WebRTCModule+RTCDataChannel.h"
#import "WebRTCModule+RTCPeerConnection.h"

@implementation WebRTCModule (RTCDataChannel)

/*
 * Thuis methos is implemented synchronously since we need to create the DataChannel on the spot
 * and where is no good way to report an error at creation time.
 */
RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(createDataChannel
                                       : (nonnull NSNumber *)peerConnectionId label
                                       : (NSString *)label config
                                       : (RTCDataChannelConfiguration *)config) {
    __block id channelInfo;

    dispatch_sync(self.workerQueue, ^{
        RTCPeerConnection *peerConnection = self.peerConnections[peerConnectionId];

        if (peerConnection == nil) {
            RCTLogWarn(@"PeerConnection %@ not found", peerConnectionId);
            channelInfo = nil;
            return;
        }

        RTCDataChannel *dataChannel = [peerConnection dataChannelForLabel:label configuration:config];

        if (dataChannel == nil) {
            channelInfo = nil;
            return;
        }

        NSString *reactTag = [[NSUUID UUID] UUIDString];
        DataChannelObserver *dco = [[DataChannelObserver alloc] initWithChannel:dataChannel reactTag:reactTag];
        dco.peerConnectionId = peerConnectionId;
        peerConnection.dataChannels[reactTag] = dco;
        dco.delegate = self;

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

RCT_EXPORT_METHOD(dataChannelClose
                  : (nonnull NSNumber *)peerConnectionId reactTag
                  : (nonnull NSString *)tag {
                      RTCPeerConnection *peerConnection = self.peerConnections[peerConnectionId];
                      DataChannelObserver *dco = peerConnection.dataChannels[tag];
                      if (dco) {
                          [dco.channel close];
                      }
                  })

RCT_EXPORT_METHOD(dataChannelDispose
                  : (nonnull NSNumber *)peerConnectionId reactTag
                  : (nonnull NSString *)tag {
                      RTCPeerConnection *peerConnection = self.peerConnections[peerConnectionId];
                      DataChannelObserver *dco = peerConnection.dataChannels[tag];
                      if (dco) {
                          dco.delegate = nil;
                          [peerConnection.dataChannels removeObjectForKey:tag];
                      }
                  })

RCT_EXPORT_METHOD(dataChannelSend
                  : (nonnull NSNumber *)peerConnectionId reactTag
                  : (nonnull NSString *)tag data
                  : (NSString *)data type
                  : (NSString *)type {
                      RTCPeerConnection *peerConnection = self.peerConnections[peerConnectionId];
                      DataChannelObserver *dco = peerConnection.dataChannels[tag];
                      if (dco) {
                          BOOL isBinary = [type isEqualToString:@"binary"];
                          NSData *bytes = isBinary ? [[NSData alloc] initWithBase64EncodedString:data options:0]
                                                   : [data dataUsingEncoding:NSUTF8StringEncoding];
                          RTCDataBuffer *buffer = [[RTCDataBuffer alloc] initWithData:bytes isBinary:isBinary];
                          [dco.channel sendData:buffer];
                      }
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
    return nil;
}

#pragma mark - DataChannelObserverDelegate methods

// Called when the data channel state has changed.
- (void)dataChannelDidChangeState:(DataChannelObserver *)dco {
    RTCDataChannel *channel = dco.channel;
    NSDictionary *event = @{
        @"reactTag" : dco.reactTag,
        @"peerConnectionId" : dco.peerConnectionId,
        @"id" : @(channel.channelId),
        @"state" : [self stringForDataChannelState:channel.readyState]
    };
    [self sendEventWithName:kEventDataChannelStateChanged body:event];
}

// Called when a data buffer was successfully received.
- (void)dataChannel:(DataChannelObserver *)dco didReceiveMessageWithBuffer:(RTCDataBuffer *)buffer {
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
        @"reactTag" : dco.reactTag,
        @"peerConnectionId" : dco.peerConnectionId,
        @"type" : type,
        // XXX NSDictionary will crash the process upon
        // attempting to insert nil. Such behavior is
        // unacceptable given that protection in such a
        // scenario is extremely simple.
        @"data" : (data ? data : [NSNull null])
    };
    [self sendEventWithName:kEventDataChannelReceiveMessage body:event];
}

- (void)dataChannel:(DataChannelObserver *)dco didChangeBufferedAmount:(uint64_t)amount {
    NSDictionary *event = @{
        @"reactTag" : dco.reactTag,
        @"peerConnectionId" : dco.peerConnectionId,
        @"bufferedAmount" : [NSNumber numberWithUnsignedLongLong:amount]
    };
    [self sendEventWithName:kEventDataChannelDidChangeBufferedAmount body:event];
}

@end
