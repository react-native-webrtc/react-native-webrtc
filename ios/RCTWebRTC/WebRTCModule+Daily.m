//
//  WebRTCModule+Daily.m
//  react-native-webrtc
//
//  Created by daily-co on 7/10/20.
//

#import "WebRTCModule.h"

#import <objc/runtime.h>
#import <WebRTC/RTCAudioSession.h>
#import <WebRTC/RTCAudioSessionConfiguration.h>

NSString *const AUDIO_MODE_VIDEO_CALL = @"video";
NSString *const AUDIO_MODE_VOICE_CALL = @"voice";
NSString *const AUDIO_MODE_IDLE = @"idle";

@interface WebRTCModule (Daily)
@property (nonatomic, strong) NSTimer *audioModeRetryTimer;
@end

@implementation WebRTCModule (Daily)

#pragma mark - setDailyAudioMode

- (void)setAudioModeRetryTimer:(NSTimer *)audioModeRetryTimer {
  objc_setAssociatedObject(self,
                           @selector(audioModeRetryTimer),
                           audioModeRetryTimer,
                           OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSTimer *)audioModeRetryTimer {
  return  objc_getAssociatedObject(self, @selector(audioModeRetryTimer));
}

RCT_EXPORT_METHOD(setDailyAudioMode:(NSString *)audioMode) {
  // Validate input
  if (![@[AUDIO_MODE_VIDEO_CALL, AUDIO_MODE_VOICE_CALL, AUDIO_MODE_IDLE] containsObject:audioMode]) {
    NSLog(@"[Daily] invalid argument to setDailyAudioMode: %@", audioMode);
    return;
  }
  
  // Cancel retry timer (if any) for setting the in-call audio mode
  [self.audioModeRetryTimer invalidate];
  
  // Do nothing if we're attempting to "unset" the in-call audio mode (for now
  // it doesn't seem like there's anything to do).
  if ([audioMode isEqualToString:AUDIO_MODE_IDLE]) {
    return;
  }
  
  RTCAudioSession *audioSession = RTCAudioSession.sharedInstance;
  
  // If audioSession isn't active, configuring it won't work.
  // Instead, schedule a timer to try again.
  if (!audioSession.isActive) {
    self.audioModeRetryTimer = [NSTimer timerWithTimeInterval:1 repeats:NO block:^(NSTimer * _Nonnull timer) {
      [self setDailyAudioMode:audioMode];
    }];
    [[NSRunLoop mainRunLoop] addTimer:self.audioModeRetryTimer
                              forMode:NSRunLoopCommonModes];
    return;
  }
  
  [self applyAudioMode:audioMode toSession:audioSession];
}

- (void)applyAudioMode:(NSString *)audioMode toSession:(RTCAudioSession *)audioSession {
  [audioSession lockForConfiguration];
  
  RTCAudioSessionConfiguration *config = [[RTCAudioSessionConfiguration alloc] init];
  config.category = AVAudioSessionCategoryPlayAndRecord;
  config.mode = ([audioMode isEqualToString:AUDIO_MODE_VIDEO_CALL] ?
                 AVAudioSessionModeVideoChat :
                 AVAudioSessionModeVoiceChat);
  // Ducking other apps' audio implicitly enables allowing mixing audio with
  // other apps, which allows this app to stay alive in the backgrounnd during
  // a call (assuming it has the voip background mode set).
  AVAudioSessionCategoryOptions categoryOptions = (AVAudioSessionCategoryOptionAllowBluetooth |
                                                   AVAudioSessionCategoryOptionDuckOthers);
  if ([audioMode isEqualToString:AUDIO_MODE_VIDEO_CALL]) {
    categoryOptions |= AVAudioSessionCategoryOptionDefaultToSpeaker;
  }
  config.categoryOptions = categoryOptions;
  
  NSError *error;
  [audioSession setConfiguration:config error:&error];
  
  [audioSession unlockForConfiguration];
  
  if (error) {
    NSLog(@"[Daily] error applying in-call audio mode %@: %@", audioMode, error);
  }
}

@end
