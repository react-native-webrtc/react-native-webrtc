//
//  RTCVideoViewManager.m
//  TestReact
//
//  Created by one on 2015/9/25.
//  Copyright © 2015年 Facebook. All rights reserved.
//
#import <AVFoundation/AVFoundation.h>
#import <objc/runtime.h>

#import <WebRTC/RTCEAGLVideoView.h>
#import <WebRTC/RTCMediaStream.h>
#import <WebRTC/RTCVideoFrame.h>
#import <WebRTC/RTCVideoTrack.h>

#import "RTCVideoViewManager.h"
#import "WebRTCModule.h"

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
  RTCVideoViewObjectFitContain,
  /**
   * The cover value defined by https://www.w3.org/TR/css3-images/#object-fit:
   *
   * The replaced content is sized to maintain its aspect ratio while filling
   * the element's entire content box.
   */
  RTCVideoViewObjectFitCover
};

/**
 * Implements an equivalent of {@code HTMLVideoElement} i.e. Web's video
 * element.
 */
@interface RTCVideoView : UIView <RTCVideoRenderer, RTCEAGLVideoViewDelegate>

/**
 * In the fashion of
 * https://www.w3.org/TR/html5/embedded-content-0.html#dom-video-videowidth
 * and https://www.w3.org/TR/html5/rendering.html#video-object-fit, resembles
 * the CSS style {@code object-fit}.
 */
@property (nonatomic) RTCVideoViewObjectFit objectFit;

/**
 * The {@link RTCEAGLVideoView} which implements the actual
 * {@link RTCVideoRenderer} of this instance and which this instance fits within
 * itself so that the rendered video preserves the aspect ratio of
 * {@link #_videoSize}.
 */
@property (nonatomic, readonly) RTCEAGLVideoView *subview;

/**
 * The {@link RTCVideoTrack}, if any, which this instance renders.
 */
@property (nonatomic, strong) RTCVideoTrack *videoTrack;

@end

@implementation RTCVideoView {
  /**
   * The width and height of the video (frames) rendered by {@link #subview}.
   */
  CGSize _videoSize;
}

/**
 * Tells this view that its window object changed.
 */
- (void)didMoveToWindow {
  [super didMoveToWindow];

  // XXX This RTCVideoView strongly retains its videoTrack. The latter strongly
  // retains the former as well though because RTCVideoTrack strongly retains
  // the RTCVideoRenderers added to it. In other words, there is a cycle of
  // strong retainments and, consequently, there is a memory leak. In order to
  // break the cycle, have this RTCVideoView as the RTCVideoRenderer of its
  // videoTrack only while this view resides in a window.
  RTCVideoTrack *videoTrack = self.videoTrack;

  if (videoTrack) {
    if (self.window) {
      // TODO RTCVideoTrack's addRenderer implementation has an NSAssert1 that
      // makes sure that the specified RTCVideoRenderer is not added multiple
      // times (without intervening removals, of course). It may (or may not) be
      // wise to explicitly make sure here that we will not hit that NSAssert1.
      [videoTrack addRenderer:self];
    } else {
      [videoTrack removeRenderer:self];
    }
  }
}

/**
 * Invalidates the current layout of the receiver and triggers a layout update
 * during the next update cycle. Make sure that the method call is performed on
 * the application's main thread (as documented to be necessary by Apple).
 */
- (void)dispatchAsyncSetNeedsLayout {
  __weak UIView *weakSelf = self;
  dispatch_async(dispatch_get_main_queue(), ^{
    UIView *strongSelf = weakSelf;
    [strongSelf setNeedsLayout];
  });
}

/**
 * Initializes and returns a newly allocated view object with the specified
 * frame rectangle.
 *
 * @param frame The frame rectangle for the view, measured in points.
 */
- (instancetype)initWithFrame:(CGRect)frame {
  if (self = [super initWithFrame:frame]) {
    RTCEAGLVideoView *subview = [[RTCEAGLVideoView alloc] init];

    subview.delegate = self;

    _videoSize.height = 0;
    _videoSize.width = 0;

    self.opaque = NO;
    [self addSubview:subview];
  }
  return self;
}

/**
 * Lays out the subview of this instance while preserving the aspect ratio of
 * the video it renders.
 */
- (void)layoutSubviews {
  [super layoutSubviews];

  UIView *subview = self.subview;
  if (!subview) {
    return;
  }

  CGFloat width = _videoSize.width, height = _videoSize.height;
  CGRect newValue;
  if (width <= 0 || height <= 0) {
    newValue.origin.x = 0;
    newValue.origin.y = 0;
    newValue.size.width = 0;
    newValue.size.height = 0;
  } else if (RTCVideoViewObjectFitCover == self.objectFit) { // cover
    newValue = self.bounds;
    // Is there a real need to scale subview?
    if (newValue.size.width != width || newValue.size.height != height) {
      CGFloat scaleFactor
        = MAX(newValue.size.width / width, newValue.size.height / height);
      // Scale both width and height in order to make it obvious that the aspect
      // ratio is preserved.
      width *= scaleFactor;
      height *= scaleFactor;
      newValue.origin.x += (newValue.size.width - width) / 2.0;
      newValue.origin.y += (newValue.size.height - height) / 2.0;
      newValue.size.width = width;
      newValue.size.height = height;
    }
  } else { // contain
    // The implementation is in accord with
    // https://www.w3.org/TR/html5/embedded-content-0.html#the-video-element:
    //
    // In the absence of style rules to the contrary, video content should be
    // rendered inside the element's playback area such that the video content
    // is shown centered in the playback area at the largest possible size that
    // fits completely within it, with the video content's aspect ratio being
    // preserved. Thus, if the aspect ratio of the playback area does not match
    // the aspect ratio of the video, the video will be shown letterboxed or
    // pillarboxed. Areas of the element's playback area that do not contain the
    // video represent nothing.
    newValue
      = AVMakeRectWithAspectRatioInsideRect(
          CGSizeMake(width, height),
          self.bounds);
  }

  CGRect oldValue = subview.frame;
  if (newValue.origin.x != oldValue.origin.x
      || newValue.origin.y != oldValue.origin.y
      || newValue.size.width != oldValue.size.width
      || newValue.size.height != oldValue.size.height) {
    subview.frame = newValue;
  }
}

/**
 * Implements the setter of the {@link #objectFit} property of this
 * {@code RTCVideoView}.
 *
 * @param objectFit The value to set on the {@code objectFit} property of this
 * {@code RTCVideoView}.
 */
- (void)setObjectFit:(RTCVideoViewObjectFit)objectFit {
  if (_objectFit != objectFit) {
      _objectFit = objectFit;
      [self dispatchAsyncSetNeedsLayout];
  }
}

/**
 * Implements the setter of the {@link #videoTrack} property of this
 * {@code RTCVideoView}.
 *
 * @param videoTrack The value to set on the {@code videoTrack} property of this
 * {@code RTCVideoView}.
 */
- (void)setVideoTrack:(RTCVideoTrack *)videoTrack {
  RTCVideoTrack *oldValue = self.videoTrack;

  if (oldValue != videoTrack) {
    if (oldValue) {
      [oldValue removeRenderer:self];
    }

    _videoTrack = videoTrack;

    // XXX This RTCVideoView strongly retains its videoTrack. The latter
    // strongly retains the former as well though because RTCVideoTrack strongly
    // retains the RTCVideoRenderers added to it. In other words, there is a
    // cycle of strong retainments and, consequently, there is a memory leak. In
    // order to break the cycle, have this RTCVideoView as the RTCVideoRenderer
    // of its videoTrack only while this view resides in a window.
    if (videoTrack && self.window) {
      [videoTrack addRenderer:self];
    }
  }
}

/**
 * Implements the getter of the {@code subview} property of this
 * {@code RTCVideoView}. Gets the {@link RTCEAGLVideoView} subview of this
 * {@code RTCVideoView} which implements the actual {@link RTCVideoRenderer} of
 * this instance and which actually renders {@link #videoTrack}.
 *
 * @returns The {@code RTCEAGLVideoView} subview of this {@code RTCVideoView}
 * which implements the actual {@code RTCVideoRenderer} of this instance and
 * which actually renders {@code videoTrack}.
 */
- (RTCEAGLVideoView *)subview {
  // In order to reduce the number of strong retainments of the RTCEAGLVideoView
  // instance and, thus, the risk of memory leaks, retrieve the subview from the
  // super's list of subviews of this view.
  for (UIView *subview in self.subviews) {
    if ([subview isKindOfClass:[RTCEAGLVideoView class]]) {
      return (RTCEAGLVideoView *)subview;
    }
  }
  return nil;
}

#pragma mark - RTCVideoRenderer methods

/**
 * Renders a specific video frame. Delegates to the subview of this instance
 * which implements the actual {@link RTCVideoRenderer}.
 *
 * @param frame The video frame to render.
 */
- (void)renderFrame:(RTCVideoFrame *)frame {
  id<RTCVideoRenderer> videoRenderer = self.subview;
  if (videoRenderer) {
    [videoRenderer renderFrame:frame];
  }
}

/**
 * Sets the size of the video frame to render.
 *
 * @param size The size of the video frame to render.
 */
- (void)setSize:(CGSize)size {
  id<RTCVideoRenderer> videoRenderer = self.subview;
  if (videoRenderer) {
    [videoRenderer setSize:size];
  }
}

#pragma mark - RTCEAGLVideoViewDelegate methods

/**
 * Notifies this {@link RTCEAGLVideoViewDelegate} that a specific
 * {@link RTCEAGLVideoView} had the size of the video (frames) it renders
 * changed.
 *
 * @param videoView The {@code RTCEAGLVideoView} which had the size of the video
 * (frames) it renders changed to the specified size.
 * @param size The new size of the video (frames) to be rendered by the
 * specified {@code videoView}.
 */
- (void)videoView:(RTCEAGLVideoView *)videoView didChangeVideoSize:(CGSize)size {
  if (videoView == self.subview) {
    _videoSize = size;
    [self dispatchAsyncSetNeedsLayout];
  }
}

@end

@implementation RTCVideoViewManager

RCT_EXPORT_MODULE()

- (UIView *)view {
  RTCVideoView *v = [[RTCVideoView alloc] init];
  v.clipsToBounds = YES;
  return v;
}

- (dispatch_queue_t)methodQueue {
  return dispatch_get_main_queue();
}

/**
 * In the fashion of
 * https://www.w3.org/TR/html5/embedded-content-0.html#dom-video-videowidth
 * and https://www.w3.org/TR/html5/rendering.html#video-object-fit, resembles
 * the CSS style {@code object-fit}.
 */
RCT_CUSTOM_VIEW_PROPERTY(objectFit, NSString *, RTCVideoView) {
  NSString *s = [RCTConvert NSString:json];
  RTCVideoViewObjectFit e
    = (s && [s isEqualToString:@"cover"])
      ? RTCVideoViewObjectFitCover
      : RTCVideoViewObjectFitContain;

  view.objectFit = e;
}

RCT_CUSTOM_VIEW_PROPERTY(streamURL, NSNumber, RTCVideoView) {
  RTCVideoTrack *videoTrack;

  if (json) {
    NSString *streamId = (NSString *)json;

    WebRTCModule *module = [self.bridge moduleForName:@"WebRTCModule"];
    RTCMediaStream *stream = module.mediaStreams[streamId];
    NSArray *videoTracks = stream.videoTracks;

    videoTrack = videoTracks.count ? videoTracks[0] : nil;
  } else {
    videoTrack = nil;
  }

  view.videoTrack = videoTrack;
}

@end
