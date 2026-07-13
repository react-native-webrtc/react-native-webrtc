#import <Foundation/Foundation.h>

typedef NS_ENUM(NSInteger, FulfillResult) {
    FulfillResultFulfilled,
    FulfillResultCancelled,
    FulfillResultTimedOut,
};

NS_ASSUME_NONNULL_BEGIN

@interface FulfillRequestManager : NSObject

+ (instancetype)shared;

- (NSString *)createRequestWithTimeout:(NSTimeInterval)timeout
                            completion:(void (^)(FulfillResult result))completion;
- (BOOL)fulfill:(NSString *)requestId;
- (BOOL)cancel:(NSString *)requestId;
- (void)cancelAll;

@end

NS_ASSUME_NONNULL_END
