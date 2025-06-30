#import <AVKit/AVKit.h>
#import <UIKit/UIKit.h>
#import <WebRTC/RTCVideoTrack.h>

#import "RTCVideoViewManager.h"

API_AVAILABLE(ios(15.0))
@interface PIPController : NSObject<AVPictureInPictureControllerDelegate>

@property(nonatomic, weak) UIView *sourceView;
@property(nonatomic, strong) RTCVideoTrack *videoTrack;

@property(nonatomic, assign) BOOL startAutomatically;
@property(nonatomic, assign) BOOL stopAutomatically;
@property(nonatomic, assign) CGSize preferredSize;

- (instancetype)initWithSourceView:(UIView *)sourceView;
- (void)togglePIP;
- (void)startPIP;
- (void)stopPIP;
- (void)insertFallbackView:(UIView *)subview;
- (void)setObjectFit:(RTCVideoViewObjectFit)fit;

@end
