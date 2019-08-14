//
//  AudioSession.m
//  testing
//
//  Created by Robert Barclay on 7/30/19.
//

@import React;

@interface RCT_EXTERN_MODULE(WebRTCAudioSession, NSObject)

#pragma mark - Audio session configuration -

RCT_EXTERN_METHOD(engageVoipAudioSession:(RCTPromiseResolveBlock _Nonnull)resolve rejecter:(RCTPromiseRejectBlock _Nonnull)reject);
RCT_EXTERN_METHOD(engageVideoAudioSession:(RCTPromiseResolveBlock _Nonnull)resolve rejecter:(RCTPromiseRejectBlock _Nonnull)reject);
RCT_EXTERN_METHOD(disengageAudioSession:(RCTPromiseResolveBlock _Nonnull)resolve rejecter:(RCTPromiseRejectBlock _Nonnull)reject);

#pragma mark - Manual Audio -

RCT_EXTERN_METHOD(setManualAudio:(BOOL)manualAudio);
RCT_EXTERN_METHOD(isManualAudio);
RCT_EXTERN_METHOD(isAudioEnabled);
RCT_EXTERN_METHOD(startAudio:(RCTPromiseResolveBlock _Nonnull)resolve rejecter:(RCTPromiseRejectBlock _Nonnull)reject);
RCT_EXTERN_METHOD(stopAudio:(RCTPromiseResolveBlock _Nonnull)resolve rejecter:(RCTPromiseRejectBlock _Nonnull)reject);

@end
