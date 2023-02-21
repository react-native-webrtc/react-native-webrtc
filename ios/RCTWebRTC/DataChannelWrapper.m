
#import "DataChannelWrapper.h"

#import <WebRTC/RTCDataChannel.h>

@interface DataChannelWrapper ()<RTCDataChannelDelegate>
@end

@implementation DataChannelWrapper

- (instancetype)initWithChannel:(RTCDataChannel *)channel reactTag:(NSString *)tag {
    self = [super init];
    if (self) {
        _channel = channel;
        _reactTag = tag;

        // Set ourselves as the deletagate.
        _channel.delegate = self;
    }

    return self;
}

- (void)dataChannel:(nonnull RTCDataChannel *)dataChannel didReceiveMessageWithBuffer:(nonnull RTCDataBuffer *)buffer {
    if (_delegate) {
        [_delegate dataChannel:self didReceiveMessageWithBuffer:buffer];
    }
}

- (void)dataChannelDidChangeState:(nonnull RTCDataChannel *)dataChannel {
    if (_delegate) {
        [_delegate dataChannelDidChangeState:self];
    }
}

- (void)dataChannel:(nonnull RTCDataChannel *)dataChannel didChangeBufferedAmount:(uint64_t)amount {
    if (_delegate) {
        [_delegate dataChannel:self didChangeBufferedAmount:amount];
    }
}

@end
