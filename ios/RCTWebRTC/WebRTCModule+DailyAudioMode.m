#import <objc/runtime.h>
#import "WebRTCModule.h"
#import "WebRTCModule+DailyAudioMode.h"

@implementation WebRTCModule (DailyAudioMode)

- (void)setAudioMode:(NSString *)audioMode {
  objc_setAssociatedObject(self,
                           @selector(audioMode),
                           audioMode,
                           OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSString *)audioMode {
  return  objc_getAssociatedObject(self, @selector(audioMode));
}

@end
