#import "WebRTCModuleOptions.h"

@implementation WebRTCModuleOptions

#pragma mark - This class is a singleton

+ (instancetype)sharedInstance {
    static WebRTCModuleOptions *sharedInstance = nil;
    static dispatch_once_t onceToken;

    dispatch_once(&onceToken, ^{
        sharedInstance = [[self alloc] init];
    });

    return sharedInstance;
}

- (instancetype)init {
    if (self = [super init]) {
        self.audioDevice = nil;
        self.fieldTrials = nil;
        self.videoEncoderFactory = nil;
        self.videoDecoderFactory = nil;
        self.loggingSeverity = RTCLoggingSeverityNone;
    }

    return self;
}

@end
