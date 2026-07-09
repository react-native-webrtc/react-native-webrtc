#import "AudioDeviceModuleObserver.h"
#import <React/RCTLog.h>
#import <os/log.h>

NS_ASSUME_NONNULL_BEGIN

// Upper bound on how long a delegate callback parks the native audio thread
// waiting for JS to respond. The wait used to be DISPATCH_TIME_FOREVER, which
// deadlocks the app if the JS thread is itself blocked inside a synchronous
// bridge call (e.g. peerConnectionAddTransceiver) that is transitively waiting
// on this same audio operation. Bounding the wait turns that permanent freeze
// into a recoverable stall: on timeout we return the default 0 ("proceed") so
// the engine operation degrades gracefully.
//
// Caveat for willEnableEngine: returning 0 here lets the engine proceed even
// though the JS handler (which configures/activates the AVAudioSession) never
// completed. libwebrtc re-validates the session *category* after this callback
// but not that it was *activated*, so a timeout on willEnableEngine can start
// the engine against an unconfigured session. That degraded-but-recoverable
// outcome is deliberately preferred over the unrecoverable deadlock.
static const int64_t kJSResponseTimeoutSeconds = 2;

// Returned from willEnable/didDisable when native automatic audio-session
// configuration fails, so libwebrtc rolls the engine operation back. Mirrors the
// SDK's kAudioEngineErrorFailedToConfigureAudioSession value.
static const NSInteger kAutomaticAudioSessionConfigError = -4100;

static os_log_t ADMObserverLog(void) {
    static os_log_t log;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        log = os_log_create("com.livekit.react-native-webrtc", "AudioDeviceModuleObserver");
    });
    return log;
}

@interface AudioDeviceModuleObserver ()

@property(weak, nonatomic) WebRTCModule *module;
@property(nonatomic, strong) dispatch_semaphore_t engineCreatedSemaphore;
@property(nonatomic, strong) dispatch_semaphore_t willEnableEngineSemaphore;
@property(nonatomic, strong) dispatch_semaphore_t willStartEngineSemaphore;
@property(nonatomic, strong) dispatch_semaphore_t didStopEngineSemaphore;
@property(nonatomic, strong) dispatch_semaphore_t didDisableEngineSemaphore;
@property(nonatomic, strong) dispatch_semaphore_t willReleaseEngineSemaphore;

@property(nonatomic, assign) NSInteger engineCreatedResult;
@property(nonatomic, assign) NSInteger willEnableEngineResult;
@property(nonatomic, assign) NSInteger willStartEngineResult;
@property(nonatomic, assign) NSInteger didStopEngineResult;
@property(nonatomic, assign) NSInteger didDisableEngineResult;
@property(nonatomic, assign) NSInteger willReleaseEngineResult;

// Monotonic id stamped on every event sent to JS, and the id of the round
// currently being awaited (0 = none). JS echoes the id back when it resolves;
// the observer only accepts a resolve whose id matches the in-flight round, so a
// late resolve from a round that already timed out cannot be misattributed to
// the next round. Both are guarded by @synchronized(self) since the send side
// runs on the native audio thread and the resolve side on the JS thread.
@property(nonatomic, assign) NSInteger requestIdSeq;
@property(nonatomic, assign) NSInteger awaitingRequestId;

// Whether the native auto-config path currently holds an
// RTCAudioSession activation. RTCAudioSession reference-counts activations
// (setActive:YES on an already-active session only bumps the count, and
// setActive:NO only deactivates at count 1), so the observer must activate at
// most once per hold and release exactly what it took, or the OS session leaks
// active forever. Only touched on the serial delegate (worker) thread, and kept
// atomic as cheap insurance against future cross-thread reads.
@property(atomic, assign) BOOL autoSessionHoldsActivation;

@end

@implementation AudioDeviceModuleObserver

- (instancetype)initWithWebRTCModule:(WebRTCModule *)module {
    self = [super init];
    if (self) {
        self.module = module;
        _engineCreatedSemaphore = dispatch_semaphore_create(0);
        _willEnableEngineSemaphore = dispatch_semaphore_create(0);
        _willStartEngineSemaphore = dispatch_semaphore_create(0);
        _didStopEngineSemaphore = dispatch_semaphore_create(0);
        _didDisableEngineSemaphore = dispatch_semaphore_create(0);
        _willReleaseEngineSemaphore = dispatch_semaphore_create(0);

        // Default every hook to active so a delegate callback that fires before JS
        // has reconciled its handler state (e.g. a recreated observer) still does the
        // bounded round trip rather than silently skipping a handler's veto. JS
        // reconciles these to the real handler state in setupListeners().
        _isEngineCreatedActive = YES;
        _isWillEnableEngineActive = YES;
        _isWillStartEngineActive = YES;
        _isDidStopEngineActive = YES;
        _isDidDisableEngineActive = YES;
        _isWillReleaseEngineActive = YES;
    }
    return self;
}

#pragma mark - Bounded JS round-trip

// Sends an event to JS and blocks the calling (native audio) thread until JS
// resolves the matching semaphore or kJSResponseTimeoutSeconds elapses.
//
// Each round is tagged with a unique requestId that JS echoes back on resolve;
// -resolveRequestId:... drops any resolve whose id does not match the in-flight
// round, so a late resolve from a previously timed-out round cannot signal this
// round's semaphore. The pre-send drain below is the remaining safety net: it
// clears a stray signal from the narrow case where a matching resolve raced the
// previous round past its timeout deadline (signalling after that round had
// already given up). No legitimate signal for *this* round can exist at drain
// time because the event has not been sent yet.
//
// If isActive is NO, returns 0 immediately without a JS round trip, avoiding the
// deadlock window when no handler is registered.
//
// Returns the JS-provided result on success, or 0 on timeout or when inactive.
- (NSInteger)sendEventAndWaitWithName:(NSString *)eventName
                                 body:(NSDictionary *)body
                            semaphore:(dispatch_semaphore_t)semaphore
                          resultBlock:(NSInteger (^)(void))resultBlock
                             isActive:(BOOL)isActive {
    if (!isActive) {
        // No handler registered, proceed immediately without JS round trip.
        // This avoids the deadlock window entirely.
        os_log_debug(ADMObserverLog(), "Skipping JS round-trip for %{public}@ (no handler registered)", eventName);
        return 0;
    }

    NSInteger requestId;
    @synchronized(self) {
        requestId = ++self.requestIdSeq;
        self.awaitingRequestId = requestId;
    }

    while (dispatch_semaphore_wait(semaphore, DISPATCH_TIME_NOW) == 0) {
        // Drain a stray signal left by a resolve that raced the previous round's timeout.
    }

    NSMutableDictionary *payload = [body mutableCopy];
    payload[@"requestId"] = @(requestId);
    [self.module sendEventWithName:eventName body:payload];

    dispatch_time_t deadline = dispatch_time(DISPATCH_TIME_NOW, kJSResponseTimeoutSeconds * NSEC_PER_SEC);
    if (dispatch_semaphore_wait(semaphore, deadline) != 0) {
        @synchronized(self) {
            // Stop accepting this round's resolve; if it arrives now it is stale.
            if (self.awaitingRequestId == requestId) {
                self.awaitingRequestId = 0;
            }
        }
        os_log_error(ADMObserverLog(),
                     "Timed out after %llds waiting for JS to respond to %{public}@; returning default 0",
                     (long long)kJSResponseTimeoutSeconds,
                     eventName);
        return 0;
    }

    return resultBlock();
}

#pragma mark - RTCAudioDeviceModuleDelegate

- (void)audioDeviceModule:(RTCAudioDeviceModule *)audioDeviceModule
    didReceiveSpeechActivityEvent:(RTCSpeechActivityEvent)speechActivityEvent {
    NSString *eventType = speechActivityEvent == RTCSpeechActivityEventStarted ? @"started" : @"ended";

    [self.module sendEventWithName:kEventAudioDeviceModuleSpeechActivity
                              body:@{
                                  @"event" : eventType,
                              }];

    RCTLog(@"[AudioDeviceModuleObserver] Speech activity event: %@", eventType);
}

- (NSInteger)audioDeviceModule:(RTCAudioDeviceModule *)audioDeviceModule didCreateEngine:(AVAudioEngine *)engine {
    BOOL isActive = self.isEngineCreatedActive;

    if (isActive) {
        RCTLog(@"[AudioDeviceModuleObserver] Engine created - waiting for JS response");
    }

    NSInteger result = [self sendEventAndWaitWithName:kEventAudioDeviceModuleEngineCreated
                                                 body:@{}
                                            semaphore:self.engineCreatedSemaphore
                                          resultBlock:^NSInteger {
                                              return self.engineCreatedResult;
                                          }
                                             isActive:isActive];

    if (isActive) {
        RCTLog(@"[AudioDeviceModuleObserver] Engine created - JS returned: %ld", (long)result);
    }
    return result;
}

- (NSInteger)audioDeviceModule:(RTCAudioDeviceModule *)audioDeviceModule
              willEnableEngine:(AVAudioEngine *)engine
              isPlayoutEnabled:(BOOL)isPlayoutEnabled
            isRecordingEnabled:(BOOL)isRecordingEnabled {
    // With no custom JS handler registered, configure the audio session natively
    // here instead of doing a JS round trip. This is the default path and avoids
    // parking the audio worker thread on the JS thread entirely. A custom handler
    // (isWillEnableEngineActive == YES) falls through to the bounded round trip.
    if (!self.isWillEnableEngineActive && self.automaticAudioSessionConfig != nil) {
        return [self applyAutomaticAudioSessionConfigForPlayout:isPlayoutEnabled recording:isRecordingEnabled];
    }

    BOOL isActive = self.isWillEnableEngineActive;

    if (isActive) {
        RCTLog(@"[AudioDeviceModuleObserver] Engine will enable - playout: %d, recording: %d - waiting for JS response",
               isPlayoutEnabled,
               isRecordingEnabled);
    }

    NSInteger result = [self sendEventAndWaitWithName:kEventAudioDeviceModuleEngineWillEnable
                                                 body:@{
                                                     @"isPlayoutEnabled" : @(isPlayoutEnabled),
                                                     @"isRecordingEnabled" : @(isRecordingEnabled),
                                                 }
                                            semaphore:self.willEnableEngineSemaphore
                                          resultBlock:^NSInteger {
                                              return self.willEnableEngineResult;
                                          }
                                             isActive:isActive];

    if (isActive) {
        RCTLog(@"[AudioDeviceModuleObserver] Engine will enable - JS returned: %ld", (long)result);

        AVAudioSession *audioSession = [AVAudioSession sharedInstance];
        RCTLog(@"[AudioDeviceModuleObserver] Audio session category: %@", audioSession.category);
    }

    return result;
}

- (NSInteger)audioDeviceModule:(RTCAudioDeviceModule *)audioDeviceModule
               willStartEngine:(AVAudioEngine *)engine
              isPlayoutEnabled:(BOOL)isPlayoutEnabled
            isRecordingEnabled:(BOOL)isRecordingEnabled {
    BOOL isActive = self.isWillStartEngineActive;

    if (isActive) {
        RCTLog(@"[AudioDeviceModuleObserver] Engine will start - playout: %d, recording: %d - waiting for JS response",
               isPlayoutEnabled,
               isRecordingEnabled);
    }

    NSInteger result = [self sendEventAndWaitWithName:kEventAudioDeviceModuleEngineWillStart
                                                 body:@{
                                                     @"isPlayoutEnabled" : @(isPlayoutEnabled),
                                                     @"isRecordingEnabled" : @(isRecordingEnabled),
                                                 }
                                            semaphore:self.willStartEngineSemaphore
                                          resultBlock:^NSInteger {
                                              return self.willStartEngineResult;
                                          }
                                             isActive:isActive];

    if (isActive) {
        RCTLog(@"[AudioDeviceModuleObserver] Engine will start - JS returned: %ld", (long)result);
    }
    return result;
}

- (NSInteger)audioDeviceModule:(RTCAudioDeviceModule *)audioDeviceModule
                 didStopEngine:(AVAudioEngine *)engine
              isPlayoutEnabled:(BOOL)isPlayoutEnabled
            isRecordingEnabled:(BOOL)isRecordingEnabled {
    BOOL isActive = self.isDidStopEngineActive;

    if (isActive) {
        RCTLog(@"[AudioDeviceModuleObserver] Engine did stop - playout: %d, recording: %d - waiting for JS response",
               isPlayoutEnabled,
               isRecordingEnabled);
    }

    NSInteger result = [self sendEventAndWaitWithName:kEventAudioDeviceModuleEngineDidStop
                                                 body:@{
                                                     @"isPlayoutEnabled" : @(isPlayoutEnabled),
                                                     @"isRecordingEnabled" : @(isRecordingEnabled),
                                                 }
                                            semaphore:self.didStopEngineSemaphore
                                          resultBlock:^NSInteger {
                                              return self.didStopEngineResult;
                                          }
                                             isActive:isActive];

    if (isActive) {
        RCTLog(@"[AudioDeviceModuleObserver] Engine did stop - JS returned: %ld", (long)result);
    }
    return result;
}

- (NSInteger)audioDeviceModule:(RTCAudioDeviceModule *)audioDeviceModule
              didDisableEngine:(AVAudioEngine *)engine
              isPlayoutEnabled:(BOOL)isPlayoutEnabled
            isRecordingEnabled:(BOOL)isRecordingEnabled {
    // Counterpart of willEnable - apply the native session policy (here the
    // deactivate-on-stop edge) without a JS round trip when no custom handler
    // is registered.
    if (!self.isDidDisableEngineActive && self.automaticAudioSessionConfig != nil) {
        return [self applyAutomaticAudioSessionConfigForPlayout:isPlayoutEnabled recording:isRecordingEnabled];
    }

    BOOL isActive = self.isDidDisableEngineActive;

    if (isActive) {
        RCTLog(@"[AudioDeviceModuleObserver] Engine did disable - playout: %d, recording: %d - waiting for JS response",
               isPlayoutEnabled,
               isRecordingEnabled);
    }

    NSInteger result = [self sendEventAndWaitWithName:kEventAudioDeviceModuleEngineDidDisable
                                                 body:@{
                                                     @"isPlayoutEnabled" : @(isPlayoutEnabled),
                                                     @"isRecordingEnabled" : @(isRecordingEnabled),
                                                 }
                                            semaphore:self.didDisableEngineSemaphore
                                          resultBlock:^NSInteger {
                                              return self.didDisableEngineResult;
                                          }
                                             isActive:isActive];

    if (isActive) {
        RCTLog(@"[AudioDeviceModuleObserver] Engine did disable - JS returned: %ld", (long)result);
    }
    return result;
}

- (NSInteger)audioDeviceModule:(RTCAudioDeviceModule *)audioDeviceModule willReleaseEngine:(AVAudioEngine *)engine {
    BOOL isActive = self.isWillReleaseEngineActive;

    if (isActive) {
        RCTLog(@"[AudioDeviceModuleObserver] Engine will release - waiting for JS response");
    }

    NSInteger result = [self sendEventAndWaitWithName:kEventAudioDeviceModuleEngineWillRelease
                                                 body:@{}
                                            semaphore:self.willReleaseEngineSemaphore
                                          resultBlock:^NSInteger {
                                              return self.willReleaseEngineResult;
                                          }
                                             isActive:isActive];

    if (isActive) {
        RCTLog(@"[AudioDeviceModuleObserver] Engine will release - JS returned: %ld", (long)result);
    }
    return result;
}

- (NSInteger)audioDeviceModule:(RTCAudioDeviceModule *)audioDeviceModule
                        engine:(AVAudioEngine *)engine
      configureInputFromSource:(nullable AVAudioNode *)source
                 toDestination:(AVAudioNode *)destination
                    withFormat:(AVAudioFormat *)format
                       context:(NSDictionary *)context {
    RCTLog(@"[AudioDeviceModuleObserver] Configure input - format: %@", format);
    return 0;
}

- (NSInteger)audioDeviceModule:(RTCAudioDeviceModule *)audioDeviceModule
                        engine:(AVAudioEngine *)engine
     configureOutputFromSource:(AVAudioNode *)source
                 toDestination:(nullable AVAudioNode *)destination
                    withFormat:(AVAudioFormat *)format
                       context:(NSDictionary *)context {
    RCTLog(@"[AudioDeviceModuleObserver] Configure output - format: %@", format);
    return 0;
}

- (void)audioDeviceModuleDidUpdateDevices:(RTCAudioDeviceModule *)audioDeviceModule {
    [self.module sendEventWithName:kEventAudioDeviceModuleDevicesUpdated body:@{}];

    RCTLog(@"[AudioDeviceModuleObserver] Devices updated");
}

#pragma mark - Resolve methods from JS

// Applies a JS response only if its requestId matches the round currently being
// awaited. A non-matching id means the round already timed out and moved on, so
// the response is dropped without touching the result or signalling — preventing
// a stale value from being handed to a later round.
- (void)resolveRequestId:(NSInteger)requestId store:(void (^)(void))store semaphore:(dispatch_semaphore_t)semaphore {
    @synchronized(self) {
        if (requestId != self.awaitingRequestId) {
            return;
        }
        self.awaitingRequestId = 0;
        store();
        // Signal inside the lock so this signal is always posted before the next
        // round's send-side @synchronized block can start. Otherwise a resolve
        // preempted between releasing the lock and signalling could, across a round
        // boundary, wake the next round and hand it this round's result. Keeping it
        // inside the lock also lets the next round's pre-send drain reliably clear
        // any stray signal left by a resolve that raced its own timeout.
        dispatch_semaphore_signal(semaphore);
    }
}

- (void)resolveEngineCreatedWithRequestId:(NSInteger)requestId result:(NSInteger)result {
    [self resolveRequestId:requestId
                     store:^{
                         self.engineCreatedResult = result;
                     }
                 semaphore:self.engineCreatedSemaphore];
}

- (void)resolveWillEnableEngineWithRequestId:(NSInteger)requestId result:(NSInteger)result {
    [self resolveRequestId:requestId
                     store:^{
                         self.willEnableEngineResult = result;
                     }
                 semaphore:self.willEnableEngineSemaphore];
}

- (void)resolveWillStartEngineWithRequestId:(NSInteger)requestId result:(NSInteger)result {
    [self resolveRequestId:requestId
                     store:^{
                         self.willStartEngineResult = result;
                     }
                 semaphore:self.willStartEngineSemaphore];
}

- (void)resolveDidStopEngineWithRequestId:(NSInteger)requestId result:(NSInteger)result {
    [self resolveRequestId:requestId
                     store:^{
                         self.didStopEngineResult = result;
                     }
                 semaphore:self.didStopEngineSemaphore];
}

- (void)resolveDidDisableEngineWithRequestId:(NSInteger)requestId result:(NSInteger)result {
    [self resolveRequestId:requestId
                     store:^{
                         self.didDisableEngineResult = result;
                     }
                 semaphore:self.didDisableEngineSemaphore];
}

- (void)resolveWillReleaseEngineWithRequestId:(NSInteger)requestId result:(NSInteger)result {
    [self resolveRequestId:requestId
                     store:^{
                         self.willReleaseEngineResult = result;
                     }
                 semaphore:self.willReleaseEngineSemaphore];
}

#pragma mark - Native automatic audio session configuration

// Applies the pushed audio-session policy on the worker thread with no JS round
// trip: configures on every active state, activates when the session is not
// already active, optionally deactivates on stop, and returns a non-zero error
// code on failure so libwebrtc rolls the operation back.
//
// Activation is decided against RTCAudioSession's own state rather than a mirror
// of past engine states, so a policy re-push mid-call (setupIOSAudioManagement
// called again), a path switch, or an interruption cannot desync it:
// - session already active (previous hold, custom JS path, or the app) ->
//   reconfigure only, never a second refcounted activation.
// - session inactive (fresh start, or the custom path deactivated it before a
//   switch back to the native path) -> activate and take the hold.
- (NSInteger)applyAutomaticAudioSessionConfigForPlayout:(BOOL)isPlayoutEnabled recording:(BOOL)isRecordingEnabled {
    NSDictionary *policy = self.automaticAudioSessionConfig;
    if (policy == nil) {
        return 0;
    }

    BOOL nowActive = isPlayoutEnabled || isRecordingEnabled;

    RTCAudioSession *session = [RTCAudioSession sharedInstance];
    [session lockForConfiguration];

    NSError *error = nil;
    if (!nowActive) {
        if (self.autoSessionHoldsActivation && [policy[@"deactivateOnStop"] boolValue]) {
            os_log_debug(ADMObserverLog(), "Native auto-config: deactivating audio session");
            [session setActive:NO error:&error];
            // RTCAudioSession decrements its activation count even when the OS
            // session is already inactive (e.g. an interruption cleared it) or the
            // call fails, so the hold is released in every outcome.
            self.autoSessionHoldsActivation = NO;
        }
    } else {
        // Recording uses the duplex (playAndRecord) config, while playout-only uses
        // the playback config. Both are supplied by the SDK in automaticAudioSessionConfig.
        NSDictionary *cfg = isRecordingEnabled ? policy[@"recording"] : policy[@"playout"];
        RTCAudioSessionConfiguration *rtcConfig = [RTCAudioSessionConfiguration webRTCConfiguration];
        if (cfg[@"audioCategory"] != nil) {
            rtcConfig.category = [self avAudioSessionCategoryFromString:cfg[@"audioCategory"]];
        }
        if (cfg[@"audioMode"] != nil) {
            rtcConfig.mode = [self avAudioSessionModeFromString:cfg[@"audioMode"]];
        }
        if (cfg[@"audioCategoryOptions"] != nil) {
            rtcConfig.categoryOptions = [self avAudioSessionCategoryOptionsFromStrings:cfg[@"audioCategoryOptions"]];
        }
        os_log_debug(ADMObserverLog(), "Native auto-config: setting category %{public}@", rtcConfig.category);
        [session setConfiguration:rtcConfig error:&error];
        if (error == nil && !session.isActive) {
            os_log_debug(ADMObserverLog(), "Native auto-config: activating audio session");
            [session setActive:YES error:&error];
            if (error == nil) {
                self.autoSessionHoldsActivation = YES;
            }
        }
    }

    [session unlockForConfiguration];

    if (error != nil) {
        os_log_error(ADMObserverLog(), "Native auto-config failed: %{public}@", error.localizedDescription);
        return kAutomaticAudioSessionConfigError;
    }

    return 0;
}

// TODO: this friendly-string -> AVAudioSession mapping mirrors AudioUtils in the
// LiveKit React Native SDK. Keep the accepted values in sync, or consolidate by
// pushing raw AVAudioSession values across the bridge instead.
- (NSString *)avAudioSessionCategoryFromString:(NSString *)value {
    static NSDictionary<NSString *, NSString *> *map;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        map = @{
            @"ambient" : AVAudioSessionCategoryAmbient,
            @"soloAmbient" : AVAudioSessionCategorySoloAmbient,
            @"playback" : AVAudioSessionCategoryPlayback,
            @"record" : AVAudioSessionCategoryRecord,
            @"playAndRecord" : AVAudioSessionCategoryPlayAndRecord,
            @"multiRoute" : AVAudioSessionCategoryMultiRoute,
        };
    });
    return map[value] ?: AVAudioSessionCategoryPlayAndRecord;
}

- (NSString *)avAudioSessionModeFromString:(NSString *)value {
    static NSDictionary<NSString *, NSString *> *map;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        map = @{
            @"default" : AVAudioSessionModeDefault,
            @"voiceChat" : AVAudioSessionModeVoiceChat,
            @"videoChat" : AVAudioSessionModeVideoChat,
            @"gameChat" : AVAudioSessionModeGameChat,
            @"videoRecording" : AVAudioSessionModeVideoRecording,
            @"measurement" : AVAudioSessionModeMeasurement,
            @"moviePlayback" : AVAudioSessionModeMoviePlayback,
            @"spokenAudio" : AVAudioSessionModeSpokenAudio,
        };
    });
    return map[value] ?: AVAudioSessionModeDefault;
}

- (AVAudioSessionCategoryOptions)avAudioSessionCategoryOptionsFromStrings:(NSArray<NSString *> *)options {
    AVAudioSessionCategoryOptions result = 0;
    for (NSString *option in options) {
        if ([option isEqualToString:@"mixWithOthers"]) {
            result |= AVAudioSessionCategoryOptionMixWithOthers;
        } else if ([option isEqualToString:@"duckOthers"]) {
            result |= AVAudioSessionCategoryOptionDuckOthers;
        } else if ([option isEqualToString:@"allowBluetooth"]) {
            result |= AVAudioSessionCategoryOptionAllowBluetooth;
        } else if ([option isEqualToString:@"allowBluetoothA2DP"]) {
            result |= AVAudioSessionCategoryOptionAllowBluetoothA2DP;
        } else if ([option isEqualToString:@"allowAirPlay"]) {
            result |= AVAudioSessionCategoryOptionAllowAirPlay;
        } else if ([option isEqualToString:@"defaultToSpeaker"]) {
            result |= AVAudioSessionCategoryOptionDefaultToSpeaker;
        } else if ([option isEqualToString:@"interruptSpokenAudioAndMixWithOthers"]) {
            result |= AVAudioSessionCategoryOptionInterruptSpokenAudioAndMixWithOthers;
        }
    }
    return result;
}

@end

NS_ASSUME_NONNULL_END
