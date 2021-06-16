#import <React/RCTConvert.h>
#import <WebRTC/RTCDataChannelConfiguration.h>
#import <WebRTC/RTCConfiguration.h>
#import <WebRTC/RTCIceServer.h>
#import <WebRTC/RTCSessionDescription.h>
#import <WebRTC/RTCIceCandidate.h>

@interface RCTConvert (WebRTC)

+ (RTCIceCandidate *)RTCIceCandidate:(id)json;
+ (RTCSessionDescription *)RTCSessionDescription:(id)json;
+ (RTCIceServer *)RTCIceServer:(id)json;
+ (RTCDataChannelConfiguration *)RTCDataChannelConfiguration:(id)json;
+ (RTCConfiguration *)RTCConfiguration:(id)json;

@end
