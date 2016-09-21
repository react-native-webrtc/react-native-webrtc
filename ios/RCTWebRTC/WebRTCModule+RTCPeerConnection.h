//
//  WebRTCModule+RTCPeerConnection.h
//
//  Created by one on 2015/9/24.
//  Copyright © 2015 One. All rights reserved.
//

#import "WebRTCModule.h"
#import <WebRTC/RTCPeerConnection.h>

@interface RTCPeerConnection (React)

@property (nonatomic, strong) NSNumber *reactTag;

@end

@interface WebRTCModule (RTCPeerConnection) <RTCPeerConnectionDelegate>

@end
