//
//  WebRTCModule.h
//
//  Created by one on 2015/9/24.
//  Copyright © 2015 One. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>

#import "RCTBridgeModule.h"
#import "RCTConvert.h"

#import "RTCDataChannel.h"
#import "RTCMediaStream.h"
#import "RTCPeerConnectionFactory.h"
#import "RTCPeerConnection.h"
#import "RTCAudioTrack.h"
#import "RTCVideoTrack.h"

@interface WebRTCModule : NSObject <RCTBridgeModule>

@property (nonatomic, strong) RTCPeerConnectionFactory *peerConnectionFactory;

@property (nonatomic, strong) NSMutableDictionary<NSNumber *, RTCPeerConnection *> *peerConnections;
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, RTCMediaStream *> *mediaStreams;
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, RTCMediaStreamTrack *> *tracks;
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, RTCDataChannel *> *dataChannels;
@property (nonatomic) NSInteger mediaStreamId;
@property (nonatomic) NSInteger trackId;

@end
