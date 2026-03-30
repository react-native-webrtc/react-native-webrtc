#import "ProcessorProvider.h"

@implementation ProcessorProvider

static NSMutableDictionary<NSString *, NSObject<VideoFrameProcessorDelegate> *> *processorMap;

+ (void)initialize {
    processorMap = [[NSMutableDictionary alloc] init];
}

+ (NSObject<VideoFrameProcessorDelegate> *)getProcessor:(NSString *)name {
    @synchronized(self) {
        return [processorMap objectForKey:name];
    }
}

+ (void)addProcessor:(NSObject<VideoFrameProcessorDelegate> *)processor forName:(NSString *)name {
    @synchronized(self) {
        [processorMap setObject:processor forKey:name];
    }
}

+ (void)removeProcessor:(NSString *)name {
    @synchronized(self) {
        [processorMap removeObjectForKey:name];
    }
}

@end
