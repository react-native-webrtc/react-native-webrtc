#if TARGET_OS_IOS

#import "AudioSessionGuard.h"

#import <AVFoundation/AVFoundation.h>
#import <React/RCTLog.h>
#import <WebRTC/RTCAudioSession.h>
#import <objc/runtime.h>

static __thread BOOL webRTCIsSettingActive = NO;

#pragma mark - C functions

static BOOL shouldAllowSetActive(BOOL active) {
    if (active || webRTCIsSettingActive || ![RTCAudioSession sharedInstance].isActive) {
        return YES;
    }
    RCTLogInfo(@"[AudioSessionGuard] Ignoring an AVAudioSession deactivation while WebRTC uses the audio session");
    return NO;
}

static void swapMethods(SEL original, SEL replacement) {
    Class sessionClass = [AVAudioSession class];
    method_exchangeImplementations(class_getInstanceMethod(sessionClass, original),
                                   class_getInstanceMethod(sessionClass, replacement));
}

#pragma mark - AVAudioSession (AudioSessionGuard)

@implementation AVAudioSession (AudioSessionGuard)
- (BOOL)rnwebrtc_setActive:(BOOL)active withOptions:(AVAudioSessionSetActiveOptions)options error:(NSError **)error {
    if (shouldAllowSetActive(active)) {
        return [self rnwebrtc_setActive:active withOptions:options error:error];
    }
    return YES;
}

- (BOOL)rnwebrtc_setActive:(BOOL)active error:(NSError **)error {
    if (shouldAllowSetActive(active)) {
        return [self rnwebrtc_setActive:active error:error];
    }
    return YES;
}
@end

#pragma mark - AudioSessionGuard

@interface AudioSessionGuard ()<RTCAudioSessionDelegate>
@end

@implementation AudioSessionGuard

+ (void)activate {
    static AudioSessionGuard *guard;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        guard = [[AudioSessionGuard alloc] init];
        [[RTCAudioSession sharedInstance] addDelegate:guard];

        swapMethods(@selector(setActive:withOptions:error:), @selector(rnwebrtc_setActive:withOptions:error:));
        swapMethods(@selector(setActive:error:), @selector(rnwebrtc_setActive:error:));
    });
}

#pragma mark - RTCAudioSessionDelegate

- (void)audioSession:(RTCAudioSession *)audioSession willSetActive:(BOOL)active {
    webRTCIsSettingActive = YES;
}

- (void)audioSession:(RTCAudioSession *)audioSession didSetActive:(BOOL)active {
    webRTCIsSettingActive = NO;
}

- (void)audioSession:(RTCAudioSession *)audioSession failedToSetActive:(BOOL)active error:(NSError *)error {
    webRTCIsSettingActive = NO;
}

@end

#endif
