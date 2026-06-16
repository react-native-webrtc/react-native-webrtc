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
// Returns the JS-provided result on success, or 0 on timeout.
- (NSInteger)sendEventAndWaitWithName:(NSString *)eventName
                                 body:(NSDictionary *)body
                            semaphore:(dispatch_semaphore_t)semaphore
                          resultBlock:(NSInteger (^)(void))resultBlock {
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
    RCTLog(@"[AudioDeviceModuleObserver] Engine created - waiting for JS response");

    NSInteger result = [self sendEventAndWaitWithName:kEventAudioDeviceModuleEngineCreated
                                                 body:@{}
                                            semaphore:self.engineCreatedSemaphore
                                          resultBlock:^NSInteger {
                                              return self.engineCreatedResult;
                                          }];

    RCTLog(@"[AudioDeviceModuleObserver] Engine created - JS returned: %ld", (long)result);
    return result;
}

- (NSInteger)audioDeviceModule:(RTCAudioDeviceModule *)audioDeviceModule
              willEnableEngine:(AVAudioEngine *)engine
              isPlayoutEnabled:(BOOL)isPlayoutEnabled
            isRecordingEnabled:(BOOL)isRecordingEnabled {
    RCTLog(@"[AudioDeviceModuleObserver] Engine will enable - playout: %d, recording: %d - waiting for JS response",
           isPlayoutEnabled,
           isRecordingEnabled);

    NSInteger result = [self sendEventAndWaitWithName:kEventAudioDeviceModuleEngineWillEnable
                                                 body:@{
                                                     @"isPlayoutEnabled" : @(isPlayoutEnabled),
                                                     @"isRecordingEnabled" : @(isRecordingEnabled),
                                                 }
                                            semaphore:self.willEnableEngineSemaphore
                                          resultBlock:^NSInteger {
                                              return self.willEnableEngineResult;
                                          }];

    RCTLog(@"[AudioDeviceModuleObserver] Engine will enable - JS returned: %ld", (long)result);

    AVAudioSession *audioSession = [AVAudioSession sharedInstance];
    RCTLog(@"[AudioDeviceModuleObserver] Audio session category: %@", audioSession.category);

    return result;
}

- (NSInteger)audioDeviceModule:(RTCAudioDeviceModule *)audioDeviceModule
               willStartEngine:(AVAudioEngine *)engine
              isPlayoutEnabled:(BOOL)isPlayoutEnabled
            isRecordingEnabled:(BOOL)isRecordingEnabled {
    RCTLog(@"[AudioDeviceModuleObserver] Engine will start - playout: %d, recording: %d - waiting for JS response",
           isPlayoutEnabled,
           isRecordingEnabled);

    NSInteger result = [self sendEventAndWaitWithName:kEventAudioDeviceModuleEngineWillStart
                                                 body:@{
                                                     @"isPlayoutEnabled" : @(isPlayoutEnabled),
                                                     @"isRecordingEnabled" : @(isRecordingEnabled),
                                                 }
                                            semaphore:self.willStartEngineSemaphore
                                          resultBlock:^NSInteger {
                                              return self.willStartEngineResult;
                                          }];

    RCTLog(@"[AudioDeviceModuleObserver] Engine will start - JS returned: %ld", (long)result);
    return result;
}

- (NSInteger)audioDeviceModule:(RTCAudioDeviceModule *)audioDeviceModule
                 didStopEngine:(AVAudioEngine *)engine
              isPlayoutEnabled:(BOOL)isPlayoutEnabled
            isRecordingEnabled:(BOOL)isRecordingEnabled {
    RCTLog(@"[AudioDeviceModuleObserver] Engine did stop - playout: %d, recording: %d - waiting for JS response",
           isPlayoutEnabled,
           isRecordingEnabled);

    NSInteger result = [self sendEventAndWaitWithName:kEventAudioDeviceModuleEngineDidStop
                                                 body:@{
                                                     @"isPlayoutEnabled" : @(isPlayoutEnabled),
                                                     @"isRecordingEnabled" : @(isRecordingEnabled),
                                                 }
                                            semaphore:self.didStopEngineSemaphore
                                          resultBlock:^NSInteger {
                                              return self.didStopEngineResult;
                                          }];

    RCTLog(@"[AudioDeviceModuleObserver] Engine did stop - JS returned: %ld", (long)result);
    return result;
}

- (NSInteger)audioDeviceModule:(RTCAudioDeviceModule *)audioDeviceModule
              didDisableEngine:(AVAudioEngine *)engine
              isPlayoutEnabled:(BOOL)isPlayoutEnabled
            isRecordingEnabled:(BOOL)isRecordingEnabled {
    RCTLog(@"[AudioDeviceModuleObserver] Engine did disable - playout: %d, recording: %d - waiting for JS response",
           isPlayoutEnabled,
           isRecordingEnabled);

    NSInteger result = [self sendEventAndWaitWithName:kEventAudioDeviceModuleEngineDidDisable
                                                 body:@{
                                                     @"isPlayoutEnabled" : @(isPlayoutEnabled),
                                                     @"isRecordingEnabled" : @(isRecordingEnabled),
                                                 }
                                            semaphore:self.didDisableEngineSemaphore
                                          resultBlock:^NSInteger {
                                              return self.didDisableEngineResult;
                                          }];

    RCTLog(@"[AudioDeviceModuleObserver] Engine did disable - JS returned: %ld", (long)result);
    return result;
}

- (NSInteger)audioDeviceModule:(RTCAudioDeviceModule *)audioDeviceModule willReleaseEngine:(AVAudioEngine *)engine {
    RCTLog(@"[AudioDeviceModuleObserver] Engine will release - waiting for JS response");

    NSInteger result = [self sendEventAndWaitWithName:kEventAudioDeviceModuleEngineWillRelease
                                                 body:@{}
                                            semaphore:self.willReleaseEngineSemaphore
                                          resultBlock:^NSInteger {
                                              return self.willReleaseEngineResult;
                                          }];

    RCTLog(@"[AudioDeviceModuleObserver] Engine will release - JS returned: %ld", (long)result);
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
    }
    dispatch_semaphore_signal(semaphore);
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

@end

NS_ASSUME_NONNULL_END
