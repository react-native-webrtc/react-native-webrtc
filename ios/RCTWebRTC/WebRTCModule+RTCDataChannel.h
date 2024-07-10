#import "DataChannelObserver.h"
#import "WebRTCModule.h"

@interface RTCDataChannel (React)

@property(nonatomic, strong) NSNumber *peerConnectionId;

@end

@interface WebRTCModule (RTCDataChannel)<DataChannelObserverDelegate>

- (NSString *)stringForDataChannelState:(RTCDataChannelState)state;

@end
