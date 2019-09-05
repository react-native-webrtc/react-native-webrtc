//
//  WebRTCModule.h
//
//  Created by one on 2015/9/24.
//  Copyright © 2015 One. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>

#import <React/RCTBridgeModule.h>
#import <React/RCTConvert.h>

#import <WebRTC/RTCMediaStream.h>
#import <WebRTC/RTCPeerConnectionFactory.h>
#import <WebRTC/RTCPeerConnection.h>
#import <WebRTC/RTCAudioTrack.h>
#import <WebRTC/RTCVideoTrack.h>
#import <WebRTC/RTCVideoDecoderFactory.h>
#import <WebRTC/RTCVideoEncoderFactory.h>

@interface WebRTCModule : NSObject <RCTBridgeModule>

@property(nonatomic, strong) dispatch_queue_t workerQueue;

@property (nonatomic, strong) RTCPeerConnectionFactory *peerConnectionFactory;

@property (nonatomic, strong) NSMutableDictionary<NSNumber *, RTCPeerConnection *> *peerConnections;
@property (nonatomic, strong) NSMutableDictionary<NSString *, RTCMediaStream *> *localStreams;
@property (nonatomic, strong) NSMutableDictionary<NSString *, RTCMediaStreamTrack *> *localTracks;

- (instancetype)initWithEncoderFactory:(id<RTCVideoEncoderFactory>)encoderFactory
                        decoderFactory:(id<RTCVideoDecoderFactory>)decoderFactory;

- (RTCMediaStream*)streamForReactTag:(NSString*)reactTag;

@end
