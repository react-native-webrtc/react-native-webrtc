//
//  WebRTCAudioSession.h
//  sharekey
//
//  Created by  Denis on 3.12.24.
//  Copyright © 2024 Sharekey. All rights reserved.
//

#import "WebRTCModule.h"
#import "WebRTCAudioInput.h"
#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface WebRTCAudioSession : NSObject

@property (assign) BOOL useCallkit;

+ (id)shared;

- (void)initialize;
- (void)configure:(BOOL) speakerOn;
- (void)closeConnection:(void (^)(void)) block;
- (void)configureFromInput:(WebRTCAudioInput *) input;
- (void)setAudioSessionEnabled:(BOOL) enabled;
- (void)audioSessionDidActivate:(AVAudioSession *) session;
- (void)audioSessionDidDeactivate:(AVAudioSession *) session;
- (void)activateCallKitAudioSession;

+ (RTCAudioSessionConfiguration *)defaultConfiguration;

@end

NS_ASSUME_NONNULL_END
