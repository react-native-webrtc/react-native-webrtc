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
@property(nonatomic, assign) BOOL isCallOnHold;
@property(nonatomic, copy, nullable) NSString *pendingAnswerRequestId;
@property(nonatomic, copy, nullable) dispatch_block_t ringTimeoutBlock;
@property(nonatomic, strong) NSUUID *waitingCallUUID;
@property(nonatomic, copy, nullable) dispatch_block_t waitingRingTimeoutBlock;
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
        providerConfiguration.maximumCallGroups = 2;
        providerConfiguration.includesCallsInRecents = YES;

        _provider = [[CXProvider alloc] initWithConfiguration:providerConfiguration];
        [_provider setDelegate:self queue:nil];
        _callController = [[CXCallController alloc] init];
        _incomingCallTimeout = timeoutFromInfoPlist(@"VoipIncomingCallTimeout", kDefaultIncomingCallTimeout);
        _outgoingCallTimeout = timeoutFromInfoPlist(@"VoipOutgoingCallTimeout", kDefaultOutgoingCallTimeout);
        _fulfillAnswerTimeout = timeoutFromInfoPlist(@"VoipFulfillAnswerTimeout", kDefaultFulfillAnswerTimeout);
    }
    return self;
}

- (BOOL)hasActiveCall {
    return self.currentCallUUID != nil;
}

- (void)reportCallCapabilitiesForUUID:(NSUUID *)uuid supportsHolding:(BOOL)supportsHolding {
    if (uuid == nil) {
        return;
    }
    CXCallUpdate *update = [[CXCallUpdate alloc] init];
    update.supportsHolding = supportsHolding;
    update.supportsGrouping = NO;
    update.supportsUngrouping = NO;
    update.supportsDTMF = NO;
    [self.provider reportCallWithUUID:uuid updated:update];
}

- (void)startCallWithDisplayName:(NSString *)displayName handle:(NSString *)handle isVideo:(BOOL)isVideo {
    if (self.currentCallUUID != nil || self.waitingCallUUID != nil) {
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
                        [weakSelf cleanupCurrentCall];
                        return;
                    }

                    if (weakSelf.onCallStarted) {
                        weakSelf.onCallStarted();
                    }

                    CXCallUpdate *update = [[CXCallUpdate alloc] init];
                    update.supportsHolding = YES;
                    update.supportsGrouping = NO;
                    update.supportsUngrouping = NO;
                    update.supportsDTMF = NO;
                    [weakSelf.provider reportCallWithUUID:uuid updated:update];
                }];
}

- (IncomingCallSlot)reportIncomingCallWithDisplayName:(NSString *)displayName
                                                      handle:(NSString *)handle
                                                     isVideo:(BOOL)isVideo {
    if (self.currentCallUUID != nil) {
        if (!self.isCallAnswered || self.waitingCallUUID != nil) {
            [self reportTransientIncomingCallAndEndWithDisplayName:displayName handle:handle isVideo:isVideo];
            return IncomingCallSlotRejected;
        }
    }

    BOOL becomesWaiting = self.currentCallUUID != nil;
    NSUUID *uuid = [NSUUID UUID];

    if (becomesWaiting) {
        self.waitingCallUUID = uuid;
        if (self.isCallAnswered) {
            [self reportCallCapabilitiesForUUID:self.currentCallUUID supportsHolding:NO];
        }
    } else {
        self.currentCallUUID = uuid;
        self.isCallAnswered = NO;
        self.isOutgoingCall = NO;
    }

    CXCallUpdate *update = [[CXCallUpdate alloc] init];
    update.remoteHandle = [[CXHandle alloc] initWithType:CXHandleTypeGeneric value:handle];
    update.localizedCallerName = displayName;
    update.hasVideo = isVideo;
    // Waiting calls must not offer Hold & Accept
    update.supportsHolding = becomesWaiting ? NO : YES;
    update.supportsGrouping = NO;
    update.supportsUngrouping = NO;
    update.supportsDTMF = NO;

    __weak typeof(self) weakSelf = self;
    [self.provider
        reportNewIncomingCallWithUUID:uuid
                               update:update
                           completion:^(NSError *_Nullable error) {
                               typeof(self) strongSelf = weakSelf;
                               if (strongSelf == nil) {
                                   return;
                               }
                               if (error) {
                                   NSLog(@"[CallKitManager] Failed to report incoming call: %@",
                                         error.localizedDescription);
                                   if (becomesWaiting) {
                                       [strongSelf cleanupWaitingCall];
                                   } else {
                                       strongSelf.currentCallUUID = nil;
                                       if (strongSelf.onCallFailed) {
                                           strongSelf.onCallFailed(error.localizedDescription);
                                       }
                                   }
                                   return;
                               }
                               if (becomesWaiting) {
                                   [strongSelf startWaitingRingTimeoutForCall:uuid
                                                                      timeout:strongSelf.incomingCallTimeout];
                               } else {
                                   [strongSelf startRingTimeoutForCall:uuid timeout:strongSelf.incomingCallTimeout];
                               }
                           }];

    return becomesWaiting ? IncomingCallSlotWaiting : IncomingCallSlotCurrent;
}

/**
 * PushKit requires every VoIP push to post an incoming call to CallKit. When there is no
 * slot left, report a throwaway call and end it immediately without disturbing whoever
 * is already ringing.
 */
- (void)reportTransientIncomingCallAndEndWithDisplayName:(NSString *)displayName
                                                    handle:(NSString *)handle
                                                   isVideo:(BOOL)isVideo {
    NSUUID *uuid = [NSUUID UUID];

    CXCallUpdate *update = [[CXCallUpdate alloc] init];
    update.remoteHandle = [[CXHandle alloc] initWithType:CXHandleTypeGeneric value:handle];
    update.localizedCallerName = displayName;
    update.hasVideo = isVideo;
    update.supportsHolding = NO;
    update.supportsGrouping = NO;
    update.supportsUngrouping = NO;
    update.supportsDTMF = NO;

    __weak typeof(self) weakSelf = self;
    [self.provider
        reportNewIncomingCallWithUUID:uuid
                               update:update
                           completion:^(NSError *_Nullable error) {
                               typeof(self) strongSelf = weakSelf;
                               if (strongSelf == nil) {
                                   return;
                               }
                               if (error) {
                                   NSLog(@"[CallKitManager] Failed to report transient incoming call: %@",
                                         error.localizedDescription);
                               }
                               [strongSelf.provider reportCallWithUUID:uuid
                                                             endedAtDate:[NSDate date]
                                                                reason:CXCallEndedReasonFailed];
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
    [self cleanupCurrentCall];
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
    self.isCallAnswered = YES;
    [self.provider reportOutgoingCallWithUUID:uuid connectedAtDate:[NSDate date]];
}

- (void)setCallHeld:(BOOL)onHold {
    NSUUID *uuid = self.currentCallUUID;
    if (uuid == nil) {
        NSLog(@"[CallKitManager] No active call to set held");
        return;
    }

    CXSetHeldCallAction *action = [[CXSetHeldCallAction alloc] initWithCallUUID:uuid onHold:onHold];
    CXTransaction *transaction = [[CXTransaction alloc] initWithAction:action];
    [self.callController requestTransaction:transaction
                                 completion:^(NSError *error) {
                                     if (error) {
                                         NSLog(@"[CallKitManager] Failed to set held: %@", error.localizedDescription);
                                     }
                                 }];
}

- (void)reportAnswerFailureForCall:(NSUUID *)uuid {
    if (uuid == nil || ![uuid isEqual:self.currentCallUUID]) {
        return;
    }

    [self.provider reportCallWithUUID:uuid endedAtDate:[NSDate date] reason:CXCallEndedReasonFailed];
    [self cleanupCurrentCall];
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
    dispatch_after(dispatch_walltime(NULL, (int64_t)(timeout * NSEC_PER_SEC)), dispatch_get_main_queue(), block);
}

- (void)cancelRingTimeout {
    if (self.ringTimeoutBlock != nil) {
        dispatch_block_cancel(self.ringTimeoutBlock);
        self.ringTimeoutBlock = nil;
    }
}

- (void)startWaitingRingTimeoutForCall:(NSUUID *)uuid timeout:(NSTimeInterval)timeout {
    [self cancelWaitingRingTimeout];

    __weak typeof(self) weakSelf = self;
    dispatch_block_t block = dispatch_block_create(0, ^{
        typeof(self) strongSelf = weakSelf;
        if (strongSelf == nil) {
            return;
        }
        strongSelf.waitingRingTimeoutBlock = nil;
        if (![uuid isEqual:strongSelf.waitingCallUUID]) {
            return;
        }
        [strongSelf.provider reportCallWithUUID:uuid endedAtDate:[NSDate date] reason:CXCallEndedReasonUnanswered];
        [strongSelf cleanupWaitingCall];
    });
    self.waitingRingTimeoutBlock = block;
    dispatch_after(dispatch_walltime(NULL, (int64_t)(timeout * NSEC_PER_SEC)), dispatch_get_main_queue(), block);
}

- (void)cancelWaitingRingTimeout {
    if (self.waitingRingTimeoutBlock != nil) {
        dispatch_block_cancel(self.waitingRingTimeoutBlock);
        self.waitingRingTimeoutBlock = nil;
    }
}

- (void)cleanupCurrentCall {
    [self cancelRingTimeout];
    self.currentCallUUID = nil;
    self.isCallAnswered = NO;
    self.isOutgoingCall = NO;
    self.isCallOnHold = NO;
    self.pendingAnswerRequestId = nil;
    [[FulfillRequestManager shared] cancelAll];
    [[VoipManager shared] clearPendingIncomingCall];
}

- (void)cleanupWaitingCall {
    [self cancelWaitingRingTimeout];
    self.waitingCallUUID = nil;
    if (self.currentCallUUID != nil) {
        [self reportCallCapabilitiesForUUID:self.currentCallUUID supportsHolding:YES];
    }
    [[VoipManager shared] discardPendingSecondIncomingCall];
}

/**
 * Answering the waiting call ends whichever call was current and takes its place.
 * "End & Accept" delivers a `CXEndCallAction` for the old call before the
 * `CXAnswerCallAction` that triggers this, so `performEndCallAction:` normally does
 * that ending; this is only a safety net in case the old call is still around.
 */
- (void)promoteWaitingCallToCurrent {
    NSUUID *promoted = self.waitingCallUUID;
    [self cancelWaitingRingTimeout];
    self.waitingCallUUID = nil;

    if (self.currentCallUUID != nil) {
        if (self.onCallEnded) {
            self.onCallEnded(@"local");
        }
        [self cleanupCurrentCall];
    }

    self.currentCallUUID = promoted;
    self.isCallAnswered = NO;
    self.isOutgoingCall = NO;
    self.isCallOnHold = NO;
    [self reportCallCapabilitiesForUUID:promoted supportsHolding:YES];
    [[VoipManager shared] revealPendingSecondIncomingCall];
}

#pragma mark - CXProviderDelegate

- (void)providerDidReset:(CXProvider *)provider {
    [self cleanupCurrentCall];
    [self cleanupWaitingCall];
}

- (void)provider:(CXProvider *)provider performStartCallAction:(CXStartCallAction *)action {
    [provider reportOutgoingCallWithUUID:action.callUUID startedConnectingAtDate:[NSDate date]];
    [self startRingTimeoutForCall:action.callUUID timeout:self.outgoingCallTimeout];
    [action fulfill];
}

- (void)provider:(CXProvider *)provider performEndCallAction:(CXEndCallAction *)action {
    NSUUID *uuid = action.callUUID;

    if ([uuid isEqual:self.waitingCallUUID]) {
        // Declined (or ended) before ever being answered
        [self cleanupWaitingCall];
        [action fulfill];
        return;
    }

    if (![uuid isEqual:self.currentCallUUID]) {
        // Already handled, safety net
        [action fulfill];
        return;
    }

    if (self.onCallEnded) {
        self.onCallEnded(@"local");
    }
    [action fulfill];
    [self cleanupCurrentCall];
}

- (void)provider:(CXProvider *)provider performAnswerCallAction:(CXAnswerCallAction *)action {
    NSUUID *uuid = action.callUUID;

    if ([uuid isEqual:self.waitingCallUUID]) {
        [self promoteWaitingCallToCurrent];
    }

    if (![uuid isEqual:self.currentCallUUID]) {
        [action fail];
        return;
    }

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
    if (self.waitingCallUUID != nil &&
        [action.callUUID isEqual:self.currentCallUUID] &&
        action.isOnHold) {
        [action fail];
        return;
    }

    self.isCallOnHold = action.isOnHold;
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
