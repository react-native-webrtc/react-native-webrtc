#import <Foundation/Foundation.h>

#if TARGET_OS_IOS

NS_ASSUME_NONNULL_BEGIN

@interface AudioSessionGuard : NSObject

+ (void)activate;

@end

NS_ASSUME_NONNULL_END

#endif
