//
//  PortNameHelper.h
//  sharekey
//
//  Created by  Denis on 3.12.24.
//  Copyright © 2024 Sharekey. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, PortName) {
  PortNamePhone,
  PortNameSpeaker,
  PortNameHeadset,
  PortNameUnknown,
  PortNameBluetooth
};

@interface PortNameHelper : NSObject

+ (NSString *)stringFromPortName:(PortName)portName;
+ (PortName)portNameFromString:(NSString *)portName;

@end

NS_ASSUME_NONNULL_END
