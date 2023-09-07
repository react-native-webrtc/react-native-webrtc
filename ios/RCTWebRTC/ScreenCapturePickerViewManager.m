#if TARGET_OS_IOS

#import <React/RCTUIManager.h>
#import <ReplayKit/ReplayKit.h>

#import "ScreenCapturePickerViewManager.h"

NSString *const kRTCScreenSharingExtension = @"RTCScreenSharingExtension";

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

@end

#endif