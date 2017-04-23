//
//  WebRTCModule.m
//
//  Created by one on 2015/9/24.
//  Copyright © 2015 One. All rights reserved.
//

#import <UIKit/UIKit.h>

#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTUtils.h>

#import "WebRTCModule.h"

@interface WebRTCModule ()

@end

@implementation WebRTCModule

@synthesize bridge = _bridge;

- (instancetype)init
{
  self = [super init];
  if (self) {
    _peerConnectionFactory = [RTCPeerConnectionFactory new];
//    [RTCPeerConnectionFactory initializeSSL];

    _peerConnections = [NSMutableDictionary new];
    _remoteStreams = [NSMutableDictionary new];
    _remoteTracks = [NSMutableDictionary new];
    _localStreams = [NSMutableDictionary new];
    _localTracks = [NSMutableDictionary new];
  }
  return self;
}

- (void)dealloc
{
  [_remoteTracks removeAllObjects];
  _remoteTracks = nil;
  [_remoteStreams removeAllObjects];
  _remoteStreams = nil;
  [_localTracks removeAllObjects];
  _localTracks = nil;
  [_localStreams removeAllObjects];
  _localStreams = nil;

  for (NSNumber *peerConnectionId in _peerConnections) {
    RTCPeerConnection *peerConnection = _peerConnections[peerConnectionId];
    peerConnection.delegate = nil;
    [peerConnection close];
  }
  [_peerConnections removeAllObjects];

  _peerConnectionFactory = nil;
}

RCT_EXPORT_MODULE();

- (dispatch_queue_t)methodQueue
{
  return dispatch_get_main_queue();
}

@end
