#import "RCTConvert+WebRTC.h"
#import "RTCTypes.h"

@implementation RCTConvert (WebRTC)

+ (RTCDataChannelInit *)RTCDataChannelInit:(id)json
{
  if (!json) {
    return nil;
  }
  if ([json isKindOfClass:[NSDictionary class]]) {
    RTCDataChannelInit *init = [RTCDataChannelInit new];

    if (json[@"ordered"]) {
      init.isOrdered = [RCTConvert BOOL:json[@"ordered"]];
    }
    if (json[@"maxRetransmitTime"]) {
      init.maxRetransmitTimeMs = [RCTConvert NSInteger:json[@"maxRetransmitTime"]];
    }
    if (json[@"maxRetransmits"]) {
      init.maxRetransmits = [RCTConvert NSInteger:json[@"maxRetransmits"]];
    }
    if (json[@"negotiated"]) {
      init.isNegotiated = [RCTConvert NSInteger:json[@"negotiated"]];
    }
    if (json[@"streamId"]) {
      init.streamId = [RCTConvert NSInteger:json[@"streamId"]];
    }
    if (json[@"protocol"]) {
      init.protocol = [RCTConvert NSString:json[@"protocol"]];
    }
    return init;
  }
  return nil;
}

@end
