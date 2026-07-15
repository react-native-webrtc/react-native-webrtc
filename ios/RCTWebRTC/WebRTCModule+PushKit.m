#import "WebRTCModule+PushKit.h"

#import "VoipManager.h"

@implementation WebRTCModule (PushKit)

- (void)startObservingPushKit {
    VoipManager *push = [VoipManager shared];
    __weak typeof(self) weakSelf = self;

    push.onTokenUpdated = ^(NSString *token) {
        [weakSelf sendEventWithName:kEventVoipPush body:@{@"registered" : token ?: @""}];
    };

    push.onIncomingPush = ^(NSDictionary *payload) {
        [weakSelf sendEventWithName:kEventVoipPush body:@{@"incoming" : payload ?: @{}}];
    };
    push.onWaitingCallDeclined = ^(NSDictionary *payload) {
        [weakSelf sendEventWithName:kEventVoipPush body:@{@"waitingDeclined" : payload ?: @{}}];
    };
    push.onCallIntent = ^(NSDictionary *intent) {
        [weakSelf sendEventWithName:kEventVoipPush body:@{@"callIntent" : intent ?: @{}}];
    };

    NSString *token = push.token;
    if (token.length > 0) {
        [weakSelf sendEventWithName:kEventVoipPush body:@{@"registered" : token}];
    }

    NSDictionary *pendingCall = push.pendingIncomingCall;
    if (pendingCall) {
        [weakSelf sendEventWithName:kEventVoipPush body:@{@"incoming" : pendingCall}];
    }
}

- (void)stopObservingPushKit {
    VoipManager *push = [VoipManager shared];
    push.onTokenUpdated = nil;
    push.onIncomingPush = nil;
    push.onWaitingCallDeclined = nil;
    push.onCallIntent = nil;
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(getVoipToken) {
    return [VoipManager shared].token ?: [NSNull null];
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(getPendingIncomingCall) {
    return [VoipManager shared].pendingIncomingCall ?: [NSNull null];
}

RCT_EXPORT_METHOD(clearPendingIncomingCall) {
    [[VoipManager shared] clearPendingIncomingCall];
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(getPendingCallIntent) {
    return [VoipManager shared].pendingCallIntent ?: [NSNull null];
}

RCT_EXPORT_METHOD(clearPendingCallIntent) {
    [[VoipManager shared] clearPendingCallIntent];
}

@end
