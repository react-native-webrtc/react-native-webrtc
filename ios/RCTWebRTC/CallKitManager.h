#import <CallKit/CallKit.h>
#import <Foundation/Foundation.h>

typedef void (^CallKitVoidCallback)(void);
typedef void (^CallKitStringCallback)(NSString *);
typedef void (^CallKitBoolCallback)(BOOL);

/**
 * Where an incoming call landed relative to whatever call already exists:
 * - Current: there was no call yet, so it is reported as the one call the app tracks.
 * - Waiting: another call is already answered/connected, so this one rings as the second
 *   CallKit call; JS is not told unless it is answered.
 * - Rejected: no slot left (still ringing or waiting already taken) — transient CallKit
 *   report for PushKit, then ended; JS signals rejection to the caller.
 */
typedef NS_ENUM(NSInteger, IncomingCallSlot) {
    IncomingCallSlotCurrent,
    IncomingCallSlotWaiting,
    IncomingCallSlotRejected,
};

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
- (IncomingCallSlot)reportIncomingCallWithDisplayName:(NSString *)displayName
                                                      handle:(NSString *)handle
                                                     isVideo:(BOOL)isVideo;
- (void)endCallWithReason:(NSString *_Nullable)reason;
- (BOOL)fulfillIncomingCallConnected:(NSString *)requestId;
- (void)failIncomingCallConnected:(NSString *)requestId;
- (void)reportOutgoingCallConnected;
- (void)setCallHeld:(BOOL)onHold;

@end
