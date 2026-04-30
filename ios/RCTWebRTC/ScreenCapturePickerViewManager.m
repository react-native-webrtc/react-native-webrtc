#if TARGET_OS_IOS

#import <React/RCTUIManager.h>
#import <ReplayKit/ReplayKit.h>

#import "ScreenCapturePickerViewManager.h"

NSString *const kRTCScreenSharingExtension = @"RTCScreenSharingExtension";

/**
 * Clears observers registered by showAwaiting
 */
void clearObservers(NSMutableDictionary *promiseDict) {
    
    if (!promiseDict) {
        return;
    }
    
    CFNotificationCenterRef darwinCenter = CFNotificationCenterGetDarwinNotifyCenter();
    CFNotificationCenterRemoveObserver(darwinCenter, (__bridge void*)promiseDict, CFSTR("iOS_BroadcastStarted"), NULL);
    
    id nsObserver = promiseDict[@"ns_observer"];
    if(nsObserver) {
        [[NSNotificationCenter defaultCenter] removeObserver:nsObserver];
    };
    [promiseDict removeAllObjects];
}

/**
 * Callback function for iOS_BroadcastStarted notification
 */
void onBroadcastStarted(CFNotificationCenterRef center, void *observer, CFStringRef name, const void *object, CFDictionaryRef userInfo) {
    NSMutableDictionary *promiseDict = (__bridge_transfer NSMutableDictionary*)observer;
    RCTPromiseResolveBlock resolve = promiseDict[@"resolve"];
    if (resolve) {
        resolve(nil);
    }
    
    clearObservers(promiseDict);
    
}

@implementation ScreenCapturePickerViewManager {
    RPSystemBroadcastPickerView *_broadcastPickerView;
}

RCT_EXPORT_MODULE()

- (UIView *)view {
    _broadcastPickerView = [[RPSystemBroadcastPickerView alloc] init];
    _broadcastPickerView.preferredExtension = self.preferredExtension;
    _broadcastPickerView.showsMicrophoneButton = false;
    _broadcastPickerView.userInteractionEnabled = false;

    return _broadcastPickerView;
}

// MARK: Private Methods

- (NSString *)preferredExtension {
    NSDictionary *infoDictionary = [[NSBundle mainBundle] infoDictionary];
    return infoDictionary[kRTCScreenSharingExtension];
}

RCT_EXPORT_METHOD(show : (nonnull NSNumber *)reactTag) {
    [self.bridge.uiManager
        addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
            id view = viewRegistry[reactTag];
            if (![view isKindOfClass:[RPSystemBroadcastPickerView class]]) {
                RCTLogError(@"Invalid view returned from registry, expecting "
                            @"RPSystemBroadcastPickerView, got: %@",
                            view);
            } else {
                // Simulate a click
                UIButton *btn = nil;

                for (UIView *subview in ((RPSystemBroadcastPickerView *)view).subviews) {
                    if ([subview isKindOfClass:[UIButton class]]) {
                        btn = (UIButton *)subview;
                    }
                }
                if (btn != nil) {
                    [btn sendActionsForControlEvents:UIControlEventTouchUpInside];
                } else {
                    RCTLogError(@"RPSystemBroadcastPickerView button not found");
                }
            }
        }];
}

/**
 * Shows the RPSystemBroadcastPickerView and waits for either the broadcast to
 * start, or a 5 second timeout after the picker has been dismissed (broadcast
 * only starts after a 3 second countdown.)
 */
RCT_EXPORT_METHOD(showAwaiting: (nonnull NSNumber *)reactTag
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject) {
    [self.bridge.uiManager
        addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
            id view = viewRegistry[reactTag];
            if (![view isKindOfClass:[RPSystemBroadcastPickerView class]]) {
                RCTLogError(@"Invalid view returned from registry, expecting "
                            @"RPSystemBroadcastPickerView, got: %@",
                            view);
            } else {
                // Simulate a click
                UIButton *btn = nil;

                for (UIView *subview in ((RPSystemBroadcastPickerView *)view).subviews) {
                    if ([subview isKindOfClass:[UIButton class]]) {
                        btn = (UIButton *)subview;
                    }
                }
                
                NSMutableDictionary *promiseDict = [NSMutableDictionary dictionaryWithDictionary:@{
                    @"resolve": resolve,
                    @"reject": reject
                }];
                
                __weak NSMutableDictionary *weakDict = promiseDict;
                __weak id nsObserver = nil;

                // Observer for picker dismissal
                nsObserver = [[NSNotificationCenter defaultCenter] addObserverForName:UIApplicationDidBecomeActiveNotification object:nil queue:[NSOperationQueue mainQueue] usingBlock:^(NSNotification *notif) {
                    if(weakDict != NULL && [weakDict count] != 0) {
                        // Timeout after 5 seconds, if blockDict still has valid keys.
                        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(5.0 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
                            NSMutableDictionary * blockDict = weakDict;
                            
                            if(blockDict != NULL && [blockDict count] != 0) {
                                RCTPromiseRejectBlock rejecter = blockDict[@"reject"];
                                if(rejecter) {
                                    rejecter(@"ScreenCaptureViewManager.showAwaiting", @"Did not result in broadcast starting!", nil);
                                }
                                clearObservers(blockDict);
                            }
                        });
                    }
                }];
                promiseDict[@"ns_observer"] = nsObserver;
                
                // Observer for broadcast starting
                CFNotificationCenterRef notification = CFNotificationCenterGetDarwinNotifyCenter();
                CFNotificationCenterAddObserver(notification,
                                                (__bridge_retained const void *)promiseDict,
                                                onBroadcastStarted,
                                                CFSTR("iOS_BroadcastStarted"),
                                                NULL,
                                                CFNotificationSuspensionBehaviorDeliverImmediately);
                                
                if (btn != nil) {
                    [btn sendActionsForControlEvents:UIControlEventTouchUpInside];
                } else {
                    RCTLogError(@"RPSystemBroadcastPickerView button not found");
                }
            }
        }];
}

@end

#endif