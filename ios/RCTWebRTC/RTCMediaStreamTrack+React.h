
#import <WebRTC/RTCMediaStreamTrack.h>

#import "VideoCaptureControllerProtocol.h"

@interface RTCMediaStreamTrack (React)

@property (strong, nonatomic) id<VideoCaptureControllerProtocol> videoCaptureController;

@end
