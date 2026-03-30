#ifdef RCT_NEW_ARCH_ENABLED

#import <React/RCTFabricComponentsPlugins.h>
#import <ReplayKit/ReplayKit.h>

#import "ScreenCapturePickerViewComponentView.h"

NSString *const kRTCScreenSharingExtensionFabric = @"RTCScreenSharingExtension";

@interface ScreenCapturePickerViewComponentView ()
@end

@implementation ScreenCapturePickerViewComponentView {
    RPSystemBroadcastPickerView *_broadcastPickerView;
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _broadcastPickerView = [[RPSystemBroadcastPickerView alloc] initWithFrame:frame];
        _broadcastPickerView.preferredExtension = [self preferredExtension];
        _broadcastPickerView.showsMicrophoneButton = false;
        _broadcastPickerView.userInteractionEnabled = false;
        self.contentView = _broadcastPickerView;
    }
    return self;
}

- (NSString *)preferredExtension {
    NSDictionary *infoDictionary = [[NSBundle mainBundle] infoDictionary];
    return infoDictionary[kRTCScreenSharingExtensionFabric];
}

- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args {
    if ([commandName isEqual:@"show"]) {
        UIButton *btn = nil;
        for (UIView *subview in _broadcastPickerView.subviews) {
            if ([subview isKindOfClass:[UIButton class]]) {
                btn = (UIButton *)subview;
            }
        }
        if (btn != nil) {
            [btn sendActionsForControlEvents:UIControlEventTouchUpInside];
        }
    }
}

@end

Class<RCTComponentViewProtocol> ScreenCapturePickerViewCls(void) {
    return ScreenCapturePickerViewComponentView.class;
}

#endif
