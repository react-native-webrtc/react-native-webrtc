
#import <Foundation/Foundation.h>
#import <WebRTC/RTCCameraVideoCapturer.h>

@interface VideoCaptureController : NSObject

@property(nonatomic, readonly) int height;
@property(nonatomic, readonly) int width;
@property(nonatomic, readonly) int fps;
@property(nonatomic, copy, readonly) NSString *facingMode;

-(instancetype)initWithCapturer:(RTCCameraVideoCapturer *)capturer
                 andConstraints:(NSDictionary *)constraints;
-(void)startCapture;
-(void)stopCapture;
-(void)switchCamera;

@end
