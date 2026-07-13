#import "FulfillRequestManager.h"

@interface FulfillRequestManager ()
@property(nonatomic, strong) NSMutableDictionary<NSString *, void (^)(FulfillResult)> *requests;
@property(nonatomic) dispatch_queue_t queue;
@end

@implementation FulfillRequestManager

+ (instancetype)shared {
    static FulfillRequestManager *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[FulfillRequestManager alloc] init];
    });
    return sharedInstance;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _requests = [NSMutableDictionary dictionary];
        _queue = dispatch_queue_create("io.fishjam.voip.fulfill-requests", DISPATCH_QUEUE_SERIAL);
    }
    return self;
}

- (NSString *)createRequestWithTimeout:(NSTimeInterval)timeout
                            completion:(void (^)(FulfillResult result))completion {
    NSString *requestId = [NSUUID UUID].UUIDString;
    dispatch_sync(self.queue, ^{
        self.requests[requestId] = [completion copy];
    });

    int64_t delay = (int64_t)(MAX(0, timeout) * NSEC_PER_SEC);
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, delay), self.queue, ^{
        void (^pendingCompletion)(FulfillResult) = self.requests[requestId];
        if (pendingCompletion == nil) {
            return;
        }
        [self.requests removeObjectForKey:requestId];
        dispatch_async(dispatch_get_main_queue(), ^{
            pendingCompletion(FulfillResultTimedOut);
        });
    });

    return requestId;
}

- (BOOL)resolveRequest:(NSString *)requestId result:(FulfillResult)result {
    __block void (^completion)(FulfillResult) = nil;
    dispatch_sync(self.queue, ^{
        completion = self.requests[requestId];
        if (completion != nil) {
            [self.requests removeObjectForKey:requestId];
        }
    });

    if (completion == nil) {
        return NO;
    }

    dispatch_async(dispatch_get_main_queue(), ^{
        completion(result);
    });
    return YES;
}

- (BOOL)fulfill:(NSString *)requestId {
    return [self resolveRequest:requestId result:FulfillResultFulfilled];
}

- (BOOL)cancel:(NSString *)requestId {
    return [self resolveRequest:requestId result:FulfillResultCancelled];
}

- (void)cancelAll {
    __block NSArray<void (^)(FulfillResult)> *completions = nil;
    dispatch_sync(self.queue, ^{
        completions = self.requests.allValues;
        [self.requests removeAllObjects];
    });

    for (void (^completion)(FulfillResult) in completions) {
        dispatch_async(dispatch_get_main_queue(), ^{
            completion(FulfillResultCancelled);
        });
    }
}

@end
