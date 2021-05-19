//
//  WebRTCModule+RTCPeerConnection.h
//
//  Created by one on 2015/9/24.
//  Copyright © 2015 One. All rights reserved.
//

#import "DataChannelWrapper.h"
#import "WebRTCModule.h"
#import <WebRTC/RTCPeerConnection.h>

@interface RTCPeerConnection (React)

@property (nonatomic, strong) NSNumber *reactTag;
@property (nonatomic, strong) NSMutableDictionary<NSString *, DataChannelWrapper *> *dataChannels;
@property (nonatomic, strong) NSMutableDictionary<NSString *, RTCMediaStream *> *remoteStreams;
@property (nonatomic, strong) NSMutableDictionary<NSString *, RTCMediaStreamTrack *> *remoteTracks;
@property (nonatomic, weak) id webRTCModule;

@end

@interface WebRTCModule (RTCPeerConnection) <RTCPeerConnectionDelegate>

@end
