#import <CallKit/CallKit.h>
#import <Foundation/Foundation.h>

typedef void (^CallKitVoidCallback)(void);
typedef void (^CallKitStringCallback)(NSString *);
typedef void (^CallKitBoolCallback)(BOOL);

@interface CallKitManager : NSObject<CXProviderDelegate>

@property(nonatomic, copy) CallKitVoidCallback onCallStarted;
@property(nonatomic, copy) CallKitStringCallback onCallAnswered;
@property(nonatomic, copy) CallKitStringCallback onCallEnded;
@property(nonatomic, copy) CallKitStringCallback onCallFailed;
@property(nonatomic, copy) CallKitBoolCallback onCallMuted;
@property(nonatomic, copy) CallKitBoolCallback onCallHeld;
@property(nonatomic, readonly) BOOL hasActiveCall;
@property(nonatomic, readonly) BOOL isCallAnswered;
@property(nonatomic, readonly) BOOL isOutgoingCall;
@property(nonatomic, readonly) BOOL isCallOnHold;
@property(nonatomic, readonly, nullable) NSString *pendingAnswerRequestId;

+ (instancetype)shared;

- (void)startCallWithDisplayName:(NSString *)displayName handle:(NSString *)handle isVideo:(BOOL)isVideo;
- (void)reportIncomingCallWithDisplayName:(NSString *)displayName
                                   handle:(NSString *)handle
                                  isVideo:(BOOL)isVideo;
- (void)endCallWithReason:(NSString *_Nullable)reason;
- (BOOL)fulfillIncomingCallConnected:(NSString *)requestId;
- (void)failIncomingCallConnected:(NSString *)requestId;
- (void)reportOutgoingCallConnected;
- (void)setCallHeld:(BOOL)onHold;

@end
