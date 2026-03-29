#ifdef RCT_NEW_ARCH_ENABLED
#import <React/RCTViewComponentView.h>
#import <UIKit/UIKit.h>

#import "RTCVideoViewManager.h"

@class PIPController;
@class RTCVideoTrack;

NS_ASSUME_NONNULL_BEGIN

@interface RTCVideoViewComponentView : RCTViewComponentView

/**
 * The indicator which determines whether this view is to mirror the video during rendering.
 */
@property(nonatomic) BOOL mirror;

/**
 * The object-fit property for the video (contain or cover).
 */
@property(nonatomic) RTCVideoViewObjectFit objectFit;

/**
 * PIP properties (iOS only, requires iOS 15+).
 */
@property(nonatomic) BOOL iosPIPEnabled;
@property(nonatomic) BOOL iosPIPStartAutomatically;
@property(nonatomic) BOOL iosPIPStopAutomatically;
@property(nonatomic) CGFloat iosPIPPreferredWidth;
@property(nonatomic) CGFloat iosPIPPreferredHeight;

/**
 * PIP controller.
 */
@property(nonatomic, strong, nullable) PIPController *pipController API_AVAILABLE(ios(15.0));

/**
 * The video track to render.
 */
@property(nonatomic, strong, nullable) RTCVideoTrack *videoTrack;

/**
 * Event callback for when video dimensions change.
 */
@property(nonatomic, copy, nullable) RCTDirectEventBlock onDimensionsChange;

/**
 * Start Picture in Picture mode.
 */
- (void)startPIP API_AVAILABLE(ios(15.0));

/**
 * Stop Picture in Picture mode.
 */
- (void)stopPIP API_AVAILABLE(ios(15.0));

@end

NS_ASSUME_NONNULL_END
#endif
