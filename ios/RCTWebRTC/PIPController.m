#import <AVKit/AVKit.h>
#import "PIPController.h"
#import "SampleBufferVideoCallView.h"

@interface PIPController ()

@property(nonatomic, strong) AVPictureInPictureVideoCallViewController *pipCallViewController;
@property(nonatomic, strong) AVPictureInPictureControllerContentSource *contentSource;
@property(nonatomic, strong) AVPictureInPictureController *pipController;
@property(nonnull, nonatomic, strong) SampleBufferVideoCallView *sampleView;

@end

@implementation PIPController

- (instancetype)initWithSourceView:(UIView *)sourceView {

    if (self = [super init]) {
        self.sourceView = sourceView;
        
        SampleBufferVideoCallView * subview = [[SampleBufferVideoCallView alloc] initWithFrame:CGRectMake(0, 0, 1000, 1000)];
        _sampleView = subview;
        _pipCallViewController = [[AVPictureInPictureVideoCallViewController alloc] init];
        [_pipCallViewController setPreferredContentSize:CGSizeMake(1000, 1000)];
        [_pipCallViewController.view addSubview:subview];
        
        subview.translatesAutoresizingMaskIntoConstraints = false;
        NSArray *constraints = @[
            [subview.leadingAnchor constraintEqualToAnchor:_pipCallViewController.view.leadingAnchor],
            [subview.trailingAnchor constraintEqualToAnchor: _pipCallViewController.view.trailingAnchor],
            [subview.topAnchor constraintEqualToAnchor: _pipCallViewController.view.topAnchor],
            [subview.bottomAnchor constraintEqualToAnchor: _pipCallViewController.view.bottomAnchor]
        ];
        [NSLayoutConstraint activateConstraints:constraints];
        
        _contentSource = [[AVPictureInPictureControllerContentSource alloc] initWithActiveVideoCallSourceView:sourceView contentViewController:_pipCallViewController];
        
        _pipController = [[AVPictureInPictureController alloc] initWithContentSource:_contentSource];
        _pipController.canStartPictureInPictureAutomaticallyFromInline = YES;
        _pipController.delegate = self;
    }
    
    return self;
}

- (void)setVideoTrack:(RTCVideoTrack *)videoTrack {
    if(self.videoTrack != videoTrack) {
        [self.videoTrack removeRenderer:_sampleView];
    }
    [videoTrack addRenderer:_sampleView];
}

- (BOOL)startAutomatically {
    return _pipController.canStartPictureInPictureAutomaticallyFromInline;
}

- (void)setStartAutomatically:(BOOL)value {
    _pipController.canStartPictureInPictureAutomaticallyFromInline = value;
}

- (void)togglePIP {
    if(_pipController.isPictureInPictureActive) {
        [_pipController stopPictureInPicture];
    } else if(_pipController.isPictureInPicturePossible){
        [_pipController startPictureInPicture];
    }
}

- (void)dealloc {
    [_videoTrack removeRenderer:_sampleView];
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
    
    NSLog(@"%@", NSStringFromSelector(_cmd)); // Objective-C
}

/*!
    @method        pictureInPictureControllerDidStartPictureInPicture:
    @param        pictureInPictureController
                The Picture in Picture controller.
    @abstract    Delegate can implement this method to be notified when Picture in Picture did start.
 */
- (void)pictureInPictureControllerDidStartPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    
    NSLog(@"%@", NSStringFromSelector(_cmd)); // Objective-C
}

/*!
    @method        pictureInPictureController:failedToStartPictureInPictureWithError:
    @param        pictureInPictureController
                The Picture in Picture controller.
    @param        error
                An error describing why it failed.
    @abstract    Delegate can implement this method to be notified when Picture in Picture failed to start.
 */
- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController failedToStartPictureInPictureWithError:(NSError *)error{
    
    NSLog(@"%@: %@", NSStringFromSelector(_cmd), [error localizedDescription]); // Objective-C
}

/*!
    @method        pictureInPictureControllerWillStopPictureInPicture:
    @param        pictureInPictureController
                The Picture in Picture controller.
    @abstract    Delegate can implement this method to be notified when Picture in Picture will stop.
 */
- (void)pictureInPictureControllerWillStopPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    
    NSLog(@"%@", NSStringFromSelector(_cmd)); // Objective-C
}

/*!
    @method        pictureInPictureControllerDidStopPictureInPicture:
    @param        pictureInPictureController
                The Picture in Picture controller.
    @abstract    Delegate can implement this method to be notified when Picture in Picture did stop.
 */
- (void)pictureInPictureControllerDidStopPictureInPicture:(AVPictureInPictureController *)pictureInPictureController {
    
    NSLog(@"%@", NSStringFromSelector(_cmd)); // Objective-C
}

/*!
    @method        pictureInPictureController:restoreUserInterfaceForPictureInPictureStopWithCompletionHandler:
    @param        pictureInPictureController
                The Picture in Picture controller.
    @param        completionHandler
                The completion handler the delegate needs to call after restore.
    @abstract    Delegate can implement this method to restore the user interface before Picture in Picture stops.
 */
- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController restoreUserInterfaceForPictureInPictureStopWithCompletionHandler:(void (^)(BOOL restored))completionHandler {
    
    NSLog(@"%@", NSStringFromSelector(_cmd)); // Objective-C
}


@end
