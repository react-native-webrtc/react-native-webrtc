//
//  WebRTCManager.h
//  TestReact
//
//  Created by one on 2015/9/24.
//  Copyright © 2015年 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "RCTBridgeModule.h"
#import "RCTSparseArray.h"
#import "RTCMediaStream.h"
#import "RTCPeerConnectionFactory.h"
#import "RCTConvert.h"
#import "RTCPeerConnection.h"
#import "WebRTCStore.h"
#import <AVFoundation/AVFoundation.h>
#import "RCTConvert.h"

@interface WebRTCManager : NSObject <RCTBridgeModule>

@property (nonatomic, strong) RTCPeerConnectionFactory *peerConnectionFactory;
@property (nonatomic, strong) AVCaptureDevice *videoDevice;

@end

@interface RCTConvert (RTCStates)

@end
