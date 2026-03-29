#ifdef RCT_NEW_ARCH_ENABLED

#import <AVFoundation/AVFoundation.h>

#import <React/RCTFabricComponentsPlugins.h>
#import <React/RCTLog.h>

#import <WebRTC/RTCCVPixelBuffer.h>
#import <WebRTC/RTCVideoFrame.h>
#import <WebRTC/RTCVideoTrack.h>
#if TARGET_OS_OSX
#import <WebRTC/RTCMTLNSVideoView.h>
#else
#import <WebRTC/RTCMTLVideoView.h>
#endif

#import "PIPController.h"
#import "RTCVideoViewComponentView.h"
#import "WebRTCModule.h"

#import <react/renderer/components/RNWebRTCSpec/ComponentDescriptors.h>
#import <react/renderer/components/RNWebRTCSpec/Props.h>

using namespace facebook::react;

@interface RTCVideoViewComponentView () <RTCVideoViewDelegate>
@end

@implementation RTCVideoViewComponentView {
    UIView<RTCVideoRenderer> *_videoView;
    RTCVideoTrack *_videoTrack;
    BOOL _mirror;
    RTCVideoViewObjectFit _objectFit;
    BOOL _iosPIPEnabled;
    BOOL _iosPIPStartAutomatically;
    BOOL _iosPIPStopAutomatically;
    CGFloat _iosPIPPreferredWidth;
    CGFloat _iosPIPPreferredHeight;
    PIPController *_pipController;
}

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
        _iosPIPEnabled = YES;
        _iosPIPStartAutomatically = YES;
        _iosPIPStopAutomatically = YES;
        [self.contentView addSubview:_videoView];
        _videoView.delegate = self;
        self.contentView.clipsToBounds = YES;
    }
    return self;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider {
    return concreteComponentDescriptorProvider<RTCVideoViewComponentDescriptor>();
}

- (void)layoutSubviews {
    [super layoutSubviews];
    _videoView.frame = self.contentView.bounds;
}

- (void)updateProps:(const Props::Shared &)props oldProps:(const Props::Shared &)oldProps {
    const auto &oldViewProps = static_cast<const RTCVideoViewProps &>(*oldProps);
    const auto &newViewProps = static_cast<const RTCVideoViewProps &>(*props);

    // Update mirror
    if (oldViewProps.mirror != newViewProps.mirror) {
        [self setMirror:newViewProps.mirror];
    }

    // Update objectFit
    if (oldViewProps.objectFit != newViewProps.objectFit) {
        RTCVideoViewObjectFit fit = newViewProps.objectFit == RTCVideoViewObjectFit::Cover
            ? RTCVideoViewObjectFitCover
            : RTCVideoViewObjectFitContain;
        [self setObjectFit:fit];
    }

    // Update iosPIPEnabled
    if (oldViewProps.iosPIPEnabled != newViewProps.iosPIPEnabled) {
        [self setIosPIPEnabled:newViewProps.iosPIPEnabled];
    }

    // Update iosPIPStartAutomatically
    if (oldViewProps.iosPIPStartAutomatically != newViewProps.iosPIPStartAutomatically) {
        [self setIosPIPStartAutomatically:newViewProps.iosPIPStartAutomatically];
    }

    // Update iosPIPStopAutomatically
    if (oldViewProps.iosPIPStopAutomatically != newViewProps.iosPIPStopAutomatically) {
        [self setIosPIPStopAutomatically:newViewProps.iosPIPStopAutomatically];
    }

    // Update iosPIPPreferredWidth
    if (oldViewProps.iosPIPPreferredWidth != newViewProps.iosPIPPreferredWidth) {
        [self setIosPIPPreferredWidth:newViewProps.iosPIPPreferredWidth];
    }

    // Update iosPIPPreferredHeight
    if (oldViewProps.iosPIPPreferredHeight != newViewProps.iosPIPPreferredHeight) {
        [self setIosPIPPreferredHeight:newViewProps.iosPIPPreferredHeight];
    }

    [super updateProps:props oldProps:oldProps];
}

#pragma mark - Mirror

- (void)setMirror:(BOOL)mirror {
    if (_mirror != mirror) {
        _mirror = mirror;
        _videoView.transform = mirror ? CGAffineTransformMakeScale(-1.0, 1.0) : CGAffineTransformIdentity;
    }
}

#pragma mark - ObjectFit

- (void)setObjectFit:(RTCVideoViewObjectFit)fit {
    if (_objectFit != fit) {
        _objectFit = fit;
#if !TARGET_OS_OSX
        if (fit == RTCVideoViewObjectFitCover) {
            ((RTCMTLVideoView *)_videoView).videoContentMode = UIViewContentModeScaleAspectFill;
        } else {
            ((RTCMTLVideoView *)_videoView).videoContentMode = UIViewContentModeScaleAspectFit;
        }
#endif
        if (@available(iOS 15.0, *)) {
            _pipController.objectFit = fit;
        }
    }
}

#pragma mark - Video Track

- (void)setVideoTrack:(RTCVideoTrack *)videoTrack {
    RTCVideoTrack *oldTrack = self.videoTrack;

    if (oldTrack != videoTrack) {
        if (oldTrack) {
            WebRTCModule *module = [WebRTCModule sharedInstance];
            __weak RTCVideoTrack *weakOldTrack = oldTrack;
            __weak typeof(self) weakSelf = self;
            dispatch_async(module.workerQueue, ^{
                RTCVideoTrack *strongOldTrack = weakOldTrack;
                __strong typeof(weakSelf) strongSelf = weakSelf;
                if (strongOldTrack && strongSelf) {
                    [strongOldTrack removeRenderer:strongSelf->_videoView];
                }
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

            [_videoView renderFrame:frame];

            CVPixelBufferRelease(pixelBuffer);
        }

        // See "didMoveToWindow" below.
        if (videoTrack && self.window) {
            WebRTCModule *module = [WebRTCModule sharedInstance];
            __weak typeof(self) weakSelf = self;
            dispatch_async(module.workerQueue, ^{
                __strong typeof(weakSelf) strongSelf = weakSelf;
                if (strongSelf) {
                    [videoTrack addRenderer:strongSelf->_videoView];
                }
            });
        }
    }
}

#pragma mark - Window Lifecycle

- (void)didMoveToWindow {
    [super didMoveToWindow];
    RTCVideoTrack *videoTrack = self.videoTrack;
    WebRTCModule *module = [WebRTCModule sharedInstance];
    if (videoTrack) {
        __weak typeof(self) weakSelf = self;
        if (self.window) {
            dispatch_async(module.workerQueue, ^{
                __strong typeof(weakSelf) strongSelf = weakSelf;
                if (strongSelf) {
                    [videoTrack addRenderer:strongSelf->_videoView];
                }
            });
        } else {
            __weak RTCVideoTrack *weakVideoTrack = videoTrack;
            dispatch_async(module.workerQueue, ^{
                RTCVideoTrack *strongVideoTrack = weakVideoTrack;
                __strong typeof(weakSelf) strongSelf = weakSelf;
                if (strongVideoTrack && strongSelf) {
                    [strongVideoTrack removeRenderer:strongSelf->_videoView];
                }
            });
        }
    }
}

#pragma mark - PIP Properties

- (void)setIosPIPEnabled:(BOOL)iosPIPEnabled {
    _iosPIPEnabled = iosPIPEnabled;
    if (@available(iOS 15.0, *)) {
        [self updatePIPFromIndividualProps];
    }
}

- (void)setIosPIPStartAutomatically:(BOOL)iosPIPStartAutomatically {
    _iosPIPStartAutomatically = iosPIPStartAutomatically;
    if (@available(iOS 15.0, *)) {
        [self updatePIPFromIndividualProps];
    }
}

- (void)setIosPIPStopAutomatically:(BOOL)iosPIPStopAutomatically {
    _iosPIPStopAutomatically = iosPIPStopAutomatically;
    if (@available(iOS 15.0, *)) {
        [self updatePIPFromIndividualProps];
    }
}

- (void)setIosPIPPreferredWidth:(CGFloat)iosPIPPreferredWidth {
    _iosPIPPreferredWidth = iosPIPPreferredWidth;
    if (@available(iOS 15.0, *)) {
        [self updatePIPFromIndividualProps];
    }
}

- (void)setIosPIPPreferredHeight:(CGFloat)iosPIPPreferredHeight {
    _iosPIPPreferredHeight = iosPIPPreferredHeight;
    if (@available(iOS 15.0, *)) {
        [self updatePIPFromIndividualProps];
    }
}

#pragma mark - PIP Controller

- (void)updatePIPFromIndividualProps API_AVAILABLE(ios(15.0)) {
    if (!_iosPIPEnabled) {
        [_pipController stopPIP];
        _pipController = nil;
        return;
    }

    if (!_pipController) {
        _pipController = [[PIPController alloc] initWithSourceView:self];
        _pipController.videoTrack = _videoTrack;
    }

    _pipController.startAutomatically = _iosPIPStartAutomatically;
    _pipController.stopAutomatically = _iosPIPStopAutomatically;
    _pipController.objectFit = _objectFit;
    _pipController.preferredSize = CGSizeMake(_iosPIPPreferredWidth, _iosPIPPreferredHeight);
}

- (void)startPIP API_AVAILABLE(ios(15.0)) {
    [_pipController startPIP];
}

- (void)stopPIP API_AVAILABLE(ios(15.0)) {
    [_pipController stopPIP];
}

#pragma mark - Commands

- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args {
    if (@available(iOS 15.0, *)) {
        if ([commandName isEqual:@"startIOSPIP"]) {
            [self startPIP];
        } else if ([commandName isEqual:@"stopIOSPIP"]) {
            [self stopPIP];
        }
    }
}

#pragma mark - Subviews (for PIP fallback)

- (void)insertReactSubview:(UIView *)subview atIndex:(NSInteger)atIndex {
    if (@available(iOS 15.0, *)) {
        [_pipController insertFallbackView:subview];
    }
}

#pragma mark - RTCVideoViewDelegate

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

Class<RCTComponentViewProtocol> RTCVideoViewCls(void) {
    return RTCVideoViewComponentView.class;
}

#endif
