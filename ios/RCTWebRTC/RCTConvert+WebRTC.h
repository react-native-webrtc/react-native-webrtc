#import "RCTConvert.h"
#import "RTCDataChannel.h"

@interface RCTConvert (WebRTC)

+ (RTCDataChannelInit *)RTCDataChannelInit:(id)json;

@end
