#import <AVFoundation/AVFoundation.h>
#import <objc/runtime.h>

#import <React/RCTLog.h>
#import <React/RCTUIManager.h>
#import <React/RCTView.h>

#import <WebRTC/RTCMediaStream.h>
#if TARGET_OS_OSX
#import <WebRTC/RTCMTLNSVideoView.h>
#else
#import <WebRTC/RTCMTLVideoView.h>
#endif
#import <WebRTC/RTCCVPixelBuffer.h>
#import <WebRTC/RTCVideoFrame.h>
#import <WebRTC/RTCVideoTrack.h>

#import "PIPController.h"
#import "RTCVideoViewManager.h"
#import "WebRTCModule.h"

/**
 * Implements an equivalent of {@code HTMLVideoElement} i.e. Web's video
 * element.
 */
@interface RTCVideoView : RCTView<PIPControllerDelegate, RTCVideoViewDelegate>

/**
 * The indicator which determines whether this {@code RTCVideoView} is to mirror
 * the video specified by {@link #videoTrack} during its rendering. Typically,
 * applications choose to mirror the front/user-facing camera.
 */
@property(nonatomic) BOOL mirror;

@property(nonatomic) BOOL pictureInPictureEnabled;

@property(nonatomic) BOOL autoStartPictureInPicture;

@property(nonatomic) BOOL autoStopPictureInPicture;

@property(nonatomic, assign) CGSize pictureInPicturePreferredSize;

@property(nonatomic, copy) RCTBubblingEventBlock onPictureInPictureChange;

/**
 * In the fashion of
 * https://www.w3.org/TR/html5/embedded-content-0.html#dom-video-videowidth
 * and https://www.w3.org/TR/html5/rendering.html#video-object-fit, resembles
 * the CSS style {@code object-fit}.
 */
@property(nonatomic) RTCVideoViewObjectFit objectFit;

@property(nonatomic, strong) API_AVAILABLE(ios(15.0)) PIPController *pipController;

/**
 * The {@link RRTCVideoRenderer} which implements the actual rendering.
 */
#if TARGET_OS_OSX
@property(nonatomic, readonly) RTCMTLNSVideoView *videoView;
#else
@property(nonatomic, readonly) RTCMTLVideoView *videoView;
#endif

// Add a reference to the view manager
@property(nonatomic, weak) RTCVideoViewManager *viewManager;

/**
 * The {@link RTCVideoTrack}, if any, which this instance renders.
 */
@property(nonatomic, strong) RTCVideoTrack *videoTrack;

/**
 * Reference to the main WebRTC RN module.
 */
@property(nonatomic, weak) WebRTCModule *module;

@property(nonatomic, copy) RCTDirectEventBlock onDimensionsChange;

@end

@implementation RTCVideoView

@synthesize videoView = _videoView;
@synthesize pipController = _pipController;

/**
 * Tells this view that its window object changed.
 */
- (void)didMoveToWindow {
    // This RTCVideoView strongly retains its videoTrack. The latter strongly
    // retains the former as well though because RTCVideoTrack strongly retains
    // the RTCVideoRenderers added to it. In other words, there is a cycle of
    // strong retainments. In order to break the cycle, and avoid a leak,
    // have this RTCVideoView as the RTCVideoRenderer of its
    // videoTrack only while this view resides in a window.
    RTCVideoTrack *videoTrack = self.videoTrack;

    if (videoTrack) {
        if (self.window) {
            dispatch_async(_module.workerQueue, ^{
                [videoTrack addRenderer:self.videoView];
            });
        } else {
            dispatch_async(_module.workerQueue, ^{
                [videoTrack removeRenderer:self.videoView];
            });
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
#if TARGET_OS_OSX
        RTCMTLNSVideoView *subview = [[RTCMTLNSVideoView alloc] initWithFrame:CGRectZero];
        subview.wantsLayer = true;
        _videoView = subview;
#else
        RTCMTLVideoView *subview = [[RTCMTLVideoView alloc] initWithFrame:CGRectZero];
        _videoView = subview;
#endif
        _objectFit = RTCVideoViewObjectFitCover;
        _autoStartPictureInPicture = YES;
        _autoStopPictureInPicture = YES;
        [self addSubview:self.videoView];
        self.videoView.delegate = self;
    }

    return self;
}

#if TARGET_OS_OSX
- (void)layout {
    [super layout];
#else
- (void)layoutSubviews {
    [super layoutSubviews];
#endif

    CGRect bounds = self.bounds;
    self.videoView.frame = bounds;
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

        self.videoView.transform = mirror ? CGAffineTransformMakeScale(-1.0, 1.0) : CGAffineTransformIdentity;
    }
}

- (void)setPictureInPictureEnabled:(BOOL)pictureInPictureEnabled {
    _pictureInPictureEnabled = pictureInPictureEnabled;
    if (@available(iOS 15.0, *)) {
        [self applyPictureInPictureParams];
    }
}

- (void)setAutoStartPictureInPicture:(BOOL)autoStartPictureInPicture {
    if (_autoStartPictureInPicture != autoStartPictureInPicture) {
        _autoStartPictureInPicture = autoStartPictureInPicture;
        if (@available(iOS 15.0, *)) {
            [self applyPictureInPictureParams];
        }
    }
}

- (void)setAutoStopPictureInPicture:(BOOL)autoStopPictureInPicture {
    if (_autoStopPictureInPicture != autoStopPictureInPicture) {
        _autoStopPictureInPicture = autoStopPictureInPicture;
        if (_autoStartPictureInPicture) {
            if (@available(iOS 15.0, *)) {
                [self applyPictureInPictureParams];
            }
        }
    }
}

- (void)setPictureInPicturePreferredSize:(CGSize)pictureInPicturePreferredSize {
    if (!CGSizeEqualToSize(_pictureInPicturePreferredSize, pictureInPicturePreferredSize)) {
        _pictureInPicturePreferredSize = pictureInPicturePreferredSize;
        if (_autoStartPictureInPicture) {
            if (@available(iOS 15.0, *)) {
                [self applyPictureInPictureParams];
            }
        }
    }
}

- (void)insertReactSubview:(UIView *)subview atIndex:(NSInteger)atIndex {
    // All subviews are treated as fallback views
    [_pipController insertFallbackView:subview];
}

- (void)API_AVAILABLE(ios(15.0))setPIPOptions:(NSDictionary *)pipOptions {
    if (!pipOptions) {
        _pipController = nil;
        return;
    }

    RCTLogWarn(@"'iosPIP' is deprecated. Please use the new Picture-in-Picture props.");

    BOOL enabled = YES;
    BOOL startAutomatically = YES;
    BOOL stopAutomatically = YES;

    CGSize preferredSize = CGSizeZero;

    if ([pipOptions objectForKey:@"enabled"]) {
        enabled = [pipOptions[@"enabled"] boolValue];
    }
    if ([pipOptions objectForKey:@"startAutomatically"]) {
        startAutomatically = [pipOptions[@"startAutomatically"] boolValue];
    }
    if ([pipOptions objectForKey:@"stopAutomatically"]) {
        stopAutomatically = [pipOptions[@"stopAutomatically"] boolValue];
    }
    if ([pipOptions objectForKey:@"preferredSize"]) {
        NSDictionary *sizeDict = pipOptions[@"preferredSize"];
        id width = sizeDict[@"width"];
        id height = sizeDict[@"height"];

        if ([width isKindOfClass:[NSNumber class]] && [height isKindOfClass:[NSNumber class]]) {
            preferredSize = CGSizeMake([width doubleValue], [height doubleValue]);
        }
    }

    if (!enabled) {
        _pipController = nil;
        return;
    }

    if (!_pipController) {
        _pipController = [[PIPController alloc] initWithSourceView:self];
        _pipController.videoTrack = _videoTrack;
        _pipController.delegate = self;
    }

    _pipController.startAutomatically = startAutomatically;
    _pipController.stopAutomatically = stopAutomatically;
    _pipController.objectFit = _objectFit;
    _pipController.preferredSize = preferredSize;
}

- (void)API_AVAILABLE(ios(15.0))applyPictureInPictureParams {
    if (!_pictureInPictureEnabled) {
        _pipController = nil;
        return;
    }

    if (!_pipController) {
        _pipController = [[PIPController alloc] initWithSourceView:self];
        _pipController.videoTrack = _videoTrack;
        _pipController.delegate = self;
    }

    if (!CGSizeEqualToSize(_pictureInPicturePreferredSize, CGSizeZero)) {
        _pipController.preferredSize = _pictureInPicturePreferredSize;
    }

    _pipController.startAutomatically = _autoStartPictureInPicture;
    _pipController.stopAutomatically = _autoStopPictureInPicture;
    _pipController.objectFit = _objectFit;
}

- (void)API_AVAILABLE(ios(15.0))startPIPWithParams:(BOOL)shouldApplyParams {
    if (shouldApplyParams) {
        [self applyPictureInPictureParams];
    }
    [_pipController startPIP];
}

- (void)API_AVAILABLE(ios(15.0))stopPIP {
    [_pipController stopPIP];
}

/**
 * Implements the setter of the {@link #objectFit} property of this
 * {@code RTCVideoView}.
 *
 * @param objectFit The value to set on the {@code objectFit} property of this
 * {@code RTCVideoView}.
 */
- (void)setObjectFit:(RTCVideoViewObjectFit)fit {
    if (_objectFit != fit) {
        _objectFit = fit;

#if !TARGET_OS_OSX
        if (fit == RTCVideoViewObjectFitCover) {
            self.videoView.videoContentMode = UIViewContentModeScaleAspectFill;
        } else {
            self.videoView.videoContentMode = UIViewContentModeScaleAspectFit;
        }
#endif
        if (@available(iOS 15.0, *)) {
            _pipController.objectFit = fit;
        }
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
                [oldValue removeRenderer:self.videoView];
            });
        }

        [_pipController setVideoTrack:videoTrack];
        _videoTrack = videoTrack;

        // Clear the videoView by rendering a 2x2 blank frame.
        CVPixelBufferRef pixelBuffer;
        CVReturn err = CVPixelBufferCreate(NULL, 2, 2, kCVPixelFormatType_32BGRA, NULL, &pixelBuffer);
        if (err == kCVReturnSuccess) {
            const int kBytesPerPixel = 4;
            CVPixelBufferLockBaseAddress(pixelBuffer, 0);
            int bufferWidth = (int)CVPixelBufferGetWidth(pixelBuffer);
            int bufferHeight = (int)CVPixelBufferGetHeight(pixelBuffer);
            size_t bytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer);
            uint8_t *baseAddress = CVPixelBufferGetBaseAddress(pixelBuffer);

            for (int row = 0; row < bufferHeight; row++) {
                uint8_t *pixel = baseAddress + row * bytesPerRow;
                for (int column = 0; column < bufferWidth; column++) {
                    pixel[0] = 0;  // BGRA, Blue value
                    pixel[1] = 0;  // Green value
                    pixel[2] = 0;  // Red value
                    pixel[3] = 0;  // Alpha value
                    pixel += kBytesPerPixel;
                }
            }

            CVPixelBufferUnlockBaseAddress(pixelBuffer, 0);
            int64_t time = (int64_t)(CFAbsoluteTimeGetCurrent() * 1000000000);
            RTCCVPixelBuffer *buffer = [[RTCCVPixelBuffer alloc] initWithPixelBuffer:pixelBuffer];
            RTCVideoFrame *frame = [[[RTCVideoFrame alloc] initWithBuffer:buffer
                                                                 rotation:RTCVideoRotation_0
                                                              timeStampNs:time] newI420VideoFrame];

            [self.videoView renderFrame:frame];

            CVPixelBufferRelease(pixelBuffer);
        }

        // See "didMoveToWindow" above.
        if (videoTrack && self.window) {
            dispatch_async(_module.workerQueue, ^{
                [videoTrack addRenderer:self.videoView];
            });
        }
    }
}

#pragma mark PIPControllerDelegate

- (void)didChangePictureInPicture:(BOOL)isInPictureInPicture {
    if (self.onPictureInPictureChange) {
        self.onPictureInPictureChange(@{@"isInPictureInPicture" : @(isInPictureInPicture)});
    }
}

#pragma mark RTCVideoViewDelegate
- (void)videoView:(id)videoView didChangeVideoSize:(CGSize)size {
    // Capture the callback block to avoid accessing it across threads
    RCTDirectEventBlock callback = self.onDimensionsChange;
    if (callback) {
        NSDictionary *eventData = @{@"width" : @(size.width), @"height" : @(size.height)};

        dispatch_async(dispatch_get_main_queue(), ^{
            callback(eventData);
        });
    }
}

@end

@implementation RTCVideoViewManager

RCT_EXPORT_MODULE()

- (RCTView *)view {
    RTCVideoView *v = [[RTCVideoView alloc] init];
    v.module = [self.bridge moduleForName:@"WebRTCModule"];
    v.viewManager = self;
    v.clipsToBounds = YES;
    return v;
}

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

#pragma mark - View properties

RCT_EXPORT_VIEW_PROPERTY(mirror, BOOL)

/**
 * In the fashion of
 * https://www.w3.org/TR/html5/embedded-content-0.html#dom-video-videowidth
 * and https://www.w3.org/TR/html5/rendering.html#video-object-fit, resembles
 * the CSS style {@code object-fit}.
 */
RCT_CUSTOM_VIEW_PROPERTY(objectFit, NSString *, RTCVideoView) {
    NSString *fitStr = json;
    RTCVideoViewObjectFit fit =
        (fitStr && [fitStr isEqualToString:@"cover"]) ? RTCVideoViewObjectFitCover : RTCVideoViewObjectFitContain;

    view.objectFit = fit;
}

RCT_EXPORT_VIEW_PROPERTY(onDimensionsChange, RCTDirectEventBlock)

RCT_CUSTOM_VIEW_PROPERTY(streamURL, NSString *, RTCVideoView) {
    if (!json) {
        view.videoTrack = nil;
        return;
    }

    NSString *streamReactTag = json;
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

RCT_CUSTOM_VIEW_PROPERTY(iosPIP, NSDictionary *, RTCVideoView) {
    if (@available(iOS 15.0, *)) {
        [view setPIPOptions:json];
    }
}

RCT_EXPORT_VIEW_PROPERTY(pictureInPictureEnabled, BOOL)

RCT_EXPORT_VIEW_PROPERTY(autoStartPictureInPicture, BOOL)

RCT_EXPORT_VIEW_PROPERTY(autoStopPictureInPicture, BOOL)

RCT_EXPORT_VIEW_PROPERTY(onPictureInPictureChange, RCTBubblingEventBlock)

RCT_CUSTOM_VIEW_PROPERTY(pictureInPicturePreferredSize, NSDictionary *, RTCVideoView) {
    if (@available(iOS 15.0, *)) {
        if ([json isKindOfClass:[NSDictionary class]]) {
            id width = json[@"width"];
            id height = json[@"height"];

            if ([width isKindOfClass:[NSNumber class]] && [height isKindOfClass:[NSNumber class]]) {
                view.pictureInPicturePreferredSize = CGSizeMake([width doubleValue], [height doubleValue]);
            }
        }
    }
}

RCT_EXPORT_METHOD(startPictureInPicture : (nonnull NSNumber *)reactTag) {
    if (@available(iOS 15.0, *)) {
        RCTUIManager *uiManager = [self.bridge moduleForClass:[RCTUIManager class]];
        [uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
            UIView *view = viewRegistry[reactTag];
            if (!view || ![view isKindOfClass:[RTCVideoView class]]) {
                RCTLogError(@"Cannot find RTCVideoView with tag #%@", reactTag);
                return;
            }
            [(RTCVideoView *)view startPIPWithParams:YES];
        }];
    }
}

RCT_EXPORT_METHOD(stopPictureInPicture : (nonnull NSNumber *)reactTag) {
    if (@available(iOS 15.0, *)) {
        RCTUIManager *uiManager = [self.bridge moduleForClass:[RCTUIManager class]];
        [uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
            UIView *view = viewRegistry[reactTag];
            if (!view || ![view isKindOfClass:[RTCVideoView class]]) {
                RCTLogError(@"Cannot find RTCVideoView with tag #%@", reactTag);
                return;
            }
            [(RTCVideoView *)view stopPIP];
        }];
    }
}

// Keeping startIOSPIP for backward compatibility
RCT_EXPORT_METHOD(startIOSPIP : (nonnull NSNumber *)reactTag) {
    if (@available(iOS 15.0, *)) {
        RCTUIManager *uiManager = [self.bridge moduleForClass:[RCTUIManager class]];
        [uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
            UIView *view = viewRegistry[reactTag];
            if (!view || ![view isKindOfClass:[RTCVideoView class]]) {
                RCTLogError(@"Cannot find RTCVideoView with tag #%@", reactTag);
                return;
            }
            [(RTCVideoView *)view startPIPWithParams:NO];
        }];
    }
}

RCT_EXPORT_METHOD(stopIOSPIP : (nonnull NSNumber *)reactTag) {
    if (@available(iOS 15.0, *)) {
        RCTUIManager *uiManager = [self.bridge moduleForClass:[RCTUIManager class]];
        [uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
            UIView *view = viewRegistry[reactTag];
            if (!view || ![view isKindOfClass:[RTCVideoView class]]) {
                RCTLogError(@"Cannot find RTCVideoView with tag #%@", reactTag);
                return;
            }
            [(RTCVideoView *)view stopPIP];
        }];
    }
}

+ (BOOL)requiresMainQueueSetup {
    return NO;
}

@end
