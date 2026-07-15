#import <Foundation/Foundation.h>

@interface VoipManager : NSObject
@property(nonatomic, copy, readonly, nullable) NSString *token;
@property(nonatomic, copy, readonly, nullable) NSDictionary *pendingIncomingCall;
@property(nonatomic, copy, readonly, nullable) NSDictionary *pendingCallIntent;
@property(nonatomic, copy) void (^onTokenUpdated)(NSString *token);
@property(nonatomic, copy) void (^onIncomingPush)(NSDictionary *payload);
@property(nonatomic, copy) void (^onCallIntent)(NSDictionary *intent);
@property(nonatomic, copy) void (^onWaitingCallDeclined)(NSDictionary *payload);
+ (instancetype)shared;
+ (void)registerForVoIPPushes;
+ (BOOL)handleContinueUserActivity:(NSUserActivity *)userActivity NS_SWIFT_NAME(handleContinueUserActivity(_:));
- (void)clearPendingIncomingCall;
- (void)clearPendingCallIntent;
- (void)bufferPendingSecondIncomingCall:(NSDictionary *)payload;
- (void)revealPendingSecondIncomingCall;
- (void)discardPendingSecondIncomingCall;
@end
