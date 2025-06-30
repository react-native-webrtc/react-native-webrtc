#import "PIPController.h"
#import <AVKit/AVKit.h>
#import "SampleBufferVideoCallView.h"

@interface PIPController ()

@property(nonatomic, strong) AVPictureInPictureVideoCallViewController *pipCallViewController;
@property(nonatomic, strong) AVPictureInPictureControllerContentSource *contentSource;
@property(nonatomic, strong) AVPictureInPictureController *pipController;
@property(nonnull, nonatomic, strong) SampleBufferVideoCallView *sampleView;

@property(nonnull, nonatomic, strong) UIView *fallbackView;

@end

@implementation PIPController

- (instancetype)initWithSourceView:(UIView *)sourceView {
    if (self = [super init]) {
        self.sourceView = sourceView;

        _fallbackView = [[UIView alloc] initWithFrame:CGRectZero];
        _fallbackView.translatesAutoresizingMaskIntoConstraints = false;

        SampleBufferVideoCallView *subview = [[SampleBufferVideoCallView alloc] initWithFrame:CGRectZero];
        _sampleView = subview;
        _sampleView.translatesAutoresizingMaskIntoConstraints = false;
        _pipCallViewController = [[AVPictureInPictureVideoCallViewController alloc] init];

        [self addToCallViewController:_fallbackView];

        _contentSource = [[AVPictureInPictureControllerContentSource alloc]
            initWithActiveVideoCallSourceView:sourceView
                        contentViewController:_pipCallViewController];

        _pipController = [[AVPictureInPictureController alloc] initWithContentSource:_contentSource];
        _pipController.canStartPictureInPictureAutomaticallyFromInline = YES;
        _pipController.delegate = self;

        [_pipController addObserver:self
                         forKeyPath:@"pictureInPictureActive"
                            options:NSKeyValueObservingOptionInitial | NSKeyValueObservingOptionNew
                            context:nil];

        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(applicationWillEnterForeground:)
                                                     name:UIApplicationWillEnterForegroundNotification
                                                   object:nil];
    }

    return self;
}

- (void)observeValueForKeyPath:(NSString *)keyPath
                      ofObject:(id)object
                        change:(NSDictionary *)change
                       context:(void *)context {
    if ([keyPath isEqualToString:@"pictureInPictureActive"]) {
        _sampleView.shouldRender = [change[NSKeyValueChangeNewKey] boolValue];
    }
}

- (void)applicationWillEnterForeground:(NSNotification *)notification {
    if (_stopAutomatically) {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 0.5 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
            [self.pipController stopPictureInPicture];
        });
    }
}

- (void)addToCallViewController:(UIView *)view {
    [_pipCallViewController.view addSubview:view];
    NSArray *constraints = @[
        [view.leadingAnchor constraintEqualToAnchor:_pipCallViewController.view.leadingAnchor],
        [view.trailingAnchor constraintEqualToAnchor:_pipCallViewController.view.trailingAnchor],
        [view.topAnchor constraintEqualToAnchor:_pipCallViewController.view.topAnchor],
        [view.bottomAnchor constraintEqualToAnchor:_pipCallViewController.view.bottomAnchor]
    ];
    [NSLayoutConstraint activateConstraints:constraints];
}

- (void)setVideoTrack:(RTCVideoTrack *)videoTrack {
    if (_videoTrack != videoTrack) {
        [_videoTrack removeRenderer:_sampleView];
    }

    _videoTrack = videoTrack;
    [videoTrack addRenderer:_sampleView];

    if (_videoTrack) {
        if (!_sampleView.superview) {
            [self addToCallViewController:_sampleView];
        }
        if (_fallbackView.superview) {
            [_fallbackView removeFromSuperview];
        }
    } else {
        if (!_fallbackView.superview) {
            [self addToCallViewController:_fallbackView];
        }
        if (_sampleView.superview) {
            [_sampleView removeFromSuperview];
        }
    }
}

- (void)insertFallbackView:(UIView *)view {
    [self.fallbackView addSubview:view];
}

- (void)setObjectFit:(RTCVideoViewObjectFit)fit {
    if (fit == RTCVideoViewObjectFitCover) {
        self.sampleView.sampleBufferLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    } else {
        self.sampleView.sampleBufferLayer.videoGravity = AVLayerVideoGravityResizeAspect;
    }
}

- (CGSize)preferredSize {
    return _pipCallViewController.preferredContentSize;
}

- (void)setPreferredSize:(CGSize)size {
    if (!CGSizeEqualToSize(size, _pipCallViewController.preferredContentSize)) {
        _pipCallViewController.preferredContentSize = size;
        [_sampleView requestScaleRecalculation];
    }
}

- (BOOL)startAutomatically {
    return _pipController.canStartPictureInPictureAutomaticallyFromInline;
}

- (void)setStartAutomatically:(BOOL)value {
    _pipController.canStartPictureInPictureAutomaticallyFromInline = value;
}

- (void)togglePIP {
    if (_pipController.isPictureInPictureActive) {
        [_pipController stopPictureInPicture];
    } else if (_pipController.isPictureInPicturePossible) {
        [_pipController startPictureInPicture];
    }
}

- (void)startPIP {
    if (_pipController.isPictureInPicturePossible) {
        [_pipController startPictureInPicture];
    }
}

- (void)stopPIP {
    if (_pipController.isPictureInPictureActive) {
        [_pipController stopPictureInPicture];
    }
}

- (void)dealloc {
    [_videoTrack removeRenderer:_sampleView];
    [_pipController removeObserver:self forKeyPath:@"pictureInPictureActive"];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationDidBecomeActiveNotification object:nil];
}

@end

@implementation PIPController (AVPictureInPictureControllerDelegate)

/*!
    @method        pictureInPictureControllerWillStartPictureInPicture:
    @param        pictureInPictureController
                The Picture in Picture controller.
    @abstract    Delegate can implement this method to be notified when Picture in Picture will start.
 */
- (void)pictureInPictureControllerWillStartPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    NSLog(@"%@", NSStringFromSelector(_cmd));  // Objective-C
}

/*!
    @method        pictureInPictureControllerDidStartPictureInPicture:
    @param        pictureInPictureController
                The Picture in Picture controller.
    @abstract    Delegate can implement this method to be notified when Picture in Picture did start.
 */
- (void)pictureInPictureControllerDidStartPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    NSLog(@"%@", NSStringFromSelector(_cmd));  // Objective-C
}

/*!
    @method        pictureInPictureController:failedToStartPictureInPictureWithError:
    @param        pictureInPictureController
                The Picture in Picture controller.
    @param        error
                An error describing why it failed.
    @abstract    Delegate can implement this method to be notified when Picture in Picture failed to start.
 */
- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController
    failedToStartPictureInPictureWithError:(NSError *)error {
    NSLog(@"%@: %@", NSStringFromSelector(_cmd), [error localizedDescription]);  // Objective-C
}

/*!
    @method        pictureInPictureControllerWillStopPictureInPicture:
    @param        pictureInPictureController
                The Picture in Picture controller.
    @abstract    Delegate can implement this method to be notified when Picture in Picture will stop.
 */
- (void)pictureInPictureControllerWillStopPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    NSLog(@"%@", NSStringFromSelector(_cmd));  // Objective-C
}

/*!
    @method        pictureInPictureControllerDidStopPictureInPicture:
    @param        pictureInPictureController
                The Picture in Picture controller.
    @abstract    Delegate can implement this method to be notified when Picture in Picture did stop.
 */
- (void)pictureInPictureControllerDidStopPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    NSLog(@"%@", NSStringFromSelector(_cmd));  // Objective-C
}

/*!
    @method        pictureInPictureController:restoreUserInterfaceForPictureInPictureStopWithCompletionHandler:
    @param        pictureInPictureController
                The Picture in Picture controller.
    @param        completionHandler
                The completion handler the delegate needs to call after restore.
    @abstract    Delegate can implement this method to restore the user interface before Picture in Picture stops.
 */
- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController
    restoreUserInterfaceForPictureInPictureStopWithCompletionHandler:(void (^)(BOOL restored))completionHandler {
    NSLog(@"%@", NSStringFromSelector(_cmd));  // Objective-C
}

@end
