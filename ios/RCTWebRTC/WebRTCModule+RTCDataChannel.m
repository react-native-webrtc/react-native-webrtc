#import <objc/runtime.h>

#import "RCTBridge.h"
#import "RCTEventDispatcher.h"

#import "WebRTCModule+RTCDataChannel.h"

@implementation RTCDataChannel (React)

- (NSNumber *)reactTag
{
  return objc_getAssociatedObject(self, _cmd);
}

- (void)setReactTag:(NSNumber *)reactTag
{
  objc_setAssociatedObject(self, @selector(reactTag), reactTag, OBJC_ASSOCIATION_COPY_NONATOMIC);
}

@end

@implementation WebRTCModule (RTCDataChannel)

RCT_EXPORT_METHOD(dataChannelInit:(nonnull NSNumber *)peerConnectionId
                    dataChannelId:(nonnull NSNumber *)dataChannelId
                            label:(NSString *)label
                           config:(RTCDataChannelInit *)config
{
  RTCPeerConnection *peerConnection = self.peerConnections[peerConnectionId];
  RTCDataChannel *dataChannel = [peerConnection createDataChannelWithLabel:label config:config];
  dataChannel.reactTag = dataChannelId;
  dataChannel.delegate = self;
  self.dataChannels[dataChannelId] = dataChannel;
})

RCT_EXPORT_METHOD(dataChannelSend:(nonnull NSNumber *)dataChannelId
                             data:(NSString *)data
                             type:(NSString *)type
{
  RTCDataChannel *dataChannel = self.dataChannels[dataChannelId];
  NSData *bytes = [type isEqualToString:@"binary"] ?
    [[NSData alloc] initWithBase64EncodedString:data options:0] :
    [data dataUsingEncoding:NSUTF8StringEncoding];
  RTCDataBuffer *buffer = [[RTCDataBuffer alloc] initWithData:bytes isBinary:[type isEqualToString:@"binary"]];
  [dataChannel sendData:buffer];
})

RCT_EXPORT_METHOD(dataChannelClose:(nonnull NSNumber *)dataChannelId
{
  RTCDataChannel *dataChannel = self.dataChannels[dataChannelId];
  [dataChannel close];
  [self.dataChannels removeObjectForKey:dataChannelId];
})

- (NSString *)stringForDataChannelState:(RTCDataChannelState)state
{
  switch (state) {
    case kRTCDataChannelStateConnecting: return @"connecting";
    case kRTCDataChannelStateOpen: return @"open";
    case kRTCDataChannelStateClosing: return @"closing";
    case kRTCDataChannelStateClosed: return @"closed";
  }
  return nil;
}

#pragma mark - RTCDataChannelDelegate methods

// Called when the data channel state has changed.
- (void)channelDidChangeState:(RTCDataChannel*)channel
{
  NSDictionary *event = @{@"id": channel.reactTag,
                          @"state": [self stringForDataChannelState:channel.state]};
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"dataChannelStateChanged"
                                                  body:event];
}

// Called when a data buffer was successfully received.
- (void)channel:(RTCDataChannel*)channel didReceiveMessageWithBuffer:(RTCDataBuffer*)buffer
{
  NSString *data = buffer.isBinary ?
    [buffer.data base64EncodedStringWithOptions:0] :
    [NSString stringWithUTF8String:buffer.data.bytes];
  NSDictionary *event = @{@"id": channel.reactTag,
                          @"type": buffer.isBinary ? @"binary" : @"text",
                          @"data": data};
  [self.bridge.eventDispatcher sendDeviceEventWithName:@"dataChannelReceiveMessage"
                                                  body:event];
}

@end
