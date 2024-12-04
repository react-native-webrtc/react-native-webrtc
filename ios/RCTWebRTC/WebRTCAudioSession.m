//
//  WebRTCAudioSession.m
//  sharekey
//
//  Created by  Denis on 3.12.24.
//  Copyright © 2024 Sharekey. All rights reserved.
//

#import "WebRTCAudioSession.h"
#import <AVFoundation/AVFoundation.h>

@interface WebRTCAudioSession()<RTCAudioSessionDelegate>

@property (nonatomic) RTCAudioSession* rtcAudioSession;

@end

@implementation WebRTCAudioSession

+ (id)shared {
    static WebRTCAudioSession *sharedManager = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
      sharedManager = [[self alloc] init];
    });
    return sharedManager;
}

- (instancetype)init
{
  self = [super init];
  if (self) {
    _rtcAudioSession = [RTCAudioSession sharedInstance];
    [_rtcAudioSession addDelegate:self];
  }
  return self;
}

- (void)initialize {
  if (!self.useCallkit) {
    [self.rtcAudioSession lockForConfiguration];
    
    self.rtcAudioSession.useManualAudio = YES;
    self.rtcAudioSession.isAudioEnabled = YES;
    
    RTCAudioSessionConfiguration* configuration = [WebRTCAudioSession defaultConfiguration];
    [self.rtcAudioSession setConfiguration:configuration error:nil];
    
    
    [self.rtcAudioSession unlockForConfiguration];
  }
}

- (void)configure:(BOOL) speakerOn {
  [self.rtcAudioSession lockForConfiguration];
  
  RTCAudioSessionConfiguration* configuration = [RTCAudioSessionConfiguration webRTCConfiguration];
  
  if (speakerOn) {
    configuration.mode = AVAudioSessionModeVideoChat;
    configuration.categoryOptions += AVAudioSessionCategoryOptionDefaultToSpeaker;
  } else {
    configuration.mode = AVAudioSessionModeVoiceChat;
    configuration.categoryOptions -= AVAudioSessionCategoryOptionDefaultToSpeaker;
  }
  
  NSError* error;
  
  [self.rtcAudioSession setConfiguration:configuration error:&error];
  
  if (error) {
    NSLog(@"WebRTCAudioSession configure %@", error.localizedDescription);
  }
  
  [self.rtcAudioSession unlockForConfiguration];
}

- (void)audioSessionDidActivate:(AVAudioSession *) session {
  [self.rtcAudioSession audioSessionDidActivate:session];
  self.rtcAudioSession.isAudioEnabled = YES;
}

- (void)audioSessionDidDeactivate:(AVAudioSession *) session {
  self.useCallkit = NO;
  [self.rtcAudioSession audioSessionDidDeactivate:session];
  self.rtcAudioSession.isAudioEnabled = NO;
}

- (void)activateCallKitAudioSession {
  RTCAudioSessionConfiguration* configuration = [RTCAudioSessionConfiguration webRTCConfiguration];
  configuration.mode = AVAudioSessionModeVoiceChat;
  configuration.category = AVAudioSessionCategoryPlayAndRecord;
  configuration.outputNumberOfChannels = 1;
  configuration.categoryOptions = AVAudioSessionCategoryOptionDuckOthers
  | AVAudioSessionCategoryOptionMixWithOthers
  | AVAudioSessionCategoryOptionAllowBluetooth
  | AVAudioSessionCategoryOptionAllowBluetoothA2DP;
  
  NSError* error;
  
  [self.rtcAudioSession lockForConfiguration];
  [self.rtcAudioSession setConfiguration:configuration error:&error];
  [self.rtcAudioSession setActive:YES error:nil];
  
  if (error) {
    NSLog(@"WebRTCAudioSession activateCallKitAudioSession %@", error.localizedDescription);
  }
  
  [self.rtcAudioSession unlockForConfiguration];
}

- (void)setAudioSessionEnabled:(BOOL) enabled {
  [self.rtcAudioSession lockForConfiguration];
  self.rtcAudioSession.isAudioEnabled = enabled;
  [self.rtcAudioSession unlockForConfiguration];
}

- (void)closeConnection:(void (^)(void)) block {
  [self setAudioSessionEnabled:NO];
  block();
  [self setAudioSessionEnabled:YES];
}

+ (RTCAudioSessionConfiguration *)defaultConfiguration {
  RTCAudioSessionConfiguration* configuration = [RTCAudioSessionConfiguration webRTCConfiguration];
  configuration.mode = AVAudioSessionModeVideoChat;
  configuration.category = AVAudioSessionCategoryPlayAndRecord;
  configuration.categoryOptions = AVAudioSessionCategoryOptionAllowBluetooth
  | AVAudioSessionCategoryOptionAllowBluetoothA2DP
  | AVAudioSessionCategoryOptionMixWithOthers;
  
  return configuration;
}

- (void)configureFromInput:(WebRTCAudioInput *) input {
  [self.rtcAudioSession lockForConfiguration];
  
  AVAudioSession* myAudioSession = [AVAudioSession sharedInstance];
  RTCAudioSessionConfiguration* configuration = [RTCAudioSessionConfiguration webRTCConfiguration];
  
  configuration.category = input.category;
  configuration.categoryOptions = input.categoryOptions;
  configuration.mode = input.mode;
  
  [RTCAudioSessionConfiguration setWebRTCConfiguration:configuration];
  
  NSError* error;
  
  [self.rtcAudioSession setMode:input.mode error:&error];
  [self.rtcAudioSession overrideOutputAudioPort:(input.categoryOptions & AVAudioSessionCategoryOptionDefaultToSpeaker) ? AVAudioSessionPortOverrideSpeaker : AVAudioSessionPortOverrideNone error:&error];
  [self.rtcAudioSession setPreferredInput:input.portDescription error:&error];
  
  if (error) {
    NSLog(@"WebRTCAudioSession configureFromInput %@", error.localizedDescription);
  }
  
  [self.rtcAudioSession unlockForConfiguration];
}

@end
