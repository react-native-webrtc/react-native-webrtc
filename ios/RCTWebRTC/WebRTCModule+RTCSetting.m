//
//  WebRTCModule+RTCSetting.m
//
//  Created by one on 2015/10/29.
//  Copyright © 2015 One. All rights reserved.
//

#import "WebRTCModule+RTCSetting.h"

@implementation WebRTCModule (RTCSetting)

RCT_EXPORT_METHOD(setAudioOutput:(NSString *)output)
{
  AVAudioSession* session = [AVAudioSession sharedInstance];
  if ([output isEqualToString:@"speaker"]) {
    [session overrideOutputAudioPort:AVAudioSessionPortOverrideSpeaker error:nil];
  } else if ([output isEqualToString:@"handset"]) {
    [session overrideOutputAudioPort:AVAudioSessionPortOverrideNone error:nil];
  }
}

RCT_EXPORT_METHOD(setKeepScreenOn:(BOOL)isOn)
{
  [UIApplication sharedApplication].idleTimerDisabled = isOn;
}

RCT_EXPORT_METHOD(setProximityScreenOff:(BOOL)enabled)
{
  [UIDevice currentDevice].proximityMonitoringEnabled = enabled;
}

@end
