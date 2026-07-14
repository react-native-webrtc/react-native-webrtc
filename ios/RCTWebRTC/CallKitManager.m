#import "CallKitManager.h"

#import <AVFoundation/AVFoundation.h>
#import <WebRTC/RTCAudioSession.h>
#import "FulfillRequestManager.h"
#import "VoipManager.h"

static const NSTimeInterval kDefaultIncomingCallTimeout = 45;
static const NSTimeInterval kDefaultOutgoingCallTimeout = 60;
static const NSTimeInterval kDefaultFulfillAnswerTimeout = 10;

static NSTimeInterval timeoutFromInfoPlist(NSString *key, NSTimeInterval fallback) {
    id value = [NSBundle.mainBundle objectForInfoDictionaryKey:key];
    if ([value respondsToSelector:@selector(doubleValue)]) {
        double seconds = [value doubleValue];
        if (seconds > 0) {
            return seconds;
        }
    }
    return fallback;
}

@interface CallKitManager ()
@property(nonatomic, strong) CXCallController *callController;
@property(nonatomic, strong) CXProvider *provider;
@property(nonatomic, strong) NSUUID *currentCallUUID;
@property(nonatomic, assign) BOOL isCallAnswered;
@property(nonatomic, assign) BOOL isOutgoingCall;
@property(nonatomic, copy, nullable) NSString *pendingAnswerRequestId;
@property(nonatomic, copy, nullable) dispatch_block_t ringTimeoutBlock;
@property(nonatomic, assign) NSTimeInterval incomingCallTimeout;
@property(nonatomic, assign) NSTimeInterval outgoingCallTimeout;
@property(nonatomic, assign) NSTimeInterval fulfillAnswerTimeout;
@end

@implementation CallKitManager

+ (instancetype)shared {
    static CallKitManager *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[CallKitManager alloc] init];
    });
    return sharedInstance;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        CXProviderConfiguration *providerConfiguration = [[CXProviderConfiguration alloc] init];
        providerConfiguration.supportsVideo = YES;
        providerConfiguration.supportedHandleTypes = [NSSet setWithObject:@(CXHandleTypeGeneric)];
        providerConfiguration.maximumCallsPerCallGroup = 1;
        providerConfiguration.maximumCallGroups = 1;
        providerConfiguration.includesCallsInRecents = YES;

        _provider = [[CXProvider alloc] initWithConfiguration:providerConfiguration];
        [_provider setDelegate:self queue:nil];
        _callController = [[CXCallController alloc] init];
        _incomingCallTimeout = timeoutFromInfoPlist(@"FishjamVoipIncomingCallTimeout", kDefaultIncomingCallTimeout);
        _outgoingCallTimeout = timeoutFromInfoPlist(@"FishjamVoipOutgoingCallTimeout", kDefaultOutgoingCallTimeout);
        _fulfillAnswerTimeout = timeoutFromInfoPlist(@"FishjamVoipFulfillAnswerTimeout", kDefaultFulfillAnswerTimeout);
    }
    return self;
}

- (BOOL)hasActiveCall {
    return self.currentCallUUID != nil;
}

- (void)startCallWithDisplayName:(NSString *)displayName handle:(NSString *)handle isVideo:(BOOL)isVideo {
    if (self.currentCallUUID != nil) {
        NSLog(@"[CallKitManager] Call already in progress");
        return;
    }

    NSUUID *uuid = [NSUUID UUID];
    self.currentCallUUID = uuid;
    self.isOutgoingCall = YES;

    // The handle is the identity persisted in Recents and handed back to us in the
    // redial intent, so it must be the caller's unique id.
    CXHandle *callHandle = [[CXHandle alloc] initWithType:CXHandleTypeGeneric value:handle];
    CXStartCallAction *startCallAction = [[CXStartCallAction alloc] initWithCallUUID:uuid handle:callHandle];
    startCallAction.video = isVideo;
    startCallAction.contactIdentifier = displayName;

    CXTransaction *transaction = [[CXTransaction alloc] initWithAction:startCallAction];

    __weak typeof(self) weakSelf = self;
    [self.callController
        requestTransaction:transaction
                completion:^(NSError *error) {
                    if (error) {
                        NSLog(@"[CallKitManager] Failed to start call: %@", error.localizedDescription);
                        weakSelf.currentCallUUID = nil;
                        if (weakSelf.onCallFailed) {
                            weakSelf.onCallFailed(error.localizedDescription);
                        }
                        [weakSelf cleanup];
                        return;
                    }

                    if (weakSelf.onCallStarted) {
                        weakSelf.onCallStarted();
                    }
                }];
}

- (void)reportIncomingCallWithDisplayName:(NSString *)displayName
                                   handle:(NSString *)handle
                                  isVideo:(BOOL)isVideo {
    NSUUID *uuid = [NSUUID UUID];
    self.currentCallUUID = uuid;
    self.isCallAnswered = NO;
    self.isOutgoingCall = NO;

    CXCallUpdate *update = [[CXCallUpdate alloc] init];
    update.remoteHandle = [[CXHandle alloc] initWithType:CXHandleTypeGeneric value:handle];
    update.localizedCallerName = displayName;
    update.hasVideo = isVideo;
    update.supportsHolding = NO;
    update.supportsGrouping = NO;
    update.supportsUngrouping = NO;
    update.supportsDTMF = NO;

    __weak typeof(self) weakSelf = self;
    [self.provider reportNewIncomingCallWithUUID:uuid
                                          update:update
                                      completion:^(NSError *_Nullable error) {
                                          if (error) {
                                              NSLog(@"[CallKitManager] Failed to report incoming call: %@",
                                                    error.localizedDescription);
                                              weakSelf.currentCallUUID = nil;
                                              if (weakSelf.onCallFailed) {
                                                  weakSelf.onCallFailed(error.localizedDescription);
                                              }
                                              return;
                                          }
                                          [weakSelf startRingTimeoutForCall:uuid timeout:weakSelf.incomingCallTimeout];
                                      }];
}

/**
 * Only the reasons that must go through `reportCallWithUUID:endedAtDate:reason:` are covered
 * here - `local`/`rejected` are handled separately via the `CXEndCallAction`
 * transaction, since CallKit has no ended-reason case for either.
 */
- (CXCallEndedReason)cxEndedReasonForReason:(NSString *)reason {
    if ([reason isEqualToString:@"missed"]) {
        return CXCallEndedReasonUnanswered;
    } else if ([reason isEqualToString:@"remote"]) {
        return CXCallEndedReasonRemoteEnded;
    } else if ([reason isEqualToString:@"answeredElsewhere"]) {
        return CXCallEndedReasonAnsweredElsewhere;
    } else if ([reason isEqualToString:@"failed"]) {
        return CXCallEndedReasonFailed;
    }
    return CXCallEndedReasonRemoteEnded;
}

- (void)endCallWithReason:(NSString *)reason {
    if (self.currentCallUUID == nil) {
        NSLog(@"[CallKitManager] No active call to end");
        return;
    }

    if (reason == nil || [reason isEqualToString:@"local"] || [reason isEqualToString:@"rejected"]) {
        CXEndCallAction *endCallAction = [[CXEndCallAction alloc] initWithCallUUID:self.currentCallUUID];
        CXTransaction *transaction = [[CXTransaction alloc] initWithAction:endCallAction];

        __weak typeof(self) weakSelf = self;
        [self.callController
            requestTransaction:transaction
                    completion:^(NSError *error) {
                        if (error) {
                            NSLog(@"[CallKitManager] Failed to end call: %@", error.localizedDescription);
                            return;
                        }
                        // onCallEnded fires from performEndCallAction once the transaction
                        // fulfills, so it is not duplicated here.
                    }];
        return;
    }

    // (missed / remote / answeredElsewhere / failed)
    NSUUID *uuid = self.currentCallUUID;
    [self.provider reportCallWithUUID:uuid endedAtDate:[NSDate date] reason:[self cxEndedReasonForReason:reason]];
    if (self.onCallEnded) {
        self.onCallEnded(reason);
    }
    [self cleanup];
}

- (BOOL)fulfillIncomingCallConnected:(NSString *)requestId {
    return [[FulfillRequestManager shared] fulfill:requestId];
}

- (void)failIncomingCallConnected:(NSString *)requestId {
    [[FulfillRequestManager shared] cancel:requestId];
}

- (void)reportOutgoingCallConnected {
    NSUUID *uuid = self.currentCallUUID;
    if (uuid == nil || !self.isOutgoingCall) {
        NSLog(@"[CallKitManager] No outgoing call to report as connected");
        return;
    }

    [self cancelRingTimeout];
    [self.provider reportOutgoingCallWithUUID:uuid connectedAtDate:[NSDate date]];
}

- (void)reportAnswerFailureForCall:(NSUUID *)uuid {
    if (uuid == nil || ![uuid isEqual:self.currentCallUUID]) {
        return;
    }

    [self.provider reportCallWithUUID:uuid endedAtDate:[NSDate date] reason:CXCallEndedReasonFailed];
    [self cleanup];
}

- (void)startRingTimeoutForCall:(NSUUID *)uuid timeout:(NSTimeInterval)timeout {
    [self cancelRingTimeout];

    __weak typeof(self) weakSelf = self;
    dispatch_block_t block = dispatch_block_create(0, ^{
        typeof(self) strongSelf = weakSelf;
        if (strongSelf == nil) {
            return;
        }
        strongSelf.ringTimeoutBlock = nil;
        if (![uuid isEqual:strongSelf.currentCallUUID] || strongSelf.isCallAnswered) {
            return;
        }
        [strongSelf endCallWithReason:@"missed"];
    });
    self.ringTimeoutBlock = block;
    dispatch_after(dispatch_walltime(NULL, (int64_t)(timeout * NSEC_PER_SEC)),
                   dispatch_get_main_queue(), block);
}

- (void)cancelRingTimeout {
    if (self.ringTimeoutBlock != nil) {
        dispatch_block_cancel(self.ringTimeoutBlock);
        self.ringTimeoutBlock = nil;
    }
}

- (void)cleanup {
    [self cancelRingTimeout];
    self.currentCallUUID = nil;
    self.isCallAnswered = NO;
    self.isOutgoingCall = NO;
    self.pendingAnswerRequestId = nil;
    [[FulfillRequestManager shared] cancelAll];
    [[VoipManager shared] clearPendingIncomingCall];
}

#pragma mark - CXProviderDelegate

- (void)providerDidReset:(CXProvider *)provider {
    [self cleanup];
}

- (void)provider:(CXProvider *)provider performStartCallAction:(CXStartCallAction *)action {
    [provider reportOutgoingCallWithUUID:action.callUUID startedConnectingAtDate:[NSDate date]];
    [self startRingTimeoutForCall:action.callUUID timeout:self.outgoingCallTimeout];
    [action fulfill];
}

- (void)provider:(CXProvider *)provider performEndCallAction:(CXEndCallAction *)action {
    if (self.onCallEnded) {
        self.onCallEnded(@"local");
    }
    [action fulfill];
    [self cleanup];
}

- (void)provider:(CXProvider *)provider performAnswerCallAction:(CXAnswerCallAction *)action {
    [self cancelRingTimeout];
    self.isCallAnswered = YES;

    __weak typeof(self) weakSelf = self;
    __block NSString *requestId = nil;
    requestId = [[FulfillRequestManager shared]
        createRequestWithTimeout:self.fulfillAnswerTimeout
                      completion:^(FulfillResult result) {
                          typeof(self) strongSelf = weakSelf;
                          if (strongSelf == nil) {
                              [action fail];
                              return;
                          }
                          if ([strongSelf.pendingAnswerRequestId isEqualToString:requestId]) {
                              strongSelf.pendingAnswerRequestId = nil;
                          }
                          if (result == FulfillResultFulfilled) {
                              [action fulfill];
                          } else {
                              [action fail];
                              [strongSelf reportAnswerFailureForCall:action.callUUID];
                          }
                      }];

    self.pendingAnswerRequestId = requestId;
    if (self.onCallAnswered) {
        self.onCallAnswered(requestId);
    }
}

- (void)provider:(CXProvider *)provider timedOutPerformingAction:(CXAction *)action {
    if (![action isKindOfClass:[CXAnswerCallAction class]]) {
        return;
    }

    NSString *requestId = self.pendingAnswerRequestId;
    if (requestId != nil && [[FulfillRequestManager shared] cancel:requestId]) {
        return;
    }

    [action fail];
    [self reportAnswerFailureForCall:action.UUID];
}

- (void)provider:(CXProvider *)provider performSetHeldCallAction:(CXSetHeldCallAction *)action {
    if (self.onCallHeld) {
        self.onCallHeld(action.isOnHold);
    }
    [action fulfill];
}

- (void)provider:(CXProvider *)provider performSetMutedCallAction:(CXSetMutedCallAction *)action {
    if (self.onCallMuted) {
        self.onCallMuted(action.isMuted);
    }
    [action fulfill];
}

- (void)provider:(CXProvider *)provider performSetGroupCallAction:(CXSetGroupCallAction *)action {
    [action fail];
}

- (void)provider:(CXProvider *)provider didActivateAudioSession:(AVAudioSession *)audioSession {
    [[RTCAudioSession sharedInstance] audioSessionDidActivate:audioSession];
}

- (void)provider:(CXProvider *)provider didDeactivateAudioSession:(AVAudioSession *)audioSession {
    [[RTCAudioSession sharedInstance] audioSessionDidDeactivate:audioSession];
}

@end
