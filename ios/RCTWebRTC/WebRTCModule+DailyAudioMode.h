//
//  WebRTCModule+DevicesManager.m
//  react-native-webrtc
//
//  Created by Filipi Fuchter on 01/04/22.
//

#import "WebRTCModule.h"

// audio modes
static NSString * _Nonnull const AUDIO_MODE_VIDEO_CALL = @"video";
static NSString * _Nonnull const AUDIO_MODE_VOICE_CALL = @"voice";
static NSString * _Nonnull const AUDIO_MODE_IDLE = @"idle";
static NSString * _Nonnull const AUDIO_MODE_USER_SPECIFIED_ROUTE = @"user_specified";

@interface WebRTCModule (DailyAudioMode)

@property (nonatomic, strong) NSString *audioMode;

- (void)setAudioMode:(nonnull NSString*)audioMode;

@end
