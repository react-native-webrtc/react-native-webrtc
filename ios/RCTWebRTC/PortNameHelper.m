//
//  PortNameHelper.m
//  sharekey
//
//  Created by  Denis on 3.12.24.
//  Copyright © 2024 Sharekey. All rights reserved.
//

#import "PortNameHelper.h"

@implementation PortNameHelper
+ (NSString *)stringFromPortName:(PortName)portName {
  switch (portName) {
    case PortNamePhone: return @"Phone";
    case PortNameSpeaker: return @"Speaker";
    case PortNameHeadset: return @"Headset";
    case PortNameBluetooth: return @"Bluetooth";
    default: return @"Unknown";
  }
}

+ (PortName)portNameFromString:(NSString *)portName {
  if ([portName isEqualToString:@"Phone"]) {
    return PortNamePhone;
  } else if ([portName isEqualToString:@"Speaker"]) {
    return PortNameSpeaker;
  } else if ([portName isEqualToString:@"Headset"]) {
    return PortNameHeadset;
  } else if ([portName isEqualToString:@"Bluetooth"]) {
    return PortNameBluetooth;
  } else {
    return PortNameUnknown;
  }
}
@end
