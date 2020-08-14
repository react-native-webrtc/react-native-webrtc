//
//  WebRTCModule+Daily.m
//  react-native-webrtc
//
//  Created by daily-co on 7/10/20.
//

#import "WebRTCModule.h"

#import <WebRTC/RTCAudioSession.h>
#import <WebRTC/RTCAudioSessionConfiguration.h>

@implementation WebRTCModule (Daily)

#pragma mark - setDailyDefaultAudioMode

RCT_EXPORT_METHOD(setDailyInCallAudioMode:(BOOL)setInCallAudioMode) {
  // For now it doesn't seem like we need to do anything to "unset" the in-call
  // audio mode, so just return.
  if (!setInCallAudioMode) {
    return;
  }
  
  RTCAudioSession *audioSession = RTCAudioSession.sharedInstance;
  
  if ([audioSession.category isEqualToString:AVAudioSessionCategoryPlayAndRecord] &&
      [audioSession.mode isEqualToString:AVAudioSessionModeVideoChat]) {
    return;
  }
  
  [audioSession lockForConfiguration];
  
  RTCAudioSessionConfiguration *config = [[RTCAudioSessionConfiguration alloc] init];
  config.category = AVAudioSessionCategoryPlayAndRecord;
  config.mode = AVAudioSessionModeVideoChat;
  config.categoryOptions = AVAudioSessionCategoryOptionAllowBluetooth;
  
  NSError *error;
  [audioSession setConfiguration:config error:&error];
  
  [audioSession unlockForConfiguration];
  
  if (error) {
    NSLog(@"[Daily] setDailyDefaultAudioMode error: %@", error);
  }
}

@end
