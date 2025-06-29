#import "ProcessorProvider.h"

@implementation ProcessorProvider

static NSMutableDictionary<NSString *, NSObject<VideoFrameProcessorDelegate> *> *processorMap;

+ (void)initialize {
    processorMap = [[NSMutableDictionary alloc] init];
}

+ (NSObject<VideoFrameProcessorDelegate> *)getProcessor:(NSString *)name {
    return [processorMap objectForKey:name];
}

+ (void)addProcessor:(NSObject<VideoFrameProcessorDelegate> *)processor forName:(NSString *)name {
    [processorMap setObject:processor forKey:name];
}

+ (void)removeProcessor:(NSString *)name {
    [processorMap removeObjectForKey:name];
}

@end
