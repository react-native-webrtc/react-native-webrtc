//
//  ScreenCaptureController.h
//  RCTWebRTC
//
//  Created by Alex-Dan Bumbu on 06/01/2021.
//

#import <Foundation/Foundation.h>
#import "CaptureController.h"

NS_ASSUME_NONNULL_BEGIN

extern NSString* const kRTCScreensharingSocketFD;
extern NSString* const kRTCAppGroupIdentifier;

@class ScreenCapturer;

@interface ScreenCaptureController : CaptureController

- (instancetype)initWithCapturer:(nonnull ScreenCapturer *)capturer;
- (void)startCapture;
- (void)stopCapture;

@end

NS_ASSUME_NONNULL_END
