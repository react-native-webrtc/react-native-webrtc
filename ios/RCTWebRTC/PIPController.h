#import <UIKit/UIKit.h>
#import <WebRTC/RTCVideoTrack.h>
#import <AVKit/AVKit.h>

API_AVAILABLE(ios(15.0))
@interface PIPController : NSObject <AVPictureInPictureControllerDelegate>

@property(nonatomic, weak) UIView *sourceView;
@property(nonatomic, strong) RTCVideoTrack *videoTrack;

@property(nonatomic, assign) BOOL startAutomatically;

// TODO implement
@property(nonatomic, assign) BOOL stopAutomatically;

- (instancetype)initWithSourceView:(UIView *)sourceView;
- (void)togglePIP;
- (void)insertFallbackView:(UIView *)subview;
@end
