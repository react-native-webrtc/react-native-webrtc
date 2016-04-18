#import "WebRTCModule.h"
#import "RTCDataChannel.h"

@interface RTCDataChannel (React)

@property (nonatomic, strong) NSNumber *reactTag;

@end

@interface WebRTCModule (RTCDataChannel) <RTCDataChannelDelegate>

@end
