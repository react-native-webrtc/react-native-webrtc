
#import <Foundation/Foundation.h>
#import <WebRTC/RTCCameraVideoCapturer.h>
#import "VideoCaptureControllerProtocol.h"

NS_ASSUME_NONNULL_BEGIN

@protocol SwitchProtocol <NSObject>

-(void)switchDevice;

@end

@interface RTCCameraVideoCapturerController : NSObject <SwitchProtocol, VideoCaptureControllerProtocol>

-(instancetype)initWithCapturer:(RTCCameraVideoCapturer *)capturer
                 andConstraints:(NSDictionary *)constraints;

@end

NS_ASSUME_NONNULL_END
