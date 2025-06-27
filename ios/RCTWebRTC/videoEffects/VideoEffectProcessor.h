#import <WebRTC/RTCVideoSource.h>

#import "VideoFrameProcessor.h"

@interface VideoEffectProcessor : NSObject<RTCVideoCapturerDelegate>

@property(nonatomic, strong) NSArray<NSObject<VideoFrameProcessorDelegate> *> *videoFrameProcessors;
@property(nonatomic, strong) RTCVideoSource *videoSource;

- (instancetype)initWithProcessors:(NSArray<NSObject<VideoFrameProcessorDelegate> *> *)videoFrameProcessors
                       videoSource:(RTCVideoSource *)videoSource;

@end
