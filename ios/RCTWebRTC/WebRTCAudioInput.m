//
//  WebRTCAudioInput.m
//  sharekey
//
//  Created by  Denis on 3.12.24.
//  Copyright © 2024 Sharekey. All rights reserved.
//

#import "WebRTCAudioInput.h"

@interface WebRTCAudioInput()



@end

@implementation WebRTCAudioInput
+ (instancetype)speaker {
  return [[self alloc] initWithPortName:@"Speaker"];
}

- (AVAudioSessionMode)mode {
  if (self.portName == PortNameSpeaker) {
    return AVAudioSessionModeVideoChat;
  } else {
    return AVAudioSessionModeVoiceChat;
  }
}

- (AVAudioSessionCategory)category {
  return AVAudioSessionCategoryPlayAndRecord;
}

- (AVAudioSessionCategoryOptions)categoryOptions {
  switch (self.portName) {
    case PortNameBluetooth:
      return AVAudioSessionCategoryOptionAllowBluetooth | AVAudioSessionCategoryOptionAllowBluetoothA2DP;
    case PortNameSpeaker:
      return AVAudioSessionCategoryOptionDefaultToSpeaker;
    default:
      return 0;
  }
}

- (instancetype)initWithPortName:(NSString *)portName {
  self = [super init];
  if (self) {
    self.portName = [PortNameHelper portNameFromString:portName];
  }
  return self;
}

- (instancetype)initWithPortDescription:(AVAudioSessionPortDescription *)portDescription {
  self = [super init];
  
  if (self) {
    self.name = portDescription.portName;
    self.portName = [PortNameHelper portNameFromString:portDescription.portName];
    self.portDescription = portDescription;
    
    if (self.portName == PortNamePhone || self.portName == PortNameSpeaker) {
      self.name = @"";
    }
  }
  
  return self;
}
@end

