
#import <Foundation/Foundation.h>
#import <WebRTC/RTCCameraVideoCapturer.h>

@interface VideoCaptureController : NSObject

-(instancetype)initWithCapturer:(RTCCameraVideoCapturer *)capturer
                 andConstraints:(NSDictionary *)constraints;
-(void)startCapture;
-(void)stopCapture;
-(NSString *)switchCamera;
-(NSString *)facingMode;

@end
