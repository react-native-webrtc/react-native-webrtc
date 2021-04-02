//
//  PlaybackModeModule.m
//  react-native-webrtc
//
//  Created by Dipesh Dulal on 02/04/2021.
//

#import <Foundation/Foundation.h>

#import <Foundation/Foundation.h>
#import "PlaybackModeModule.h"

#import <WebRTC/RTCAudioSession.h>

@implementation PlaybackModeModule

RCT_EXPORT_MODULE()
    
RCT_EXPORT_METHOD(setPlaybackMode) {
    RTCAudioSession* session = [RTCAudioSession sharedInstance];
    session.isPlaybackOnly = true;
}

RCT_EXPORT_METHOD(resetPlaybackMode) {
    RTCAudioSession* session = [RTCAudioSession sharedInstance];
    session.isPlaybackOnly = false;
}

@end
