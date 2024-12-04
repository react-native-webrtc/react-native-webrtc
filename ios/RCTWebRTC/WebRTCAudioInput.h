//
//  WebRTCAudioInput.h
//  sharekey
//
//  Created by  Denis on 3.12.24.
//  Copyright © 2024 Sharekey. All rights reserved.
//

#import "WebRTCModule.h"
#import "PortNameHelper.h"
#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface WebRTCAudioInput : NSObject

@property (nonatomic, strong) NSString *name;
@property (nonatomic, assign) PortName portName;
@property (nonatomic, strong) AVAudioSessionPortDescription *portDescription;

- (AVAudioSessionMode)mode;
- (AVAudioSessionCategory)category;
- (AVAudioSessionCategoryOptions)categoryOptions;

- (instancetype)initWithPortName:(NSString *)portName;
- (instancetype)initWithPortDescription:(AVAudioSessionPortDescription *)portDescription;

@end

NS_ASSUME_NONNULL_END
