//
//  CaptureController.h
//  RCTWebRTC
//
//  Created by Alex-Dan Bumbu on 11/01/2021.
//

#import <Foundation/Foundation.h>
#import "CapturerEventsDelegate.h"

NS_ASSUME_NONNULL_BEGIN

@interface CaptureController: NSObject

@property (nonatomic, strong) id<CapturerEventsDelegate> eventsDelegate;

- (void)startCapture;
- (void)stopCapture;

@end

NS_ASSUME_NONNULL_END
