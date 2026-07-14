#import "WebRTCModule+CallKit.h"

#import <objc/runtime.h>

#import <React/RCTBridgeModule.h>

#import "CallKitManager.h"

#import "WebRTCModule+PushKit.h"

static void *CallKitManagerKey = &CallKitManagerKey;

@implementation WebRTCModule (CallKit)

- (CallKitManager *)callKitManager {
    CallKitManager *manager = objc_getAssociatedObject(self, CallKitManagerKey);
    if (manager != nil) {
        return manager;
    }

    manager = [CallKitManager shared];
    __weak typeof(self) weakSelf = self;
    manager.onCallStarted = ^{
        [weakSelf sendEventWithName:kEventCallKitActionPerformed body:@{@"started" : [NSNull null]}];
    };
    manager.onCallAnswered = ^(NSString *requestId) {
        [weakSelf sendEventWithName:kEventCallKitActionPerformed body:@{@"answer" : requestId}];
    };
    manager.onCallEnded = ^(NSString *reason) {
        [weakSelf sendEventWithName:kEventCallKitActionPerformed body:@{@"ended" : reason ?: @"local"}];
    };
    manager.onCallFailed = ^(NSString *reason) {
        [weakSelf sendEventWithName:kEventCallKitActionPerformed body:@{@"failed" : reason ?: @""}];
    };
    manager.onCallMuted = ^(BOOL isMuted) {
        [weakSelf sendEventWithName:kEventCallKitActionPerformed body:@{@"muted" : @(isMuted)}];
    };
    manager.onCallHeld = ^(BOOL isOnHold) {
        [weakSelf sendEventWithName:kEventCallKitActionPerformed body:@{@"held" : @(isOnHold)}];
    };

    objc_setAssociatedObject(self, CallKitManagerKey, manager, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    return manager;
}

- (void)startObserving {
    [super startObserving];
    [self callKitManager];
    [self startObservingPushKit];
}

- (void)stopObserving {
    [self stopObservingPushKit];
    [super stopObserving];
}

RCT_EXPORT_METHOD(startCallKitSession
                  : (NSString *)displayName handle
                  : (NSString *)handle isVideo
                  : (BOOL)isVideo resolver
                  : (RCTPromiseResolveBlock)resolve rejecter
                  : (RCTPromiseRejectBlock)reject) {
    if (displayName == nil || displayName.length == 0) {
        reject(@"E_CALLKIT_INVALID_DISPLAY_NAME", @"displayName is required", nil);
        return;
    }

    NSString *callHandle = handle.length > 0 ? handle : displayName;

    @try {
        [[self callKitManager] startCallWithDisplayName:displayName handle:callHandle isVideo:isVideo];
        resolve(nil);
    } @catch (NSException *exception) {
        reject(@"E_CALLKIT_START_FAILED", exception.reason, nil);
    }
}

RCT_EXPORT_METHOD(endCallKitSession
                  : (NSString *)reason resolver
                  : (RCTPromiseResolveBlock)resolve rejecter
                  : (RCTPromiseRejectBlock)reject) {
    @try {
        [[self callKitManager] endCallWithReason:reason];
        resolve(nil);
    } @catch (NSException *exception) {
        reject(@"E_CALLKIT_END_FAILED", exception.reason, nil);
    }
}

RCT_EXPORT_METHOD(fulfillIncomingCallConnected
                  : (NSString *)requestId resolver
                  : (RCTPromiseResolveBlock)resolve rejecter
                  : (RCTPromiseRejectBlock)reject) {
    @try {
        resolve(@([[self callKitManager] fulfillIncomingCallConnected:requestId]));
    } @catch (NSException *exception) {
        reject(@"E_CALLKIT_FULFILL_ANSWER_FAILED", exception.reason, nil);
    }
}

RCT_EXPORT_METHOD(failIncomingCallConnected
                  : (NSString *)requestId resolver
                  : (RCTPromiseResolveBlock)resolve rejecter
                  : (RCTPromiseRejectBlock)reject) {
    @try {
        [[self callKitManager] failIncomingCallConnected:requestId];
        resolve(nil);
    } @catch (NSException *exception) {
        reject(@"E_CALLKIT_FAIL_ANSWER_FAILED", exception.reason, nil);
    }
}

RCT_EXPORT_METHOD(reportOutgoingCallConnected
                  : (RCTPromiseResolveBlock)resolve rejecter
                  : (RCTPromiseRejectBlock)reject) {
    @try {
        [[self callKitManager] reportOutgoingCallConnected];
        resolve(nil);
    } @catch (NSException *exception) {
        reject(@"E_CALLKIT_REPORT_OUTGOING_CONNECTED_FAILED", exception.reason, nil);
    }
}

RCT_EXPORT_METHOD(setCallKitCallHeld
                  : (BOOL)onHold resolver
                  : (RCTPromiseResolveBlock)resolve rejecter
                  : (RCTPromiseRejectBlock)reject) {
    @try {
        [[self callKitManager] setCallHeld:onHold];
        resolve(nil);
    } @catch (NSException *exception) {
        reject(@"E_CALLKIT_SET_HELD_FAILED", exception.reason, nil);
    }
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(getPendingAnswerRequestId) {
    return [self callKitManager].pendingAnswerRequestId;
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(hasActiveCallKitSession) {
    return @([self callKitManager].hasActiveCall);
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(isCallAnswered) {
    return @([self callKitManager].isCallAnswered);
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(isCallKitCallHeld) {
    return @([self callKitManager].isCallOnHold);
}

@end
