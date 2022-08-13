//
//  SariskaRTCVideoRenderer.h
//  RCTWebRTC
//
//  Created by Dipak Sisodiya on 13/08/22.
//

#import <WebRTC/RTCVideoRenderer.h>
#import <WebRTC/RTCMediaStream.h>
#import <WebRTC/RTCVideoFrame.h>
#import <WebRTC/RTCVideoTrack.h>

@interface SariskaRTCVideoRenderer : NSObject<RTCVideoRenderer>

/**
 * The {@link RTCVideoTrack}, if any, which this instance renders.
 */

@property (nonatomic, strong) RTCVideoTrack *videoTrack;

- (instancetype)initWithSize:(CGSize)renderSize;

- (void)dispose;

@end