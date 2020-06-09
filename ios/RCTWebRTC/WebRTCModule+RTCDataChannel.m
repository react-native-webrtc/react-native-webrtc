#import <objc/runtime.h>

#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>

#import "WebRTCModule+RTCDataChannel.h"
#import "WebRTCModule+RTCPeerConnection.h"
#import <WebRTC/RTCDataChannelConfiguration.h>

@implementation RTCDataChannel (React)

- (NSNumber *)peerConnectionId
{
  return objc_getAssociatedObject(self, _cmd);
}

- (void)setPeerConnectionId:(NSNumber *)peerConnectionId
{
  objc_setAssociatedObject(self, @selector(peerConnectionId), peerConnectionId, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

@end

@implementation WebRTCModule (RTCDataChannel)

RCT_EXPORT_METHOD(createDataChannel:(nonnull NSNumber *)peerConnectionId
                              label:(NSString *)label
                             config:(RTCDataChannelConfiguration *)config
{
  RTCPeerConnection *peerConnection = self.peerConnections[peerConnectionId];
  RTCDataChannel *dataChannel = [peerConnection dataChannelForLabel:label configuration:config];
  if (dataChannel != nil && (dataChannel.readyState == RTCDataChannelStateConnecting
      || dataChannel.readyState == RTCDataChannelStateOpen)) {
    dataChannel.peerConnectionId = peerConnectionId;
    NSNumber *dataChannelId = [NSNumber numberWithInteger:config.channelId];
    peerConnection.dataChannels[dataChannelId] = dataChannel;
    dataChannel.delegate = self;
  }
})

RCT_EXPORT_METHOD(dataChannelClose:(nonnull NSNumber *)peerConnectionId
                     dataChannelId:(nonnull NSNumber *)dataChannelId
{
  RTCPeerConnection *peerConnection = self.peerConnections[peerConnectionId];
  NSMutableDictionary *dataChannels = peerConnection.dataChannels;
  RTCDataChannel *dataChannel = dataChannels[dataChannelId];
  [dataChannel close];
  [dataChannels removeObjectForKey:dataChannelId];
})

RCT_EXPORT_METHOD(dataChannelSend:(nonnull NSNumber *)peerConnectionId
                    dataChannelId:(nonnull NSNumber *)dataChannelId
                             data:(NSString *)data
                             type:(NSString *)type
{
  RTCPeerConnection *peerConnection = self.peerConnections[peerConnectionId];
  RTCDataChannel *dataChannel = peerConnection.dataChannels[dataChannelId];
  NSData *bytes = [type isEqualToString:@"binary"] ?
    [[NSData alloc] initWithBase64EncodedString:data options:0] :
    [data dataUsingEncoding:NSUTF8StringEncoding];
  RTCDataBuffer *buffer = [[RTCDataBuffer alloc] initWithData:bytes isBinary:[type isEqualToString:@"binary"]];
  [dataChannel sendData:buffer];
})

- (NSString *)stringForDataChannelState:(RTCDataChannelState)state
{
  switch (state) {
    case RTCDataChannelStateConnecting: return @"connecting";
    case RTCDataChannelStateOpen: return @"open";
    case RTCDataChannelStateClosing: return @"closing";
    case RTCDataChannelStateClosed: return @"closed";
  }
  return nil;
}

#pragma mark - RTCDataChannelDelegate methods

// Called when the data channel state has changed.
- (void)dataChannelDidChangeState:(RTCDataChannel*)channel
{
  NSDictionary *event = @{@"id": @(channel.channelId),
                          @"peerConnectionId": channel.peerConnectionId,
                          @"state": [self stringForDataChannelState:channel.readyState]};
  [self sendEventWithName:kEventDataChannelStateChanged body:event];
}

// Called when a data buffer was successfully received.
- (void)dataChannel:(RTCDataChannel *)channel didReceiveMessageWithBuffer:(RTCDataBuffer *)buffer
{
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
    data = [[NSString alloc] initWithData:buffer.data
                                 encoding:NSUTF8StringEncoding];
  }
  NSDictionary *event = @{@"id": @(channel.channelId),
                          @"peerConnectionId": channel.peerConnectionId,
                          @"type": type,
                          // XXX NSDictionary will crash the process upon
                          // attempting to insert nil. Such behavior is
                          // unacceptable given that protection in such a
                          // scenario is extremely simple.
                          @"data": (data ? data : [NSNull null])};
  [self sendEventWithName:kEventDataChannelReceiveMessage body:event];
}

@end
