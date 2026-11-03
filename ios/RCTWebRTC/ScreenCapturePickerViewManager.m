#if TARGET_OS_IOS

#import <React/RCTLog.h>
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
    dispatch_async(dispatch_get_main_queue(), ^{
        RPSystemBroadcastPickerView *picker = self->_broadcastPickerView;
        if (![picker isKindOfClass:[RPSystemBroadcastPickerView class]]) {
            RCTLogError(@"Invalid broadcast picker view, expecting "
                        @"RPSystemBroadcastPickerView, got: %@",
                        picker);
            return;
        }

        UIButton *btn = nil;

        for (UIView *subview in picker.subviews) {
            if ([subview isKindOfClass:[UIButton class]]) {
                btn = (UIButton *)subview;
            }
        }
        if (btn != nil) {
            [btn sendActionsForControlEvents:UIControlEventTouchUpInside];
        } else {
            RCTLogError(@"RPSystemBroadcastPickerView button not found");
        }
    });
}

@end

#endif