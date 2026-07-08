#import "CallKitManager.h"

#import <AVFoundation/AVFoundation.h>
#import <WebRTC/RTCAudioSession.h>
#import "VoipManager.h"

@interface CallKitManager ()
@property(nonatomic, strong) CXCallController *callController;
@property(nonatomic, strong) CXProvider *provider;
@property(nonatomic, strong) NSUUID *currentCallUUID;
@property(nonatomic, assign) BOOL isCallAnswered;
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
        providerConfiguration.includesCallsInRecents = NO;

        _provider = [[CXProvider alloc] initWithConfiguration:providerConfiguration];
        [_provider setDelegate:self queue:nil];
        _callController = [[CXCallController alloc] init];
    }
    return self;
}

- (BOOL)hasActiveCall {
    return self.currentCallUUID != nil;
}

- (void)startCallWithDisplayName:(NSString *)displayName isVideo:(BOOL)isVideo {
    if (self.currentCallUUID != nil) {
        NSLog(@"[CallKitManager] Call already in progress");
        return;
    }

    NSUUID *uuid = [NSUUID UUID];
    self.currentCallUUID = uuid;

    CXHandle *handle = [[CXHandle alloc] initWithType:CXHandleTypeGeneric value:displayName];
    CXStartCallAction *startCallAction = [[CXStartCallAction alloc] initWithCallUUID:uuid handle:handle];
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

                    [weakSelf.provider reportOutgoingCallWithUUID:uuid startedConnectingAtDate:[NSDate date]];
                    [weakSelf.provider reportOutgoingCallWithUUID:uuid connectedAtDate:[NSDate date]];
                    if (weakSelf.onCallStarted) {
                        weakSelf.onCallStarted();
                    }
                }];
}

- (void)reportIncomingCallWithDisplayName:(NSString *)displayName isVideo:(BOOL)isVideo {
    NSUUID *uuid = [NSUUID UUID];
    self.currentCallUUID = uuid;
    self.isCallAnswered = NO;

    CXCallUpdate *update = [[CXCallUpdate alloc] init];
    update.remoteHandle = [[CXHandle alloc] initWithType:CXHandleTypeGeneric value:displayName];
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
                                          }
                                      }];
}

- (void)endCall {
    if (self.currentCallUUID == nil) {
        NSLog(@"[CallKitManager] No active call to end");
        return;
    }

    CXEndCallAction *endCallAction = [[CXEndCallAction alloc] initWithCallUUID:self.currentCallUUID];
    CXTransaction *transaction = [[CXTransaction alloc] initWithAction:endCallAction];

    __weak typeof(self) weakSelf = self;
    [self.callController requestTransaction:transaction
                                 completion:^(NSError *error) {
                                     if (error) {
                                         NSLog(@"[CallKitManager] Failed to end call: %@", error.localizedDescription);
                                         return;
                                     }
                                     if (weakSelf.onCallEnded) {
                                         weakSelf.onCallEnded();
                                     }
                                     [weakSelf cleanup];
                                 }];
}

- (void)cleanup {
    self.currentCallUUID = nil;
    self.isCallAnswered = NO;
    [[VoipManager shared] clearPendingIncomingCall];
}

#pragma mark - CXProviderDelegate

- (void)providerDidReset:(CXProvider *)provider {
    [self cleanup];
}

- (void)provider:(CXProvider *)provider performStartCallAction:(CXStartCallAction *)action {
    [action fulfill];
}

- (void)provider:(CXProvider *)provider performEndCallAction:(CXEndCallAction *)action {
    if (self.onCallEnded) {
        self.onCallEnded();
    }
    [action fulfill];
    [self cleanup];
}

- (void)provider:(CXProvider *)provider performAnswerCallAction:(CXAnswerCallAction *)action {
    self.isCallAnswered = YES;
    if (self.onCallAnswered) {
        self.onCallAnswered();
    }
    [action fulfill];
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
