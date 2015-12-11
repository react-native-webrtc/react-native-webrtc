//
//  WebRTCModule.h
//
//  Created by one on 2015/9/24.
//  Copyright © 2015 One. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "RCTBridgeModule.h"
#import "RTCMediaStream.h"
#import "RTCPeerConnectionFactory.h"
#import "RCTConvert.h"
#import "RTCPeerConnection.h"
#import <AVFoundation/AVFoundation.h>
#import "RCTConvert.h"

@interface WebRTCModule : NSObject <RCTBridgeModule>

@property (nonatomic, strong) RTCPeerConnectionFactory *peerConnectionFactory;
@property (nonatomic, strong) AVCaptureDevice *videoDevice;

@property (nonatomic, strong) NSMutableDictionary<NSNumber *, RTCPeerConnection *> *peerConnections;
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, RTCMediaStream *> *mediaStreams;
@property (nonatomic) NSInteger mediaStreamId;

@end

@interface RCTConvert (RTCStates)

@end
