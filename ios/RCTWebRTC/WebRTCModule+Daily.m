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

@interface WebRTCModule (Daily)
@property (nonatomic, strong) NSTimer *inCallAudioModeRetryTimer;
@end

@implementation WebRTCModule (Daily)

#pragma mark - setDailyDefaultAudioMode

- (void)setInCallAudioModeRetryTimer:(NSTimer *)inCallAudioModeRetryTimer {
  objc_setAssociatedObject(self,
                           @selector(inCallAudioModeRetryTimer),
                           inCallAudioModeRetryTimer,
                           OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSTimer *)inCallAudioModeRetryTimer {
  return  objc_getAssociatedObject(self, @selector(inCallAudioModeRetryTimer));
}

RCT_EXPORT_METHOD(setDailyInCallAudioMode:(BOOL)setInCallAudioMode) {
  // Cancel retry timer (if any) for setting the in-call audio mode
  [self.inCallAudioModeRetryTimer invalidate];
  
  // Do nothing if we're attempting to "unset" the in-call audio mode (for now
  // it doesn't seem like there's anything to do).
  if (!setInCallAudioMode) {
    return;
  }
  
  RTCAudioSession *audioSession = RTCAudioSession.sharedInstance;
  
  // If audioSession isn't active, configuring it won't work.
  // Instead, schedule a timer to try again.
  if (!audioSession.isActive) {
    self.inCallAudioModeRetryTimer = [NSTimer timerWithTimeInterval:1 repeats:NO block:^(NSTimer * _Nonnull timer) {
      [self setDailyInCallAudioMode:YES];
    }];
    [[NSRunLoop mainRunLoop] addTimer:self.inCallAudioModeRetryTimer
                              forMode:NSRunLoopCommonModes];
    return;
  }
  
  [self applyInCallAudioModeToSession:audioSession];
}

- (void)applyInCallAudioModeToSession:(RTCAudioSession *)audioSession {
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
