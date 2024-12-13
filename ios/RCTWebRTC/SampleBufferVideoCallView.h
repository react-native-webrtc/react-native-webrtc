#import <Foundation/Foundation.h>
#import <React/RCTViewManager.h>
#import <AVKit/AVKit.h>
#import <WebRTC/RTCVideoRenderer.h>

@interface SampleBufferVideoCallView : UIView <RTCVideoRenderer>

@property(nonnull, nonatomic, readonly) AVSampleBufferDisplayLayer *sampleBufferLayer;
@property(nonatomic, assign) BOOL shouldRender;

- (void)requestScaleRecalculation;
@end
