#import "WebRTCModule.h"
#import <WebRTC/RTCDataChannel.h>

@interface RTCDataChannel (React)

@property (nonatomic, strong) NSNumber *peerConnectionId;
@property (nonatomic, strong) NSNumber *originDataChannelId;

@end

@interface WebRTCModule (RTCDataChannel) <RTCDataChannelDelegate>

@end
