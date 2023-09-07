#import <Foundation/Foundation.h>
#import <WebRTC/WebRTC.h>

NS_ASSUME_NONNULL_BEGIN

@interface WebRTCModuleOptions : NSObject

@property(nonatomic, strong, nullable) id<RTCVideoDecoderFactory> videoDecoderFactory;
@property(nonatomic, strong, nullable) id<RTCVideoEncoderFactory> videoEncoderFactory;
@property(nonatomic, strong, nullable) id<RTCAudioDevice> audioDevice;
@property(nonatomic, strong, nullable) NSDictionary *fieldTrials;
@property(nonatomic, assign) RTCLoggingSeverity loggingSeverity;

#pragma mark - This class is a singleton

+ (instancetype _Nonnull)sharedInstance;

@end

NS_ASSUME_NONNULL_END
