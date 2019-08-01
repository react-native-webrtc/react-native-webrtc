//
//  AudioSession.m
//  testing
//
//  Created by Robert Barclay on 7/30/19.
//

@import React;

@interface RCT_EXTERN_REMAP_MODULE(RCTAudioSession, AudioSession, NSObject)

#pragma mark - Audio session configuration -

RCT_EXTERN_METHOD(lockForConfiguration);
RCT_EXTERN_METHOD(unlockForConfiguration);

#pragma mark - Manual Audio -

RCT_EXTERN_METHOD(setManualAudio:(BOOL)manualAudio);
RCT_EXTERN_METHOD(isManualAudio);
RCT_EXTERN_METHOD(isAudioEnabled);
RCT_EXTERN_METHOD(startAudio:(RCTPromiseResolveBlock _Nonnull)resolve rejecter:(RCTPromiseRejectBlock _Nonnull)reject);
RCT_EXTERN_METHOD(stopAudio:(RCTPromiseResolveBlock _Nonnull)resolve rejecter:(RCTPromiseRejectBlock _Nonnull)reject);

@end
