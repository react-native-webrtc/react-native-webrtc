#if TARGET_OS_IOS

#import "BroadcastPickerHelper.h"

#import <UIKit/UIKit.h>

static NSString *const kRTCScreenSharingExtensionKey = @"RTCScreenSharingExtension";
static NSString *const kBroadcastPickerErrorDomain = @"BroadcastPickerHelper";

@implementation BroadcastPickerHelper

+ (BOOL)presentSystemPickerWithError:(NSError **)error {
    NSDictionary *infoDictionary = [[NSBundle mainBundle] infoDictionary];
    NSString *preferredExtension = infoDictionary[kRTCScreenSharingExtensionKey];

    RPSystemBroadcastPickerView *view = [[RPSystemBroadcastPickerView alloc] init];
    view.preferredExtension = preferredExtension;
    view.showsMicrophoneButton = NO;

    return [self tapPickerView:view error:error];
}

+ (BOOL)tapPickerView:(RPSystemBroadcastPickerView *)view error:(NSError **)error {
    UIButton *btn = nil;
    for (UIView *subview in view.subviews) {
        if ([subview isKindOfClass:[UIButton class]]) {
            btn = (UIButton *)subview;
            break;
        }
    }

    if (btn == nil) {
        if (error) {
            *error = [NSError
                errorWithDomain:kBroadcastPickerErrorDomain
                           code:1
                       userInfo:@{NSLocalizedDescriptionKey : @"RPSystemBroadcastPickerView button not found"}];
        }
        return NO;
    }

    [btn sendActionsForControlEvents:UIControlEventTouchUpInside];
    return YES;
}

@end

#endif
