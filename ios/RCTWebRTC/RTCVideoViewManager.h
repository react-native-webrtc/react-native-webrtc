#import <Foundation/Foundation.h>
#import <React/RCTViewManager.h>

/**
 * In the fashion of
 * https://www.w3.org/TR/html5/embedded-content-0.html#dom-video-videowidth
 * and https://www.w3.org/TR/html5/rendering.html#video-object-fit, resembles
 * the CSS style {@code object-fit}.
 */
typedef NS_ENUM(NSInteger, RTCVideoViewObjectFit) {
    /**
     * The contain value defined by https://www.w3.org/TR/css3-images/#object-fit:
     *
     * The replaced content is sized to maintain its aspect ratio while fitting
     * within the element's content box.
     */
    RTCVideoViewObjectFitContain = 1,
    /**
     * The cover value defined by https://www.w3.org/TR/css3-images/#object-fit:
     *
     * The replaced content is sized to maintain its aspect ratio while filling
     * the element's entire content box.
     */
    RTCVideoViewObjectFitCover
};

@interface RTCVideoViewManager : RCTViewManager

@end
