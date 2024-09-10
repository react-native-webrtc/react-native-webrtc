#import <UIKit/UIKit.h>
#import <WebRTC/RTCVideoTrack.h>
#import <AVKit/AVKit.h>

API_AVAILABLE(ios(15.0))
@interface PIPController : NSObject <AVPictureInPictureControllerDelegate>

@property(nonatomic, weak) UIView *sourceView;
@property(nonatomic, strong) RTCVideoTrack *videoTrack;

@property(nonatomic, assign) BOOL startAutomatically;
@property(nonatomic, assign) BOOL stopAutomatically;

- (instancetype)initWithSourceView:(UIView *)sourceView;
- (void)togglePIP;
- (void)startPIP;
- (void)stopPIP;
- (void)insertFallbackView:(UIView *)subview;
@end
