#import "WebRTCModule.h"
#import "DataChannelWrapper.h"

@interface RTCDataChannel (React)

@property (nonatomic, strong) NSNumber *peerConnectionId;

@end

@interface WebRTCModule (RTCDataChannel) <DataChannelWrapperDelegate>

- (NSString *)stringForDataChannelState:(RTCDataChannelState)state;

@end
