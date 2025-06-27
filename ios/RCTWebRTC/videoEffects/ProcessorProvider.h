#import "VideoFrameProcessor.h"

@interface ProcessorProvider : NSObject

+ (NSObject<VideoFrameProcessorDelegate> *)getProcessor:(NSString *)name;
+ (void)addProcessor:(NSObject<VideoFrameProcessorDelegate> *)processor forName:(NSString *)name;
+ (void)removeProcessor:(NSString *)name;

@end
