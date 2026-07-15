#import "VoipManager.h"
#import <Intents/Intents.h>
#import <PushKit/PushKit.h>
#import "CallKitManager.h"

@interface VoipManager ()<PKPushRegistryDelegate>
@property(nonatomic, strong) PKPushRegistry *registry;
@property(nonatomic, strong) dispatch_queue_t registryQueue;
@property(copy, readwrite, nullable) NSString *token;
@property(copy, readwrite, nullable) NSDictionary *pendingIncomingCall;
@property(copy, readwrite, nullable) NSDictionary *pendingCallIntent;
@property(copy, nullable) NSDictionary *pendingSecondIncomingCall;
@end

@implementation VoipManager

+ (instancetype)shared {
    static VoipManager *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[VoipManager alloc] init];
    });

    return sharedInstance;
}

+ (void)registerForVoIPPushes {
    [[self shared] registerForVoIPPushes];
}

+ (BOOL)handleContinueUserActivity:(NSUserActivity *)userActivity {
    return [[self shared] handleContinueUserActivity:userActivity];
}

- (void)registerForVoIPPushes {
    if (self.registry != nil) {
        return;
    }

    self.registryQueue = dispatch_queue_create("io.fishjam.voippush", DISPATCH_QUEUE_SERIAL);
    self.registry = [[PKPushRegistry alloc] initWithQueue:self.registryQueue];
    self.registry.delegate = self;
    self.registry.desiredPushTypes = [NSSet setWithObject:PKPushTypeVoIP];
}

#pragma mark - PKPushRegistryDelegate

- (void)pushRegistry:(PKPushRegistry *)registry
    didUpdatePushCredentials:(PKPushCredentials *)pushCredentials
                     forType:(PKPushType)type {
    const unsigned char *bytes = pushCredentials.token.bytes;
    NSMutableString *hex = [NSMutableString stringWithCapacity:pushCredentials.token.length * 2];
    for (NSUInteger i = 0; i < pushCredentials.token.length; i++) {
        [hex appendFormat:@"%02x", bytes[i]];
    }
    NSString *tokenString = [hex copy];

    if ([tokenString isEqualToString:self.token]) {
        return;
    }
    self.token = tokenString;

    if (self.onTokenUpdated) {
        self.onTokenUpdated(tokenString);
    }
}

- (void)pushRegistry:(PKPushRegistry *)registry didInvalidatePushTokenForType:(PKPushType)type {
    self.token = nil;
}

- (void)pushRegistry:(PKPushRegistry *)registry
    didReceiveIncomingPushWithPayload:(PKPushPayload *)payload
                              forType:(PKPushType)type
                withCompletionHandler:(void (^)(void))completion {
    NSMutableDictionary *dict = [payload.dictionaryPayload mutableCopy];
    NSString *displayName = dict[@"displayName"];
    NSString *handle = dict[@"handle"];
    BOOL isVideo = [dict[@"isVideo"] boolValue];
    dict[@"isVideo"] = @(isVideo);

    if (displayName == nil || displayName.length == 0) {
        displayName = @"Incoming call";
        dict[@"displayName"] = displayName;
    }

    if (handle.length == 0) {
        handle = displayName;
    }
    dict[@"handle"] = handle;

    IncomingCallSlot slot = [[CallKitManager shared] reportIncomingCallWithDisplayName:displayName
                                                                                       handle:handle
                                                                                      isVideo:isVideo];

    switch (slot) {
        case IncomingCallSlotRejected:
            if (self.onWaitingCallDeclined) {
                self.onWaitingCallDeclined(dict ?: @{});
            }
            break;
        case IncomingCallSlotCurrent:
            // Buffer the payload if the app was cold-launched and JS side hasn't yet loaded
            self.pendingIncomingCall = dict;
            if (self.onIncomingPush) {
                self.onIncomingPush(dict ?: @{});
            }
            break;
        case IncomingCallSlotWaiting:
            [self bufferPendingSecondIncomingCall:dict ?: @{}];
            break;
    }

    completion();
}

- (void)clearPendingIncomingCall {
    self.pendingIncomingCall = nil;
}

- (void)bufferPendingSecondIncomingCall:(NSDictionary *)payload {
    self.pendingSecondIncomingCall = payload;
}

- (void)revealPendingSecondIncomingCall {
    NSDictionary *payload = self.pendingSecondIncomingCall;
    if (payload == nil) {
        return;
    }
    self.pendingSecondIncomingCall = nil;
    self.pendingIncomingCall = payload;
    if (self.onIncomingPush) {
        self.onIncomingPush(payload);
    }
}

- (void)discardPendingSecondIncomingCall {
    NSDictionary *payload = self.pendingSecondIncomingCall;
    self.pendingSecondIncomingCall = nil;
    if (payload != nil && self.onWaitingCallDeclined) {
        self.onWaitingCallDeclined(payload);
    }
}

- (void)clearPendingCallIntent {
    self.pendingCallIntent = nil;
}

- (BOOL)handleContinueUserActivity:(NSUserActivity *)userActivity {
    INIntent *intent = userActivity.interaction.intent;
    INPerson *person = nil;
    BOOL isVideo = NO;

    // INStartAudioCallIntent/INStartVideoCallIntent are deprecated in favour of
    // INStartCallIntent, but Recents redial still delivers them, so all three are handled.
    if ([intent isKindOfClass:[INStartCallIntent class]]) {
        INStartCallIntent *startCallIntent = (INStartCallIntent *)intent;
        person = startCallIntent.contacts.firstObject;
        isVideo = startCallIntent.callCapability == INCallCapabilityVideoCall;
    } else if ([intent isKindOfClass:[INStartAudioCallIntent class]]) {
        person = ((INStartAudioCallIntent *)intent).contacts.firstObject;
    } else if ([intent isKindOfClass:[INStartVideoCallIntent class]]) {
        person = ((INStartVideoCallIntent *)intent).contacts.firstObject;
        isVideo = YES;
    } else {
        return NO;
    }

    NSString *handle = person.personHandle.value;
    if (handle.length == 0) {
        return NO;
    }

    NSString *displayName = person.displayName.length > 0 ? person.displayName : handle;

    NSDictionary *callIntent = @{
        @"handle" : handle,
        @"displayName" : displayName,
        @"isVideo" : @(isVideo),
    };
    self.pendingCallIntent = callIntent;
    if (self.onCallIntent) {
        self.onCallIntent(callIntent);
    }
    return YES;
}

@end
