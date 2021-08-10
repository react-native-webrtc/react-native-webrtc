//
//  RTCVideoViewManager.m
//  TestReact
//
//  Created by one on 2015/9/25.
//  Copyright © 2015年 Facebook. All rights reserved.
//
#import <AVFoundation/AVFoundation.h>
#import <objc/runtime.h>

#import <React/RCTLog.h>
#if !TARGET_OS_OSX
#import <WebRTC/RTCEAGLVideoView.h>
#endif
#import <WebRTC/RTCMediaStream.h>
#if !TARGET_OS_OSX
#import <WebRTC/RTCMTLVideoView.h>
#else
#import <WebRTC/RTCMTLNSVideoView.h>
#endif
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

#if !TARGET_OS_OSX
@interface RTCVideoView : UIView <RTCVideoRenderer, RTCVideoViewDelegate>
#else
@interface RTCVideoView : NSView <RTCVideoViewDelegate>
#endif

/**
 * The indicator which determines whether this {@code RTCVideoView} is to mirror
 * the video specified by {@link #videoTrack} during its rendering. Typically,
 * applications choose to mirror the front/user-facing camera.
 */
@property (nonatomic) BOOL mirror;

/**
 * In the fashion of
 * https://www.w3.org/TR/html5/embedded-content-0.html#dom-video-videowidth
 * and https://www.w3.org/TR/html5/rendering.html#video-object-fit, resembles
 * the CSS style {@code object-fit}.
 */
@property (nonatomic) RTCVideoViewObjectFit objectFit;

@property (nonatomic, copy) RCTDirectEventBlock onFirstFrame;

/**
 * The {@link RRTCVideoRenderer} which implements the actual rendering and which
 * fits within this view so that the rendered video preserves the aspect ratio of
 * {@link #_videoSize}.
 */
#if !TARGET_OS_OSX
@property (nonatomic, readonly) __kindof UIView<RTCVideoRenderer> *videoView;
#else
@property (nonatomic, readonly) __kindof NSView<RTCVideoRenderer> *videoView;
#endif

/**
 * The {@link RTCVideoTrack}, if any, which this instance renders.
 */
@property (nonatomic, strong) RTCVideoTrack *videoTrack;

/**
 * Reference to the main WebRTC RN module.
 */
@property (nonatomic, weak) WebRTCModule *module;

@end

@implementation RTCVideoView {
  /**
   * The width and height of the video (frames) rendered by {@link #subview}.
   */
  CGSize _videoSize;
  BOOL firstFrameRendered;
}

@synthesize videoView = _videoView;

/**
 * Tells this view that its window object changed.
 */
- (void)didMoveToWindow {
  firstFrameRendered = NO;
  // XXX This RTCVideoView strongly retains its videoTrack. The latter strongly
  // retains the former as well though because RTCVideoTrack strongly retains
  // the RTCVideoRenderers added to it. In other words, there is a cycle of
  // strong retainments and, consequently, there is a memory leak. In order to
  // break the cycle, have this RTCVideoView as the RTCVideoRenderer of its
  // videoTrack only while this view resides in a window.
  RTCVideoTrack *videoTrack = self.videoTrack;

  if (videoTrack) {
    if (self.window) {
      dispatch_async(_module.workerQueue, ^{
        [videoTrack addRenderer:self];
      });
    } else {
      dispatch_async(_module.workerQueue, ^{
        [videoTrack removeRenderer:self];
      });
      _videoSize.height = 0;
      _videoSize.width = 0;
#if !TARGET_OS_OSX
      [self setNeedsLayout];
#else
        self.needsLayout = YES;
#endif
    }
  }
}

/**
 * Initializes and returns a newly allocated view object with the specified
 * frame rectangle.
 *
 * @param frame The frame rectangle for the view, measured in points.
 */
- (instancetype)initWithFrame:(CGRect)frame {
  if (self = [super initWithFrame:frame]) {
#if defined(RTC_SUPPORTS_METAL)
#if !TARGET_OS_OSX
    RTCMTLVideoView *subview = [[RTCMTLVideoView alloc] initWithFrame:CGRectZero];
    subview.delegate = self;
    _videoView = subview;
#else
      RTCMTLNSVideoView *subview = [[RTCMTLNSVideoView alloc] initWithFrame:CGRectZero];
      subview.wantsLayer = true;
      subview.delegate = self;
      _videoView = subview;
#endif
#else
#if !TARGET_OS_OSX
    RTCEAGLVideoView *subview = [[RTCEAGLVideoView alloc] initWithFrame:CGRectZero];
    subview.delegate = self;
    _videoView = subview;
#else
            RTCMTLNSVideoView *subview = [[RTCMTLNSVideoView alloc] initWithFrame:CGRectZero];
      subview.wantsLayer = true;
            subview.delegate = self;
            _videoView = subview;
      #endif
#endif

    _videoSize.height = 0;
    _videoSize.width = 0;


    #if !TARGET_OS_OSX
            self.opaque = NO;
    #endif
    [self addSubview:self.videoView];
  }
  return self;
}

/**
 * Lays out the subview of this instance while preserving the aspect ratio of
 * the video it renders.
 */

#if !TARGET_OS_OSX
- (void)layoutSubviews {
#else
    - (void)layout {
#endif
#if !TARGET_OS_OSX
  UIView *subview = self.videoView;
#else
  NSView *subview = self.videoView;
#endif
  if (!subview) {
    return;
  }

  CGFloat width = _videoSize.width, height = _videoSize.height;
  CGRect newValue;
  if (width <= 0 || height <= 0) {
    newValue = self.bounds;
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

  [subview.layer setAffineTransform:self.mirror
  ? CGAffineTransformMakeScale(-1.0, 1.0)
                                   : CGAffineTransformIdentity];
}

/**
 * Implements the setter of the {@link #mirror} property of this
 * {@code RTCVideoView}.
 *
 * @param mirror The value to set on the {@code mirror} property of this
 * {@code RTCVideoView}.
 */
- (void)setMirror:(BOOL)mirror {
  if (_mirror != mirror) {
      _mirror = mirror;
      
      #if !TARGET_OS_OSX
            [self setNeedsLayout];
      #else
            self.needsLayout = YES;
      #endif
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
      
      #if !TARGET_OS_OSX
            [self setNeedsLayout];
      #else
            self.needsLayout = YES;
      #endif
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
      dispatch_async(_module.workerQueue, ^{
        [oldValue removeRenderer:self];
      });
      _videoSize.height = 0;
      _videoSize.width = 0;
      
      #if !TARGET_OS_OSX
            [self setNeedsLayout];
      #else
            self.needsLayout = YES;
      #endif
    }

    _videoTrack = videoTrack;

    // XXX This RTCVideoView strongly retains its videoTrack. The latter
    // strongly retains the former as well though because RTCVideoTrack strongly
    // retains the RTCVideoRenderers added to it. In other words, there is a
    // cycle of strong retainments and, consequently, there is a memory leak. In
    // order to break the cycle, have this RTCVideoView as the RTCVideoRenderer
    // of its videoTrack only while this view resides in a window.
    if (videoTrack && self.window) {
        dispatch_async(_module.workerQueue, ^{
            [videoTrack addRenderer:self];
        });
    }
  }
}

#pragma mark - RTCVideoRenderer methods

/**
 * Renders a specific video frame. Delegates to the subview of this instance
 * which implements the actual {@link RTCVideoRenderer}.
 *
 * @param frame The video frame to render.
 */
- (void)renderFrame:(RTCVideoFrame *)frame {
  id<RTCVideoRenderer> videoRenderer = self.videoView;
    if (self.onFirstFrame && !firstFrameRendered) {
            firstFrameRendered = YES;
      self.onFirstFrame(@{});
    }
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
    id<RTCVideoRenderer> videoRenderer = self.videoView;
    if (videoRenderer) {
        [videoRenderer setSize:size];
    }
}

#pragma mark - RTCVideoViewDelegate methods

/**
 * Notifies this {@link RTCVideoViewDelegate} that a specific
 * {@link RTCVideoRenderer} had the size of the video (frames) it renders
 * changed.
 *
 * @param videoView The {@code RTCVideoRenderer} which had the size of the video
 * (frames) it renders changed to the specified size.
 * @param size The new size of the video (frames) to be rendered by the
 * specified {@code videoView}.
 */
- (void)videoView:(id<RTCVideoRenderer>)videoView didChangeVideoSize:(CGSize)size {
  if (videoView == self.videoView) {
    _videoSize = size;
    
    #if !TARGET_OS_OSX
          [self setNeedsLayout];
    #else
          self.needsLayout = YES;
    #endif
  }
}

@end

@implementation RTCVideoViewManager

RCT_EXPORT_MODULE()

RCT_EXPORT_VIEW_PROPERTY(onFirstFrame, RCTDirectEventBlock)

#if !TARGET_OS_OSX
- (UIView *)view {
#else
- (NSView *)view {
#endif
  RTCVideoView *v = [[RTCVideoView alloc] init];
  v.module = [self.bridge moduleForName:@"WebRTCModule"];
#if !TARGET_OS_OSX
  v.clipsToBounds = YES;
#endif
  return v;
}

- (dispatch_queue_t)methodQueue {
  return dispatch_get_main_queue();
}

RCT_EXPORT_VIEW_PROPERTY(mirror, BOOL)

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

RCT_CUSTOM_VIEW_PROPERTY(streamURL, NSString *, RTCVideoView) {
    if (!json) {
        view.videoTrack = nil;
        return;
    }

    NSString *streamReactTag = (NSString *)json;
    WebRTCModule *module = view.module;

    dispatch_async(module.workerQueue, ^{
        RTCMediaStream *stream = [module streamForReactTag:streamReactTag];
        NSArray *videoTracks = stream ? stream.videoTracks : @[];
        RTCVideoTrack *videoTrack = [videoTracks firstObject];
        if (!videoTrack) {
            RCTLogWarn(@"No video stream for react tag: %@", streamReactTag);
        } else {
            dispatch_async(dispatch_get_main_queue(), ^{
                view.videoTrack = videoTrack;
            });
        }
    });
}

+ (BOOL)requiresMainQueueSetup
{
    return NO;
}

@end
